# 支付回调重试机制设计

## 背景

`POST /api/v1/payments/notify` 是 App 将支付通知推送到服务端的核心接口。当前实现为单次请求，失败后仅打日志，不做重试。对于支付回调这类关键业务请求，服务端短暂不可用（重启、网络抖动返回 5xx）会导致支付通知**永久丢失**。

## 现状分析

### 代码证据

**PaymentPushService.kt** — 单次请求，无重试：

```kotlin
suspend fun sendPayment(config: AppConfig, paymentEvent: PaymentEvent): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        // ... build request ...
        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Unexpected HTTP ${response.code}" }
            response.body?.string().orEmpty()
        }
    }
}
```

**NeNotificationService.kt** — 调用方也不重试：

```kotlin
serviceScope.launch {
    paymentPushService.sendPayment(config, action.event)
        .onSuccess { response -> Log.d(TAG, "Payment callback response: $response") }
        .onFailure { error -> Log.e(TAG, "Payment callback request failed", error) }
}
```

**OkHttpClient** — `AppContainer` 中直接用 `OkHttpClient()` 默认构造，全项目无 Interceptor、无 `retryOnConnectionFailure` 显式配置，**无任何超时设置**。

### 现有的唯一"重试"

OkHttp 默认 `retryOnConnectionFailure = true`，仅覆盖 TCP 连接层（连接重置、DNS 临时失败），不覆盖 HTTP 5xx / 4xx / 超时等应用层失败。

### OkHttp 默认超时问题

当前 `OkHttpClient()` 未配置任何超时，使用 OkHttp 默认值：

| 参数 | 默认值 |
|------|--------|
| `connectTimeout` | 10s |
| `readTimeout` | 10s |
| `writeTimeout` | 10s |
| `callTimeout` | 0（**无限制**） |

单次请求最差耗时 = connect + write + read ≈ **30 秒**。`callTimeout = 0` 意味着没有整体截止时间，一次请求可能无限挂起。

## 设计目标

- 支付回调失败时自动重试，避免通知永久丢失
- 逻辑通用、可复用（未来 HeartbeatService 也可接入）
- 可测试（重试延迟在单测中不能真实等待）
- 调用方（`NeNotificationService`）无感知，不需要改动
- **每次请求有明确超时，整体重试有明确 deadline，总耗时可预测**

## 已知问题：Dispatchers.IO 与虚拟时间冲突

### 问题

`runTest` 提供虚拟时间调度器，`delay()` 在测试调度器上下文中会自动跳过真实等待。但 `PaymentPushService.sendPayment()` 使用 `withContext(Dispatchers.IO)` 切换到了真实的 IO 调度器，导致 `retry()` 内部的 `delay()` 在 IO 调度器上执行，使用真实挂钟时间而非虚拟时间。

后果：500 重试场景测试会真实等待 1 + 2 + 4 = **7 秒**，单测变慢且不稳定。

### 修复方案

将 `Dispatcher` 作为构造参数注入 `PaymentPushService`：

- **生产环境**：注入 `Dispatchers.IO`（行为不变）
- **测试环境**：注入 `Dispatchers.Unconfined`，`delay()` 在当前线程立即执行，不挂起，不走虚拟时间

选择 `Dispatchers.Unconfined` 而非 `UnconfinedTestDispatcher` 的原因：前者是 `kotlinx-coroutines-core` 自带，无需引入 `kotlinx-coroutines-test` 到主代码；后者仅测试可用，会造成主代码对测试库的反向依赖。

## 方案

两层设计：**通用 retry 工具函数** + **PaymentPushService 接入**。

### 1. 新增 `vmq/util/Retry.kt` — 通用重试工具

```kotlin
package vmq.util

import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Retries a suspend block with exponential backoff.
 *
 * - Retries on [RetryableException] (thrown by the block to signal a retry-worthy failure)
 * - All other exceptions propagate immediately (non-retryable)
 * - Returns the block's result on first success
 */
suspend fun <T> retry(config: RetryConfig = RetryConfig(), block: suspend (attempt: Int) -> T): T {
    var lastException: Exception? = null
    for (attempt in 0 until config.maxAttempts) {
        try {
            return block(attempt)
        } catch (e: RetryableException) {
            lastException = e
            if (attempt < config.maxAttempts - 1) {
                val delayMs = config.delayFor(attempt)
                delay(delayMs)
            }
        }
    }
    throw lastException!!
}

data class RetryConfig(
    val maxAttempts: Int = 4,
    val initialDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 10_000L,
) {
    fun delayFor(attempt: Int): Long =
        min(initialDelayMs * (1L shl attempt), maxDelayMs)
}

/** Thrown by the caller to signal that the operation is retryable. */
class RetryableException(message: String, cause: Throwable? = null) : Exception(message, cause)
```

#### 重试时间线（默认配置）

| 尝试次数 | 时机  | 等待  |
|---------|------|------|
| 第 1 次  | 0s   | —    |
| 第 2 次  | +1s  | 1s   |
| 第 3 次  | +3s  | 2s   |
| 第 4 次  | +7s  | 4s   |

纯 backoff 总延迟 = 7 秒。

#### 总耗时预算

仅计算 backoff 不够，必须计入每次请求耗时：

```
总耗时 = Σ(每次请求耗时) + Σ(backoff 延迟)
```

不设置超时时，最差情况：4 × 30s（请求）+ 7s（backoff）≈ **127 秒**，不可接受。

**修复：两层超时保护**

| 层级 | 机制 | 值 | 作用 |
|------|------|---|------|
| 单次请求 | OkHttpClient `callTimeout` | 5s | 覆盖 connect + write + read 整个请求生命周期 |
| 整体重试 | `withTimeout()` | 30s | 所有尝试 + backoff 的绝对截止时间 |

最耗时路径预算：4 × 5s + 7s = **27 秒** < 30s deadline ✅

极端场景（服务端卡住不响应）：`callTimeout` 5s 强制中断，不会无限挂起。

### 2. 修改 `PaymentPushService.kt` — 接入重试 + 注入 Dispatcher + 超时保护

核心改动：
- **HTTP 5xx → `RetryableException`（重试），4xx → 直接返回 failure（不重试）**
- **新增 `dispatcher` 构造参数**，解决虚拟时间冲突
- **派生带 callTimeout 的 OkHttpClient**，单次请求 5s 超时
- **`withTimeout(30s)` 包裹整体重试**，绝对截止保护

```kotlin
class PaymentPushService(
    okHttpClient: OkHttpClient = OkHttpClient(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    // Per-request 5s timeout, derived from the shared client
    private val client: OkHttpClient = okHttpClient.newBuilder()
        .callTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun sendPayment(config: AppConfig, paymentEvent: PaymentEvent): Result<String> =
        withContext(dispatcher) {
            runCatching {
                withTimeout(30_000L) {
                    retry(RetryConfig(maxAttempts = 4, initialDelayMs = 1_000L)) { attempt ->
                        val timestamp = (currentTimeMillis() / 1_000).toString()
                        val channel = paymentEvent.type.code
                        val sign = HashUtils.signGen("$channel${paymentEvent.amount}$timestamp", config.key)
                        val requestBody = PaymentNotifyRequestBody(
                            channel = channel,
                            price = paymentEvent.amount,
                            t = timestamp,
                            sign = sign,
                        ).toJsonRequestBody()

                        val request = Request.Builder()
                            .url(ApiUrlBuilder.buildAppPushUrl(config.host))
                            .post(requestBody)
                            .build()

                        try {
                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    response.body?.string().orEmpty()
                                } else if (response.code in 500..599) {
                                    throw RetryableException("Server error HTTP ${response.code}")
                                } else {
                                    error("Unexpected HTTP ${response.code}")
                                }
                            }
                        } catch (e: java.io.IOException) {
                            throw RetryableException("Network error", e)
                        }
                    }
                }
            }
        }
}
```

**AppContainer.kt** 无需改动（默认参数 `Dispatchers.IO` 生效）。

### 3. 新增 `vmq/util/RetryTest.kt` — 重试工具单测

```kotlin
class RetryTest {
    @Test
    fun `succeeds on first attempt`() = runTest { ... }

    @Test
    fun `retries on RetryableException and eventually succeeds`() = runTest { ... }

    @Test
    fun `exhausts all attempts and throws last exception`() = runTest { ... }

    @Test
    fun `non-RetryableException propagates immediately without retry`() = runTest { ... }

    @Test
    fun `delayFor computes exponential backoff`() { ... }
}
```

### 4. 更新 `PaymentPushServiceTest.kt` — 补充重试行

测试中注入 `Dispatchers.Unconfined`，确保 `delay()` 不走真实等待：

```kotlin
// Existing test setup, add dispatcher injection:
val service = PaymentPushService(
    okHttpClient = OkHttpClient(),
    dispatcher = Dispatchers.Unconfined,
    currentTimeMillis = { timestampMillis },
)

@Test
fun `sendPayment retries on 500 and succeeds on second attempt`() = runTest {
    val server = MockWebServer()
    server.enqueue(MockResponse().setResponseCode(500))
    server.enqueue(MockResponse().setBody("PUSH_OK"))
    // ... assert result.isSuccess, server.requestCount == 2
    // No real 1s wait thanks to Dispatchers.Unconfined
}

@Test
fun `sendPayment does not retry on 400`() = runTest {
    val server = MockWebServer()
    server.enqueue(MockResponse().setResponseCode(400))
    // ... assert result.isFailure, server.requestCount == 1
}

@Test
fun `sendPayment exhausts retries on persistent 500`() = runTest {
    val server = MockWebServer()
    repeat(4) { server.enqueue(MockResponse().setResponseCode(500)) }
    // ... assert result.isFailure, server.requestCount == 4
    // No real 7s wait (1+2+4) thanks to Dispatchers.Unconfined
}
```

## 变更清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `app/src/main/java/vmq/util/Retry.kt` | **新增** | `RetryConfig` + `RetryableException` + `retry()` |
| `app/src/main/java/vmq/network/PaymentPushService.kt` | **修改** | 新增 `dispatcher` 构造参数；派生 `callTimeout` 5s 的 client；`withTimeout(30s)` + `retry {}` 包裹 |
| `app/src/test/java/vmq/util/RetryTest.kt` | **新增** | retry 工具函数的单测 |
| `app/src/test/java/vmq/network/PaymentPushServiceTest.kt` | **修改** | 注入 `Dispatchers.Unconfined`；补充重试场景测试 |

**不需要改动的文件**：`NeNotificationService.kt`（调用方无感知）、`AppContainer.kt`（默认参数 `Dispatchers.IO` 生效，无需改动）。

## 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| 重试粒度 | 应用层 `retry()` 函数 | 比 OkHttp Interceptor 更灵活，可区分 5xx/4xx |
| 4xx 是否重试 | ❌ 不重试 | 客户端错误（签名错误等）重试无意义 |
| 5xx 是否重试 | ✅ 重试 | 服务端临时不可用是常见场景 |
| IOException 是否重试 | ✅ 重试 | 网络抖动、超时需要重试 |
| 每次重试是否重新计算签名 | ✅ 重新计算 | timestamp 更新，签名跟随变化，更符合服务端校验预期 |
| Dispatcher 注入 | ✅ 构造参数注入 | 解决 `withContext(Dispatchers.IO)` 导致 `delay()` 使用真实时间的冲突 |
| 测试用 Dispatcher | `Dispatchers.Unconfined` | 来自主代码，无需测试库依赖；`delay()` 立即执行不挂起 |
| `runTest` 兼容性 | ✅ 通过注入解决 | 生产用 `Dispatchers.IO`，测试用 `Dispatchers.Unconfined` |
| 单次请求超时 | `callTimeout(5s)` | 覆盖 connect + write + read 整个生命周期；使用 `newBuilder()` 派生，不影响共享 client |
| 整体 deadline | `withTimeout(30s)` | 绝对截止保护，防止重试无限进行；最耗时路径 4×5s+7s=27s < 30s |
