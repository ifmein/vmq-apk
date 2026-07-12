
## 构建

### 使用 Gradle

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

### 使用 Makefile

```bash
make build
make build-release
make version
make bump VERSION=1.2.3
make release VERSION=1.2.3
make tag VERSION=1.2.3
```

### 发布新版本

只更新版本号：

```bash
make bump VERSION=1.2.3
```

更新版本号并执行单元测试与 Release 构建：

```bash
make release VERSION=1.2.3
```

仅预览动作，不修改文件：

```bash
make release VERSION=1.2.3 DRY_RUN=1
```

跳过单元测试：

```bash
make release VERSION=1.2.3 SKIP_TESTS=1
```

给当前提交打 Git 标记：

```bash
make tag VERSION=1.2.3
```

仅预览打标动作：

```bash
make tag VERSION=1.2.3 DRY_RUN=1
```


## 测试规范

### 测试目标

- 默认优先编写 **单元测试**，保证核心业务逻辑可回归、可快速执行。
- 新功能、Bug 修复、重构后的行为变化，必须同步补充或更新测试。
- 当前项目以 `app/src/test/java` 下的 **本地单元测试** 为主；仅当必须依赖 Android 真机 / 模拟器能力时，才新增 `app/src/androidTest/java` 下的仪器测试。

### 测试目录约定

- 单元测试：`app/src/test/java/...`
- 仪器测试：`app/src/androidTest/java/...`
- 测试文件名统一使用：`<被测类名>Test.kt`
- 测试辅助类命名统一使用：`*Rule.kt`、`*Fake.kt`、`*Stub.kt`

### 测试命名规范

- 测试方法名必须直接描述行为，推荐使用 Kotlin 反引号命名：

```kotlin
@Test
fun `startHeartbeat emits warning when config is missing`() = runTest {
}
```

- 命名应明确体现：**前置条件 + 行为 + 预期结果**。
- 禁止使用 `test1`、`works`、`successCase` 这类含义不清的名称。

### 测试结构规范

- 每个测试只验证一个明确行为。
- 推荐使用 **Arrange / Act / Assert** 结构，三个阶段之间保留空行。
- 断言必须验证可观察结果，不要只测试“代码执行不报错”。
- 新增测试时，优先覆盖：
  - 成功路径
  - 失败路径
  - 边界输入
  - 状态变化
  - 副作用（如保存配置、发起心跳、发送 effect）

### ViewModel 测试规范

- `ViewModel` 测试必须覆盖：
  - `uiState` 是否按预期更新
  - `effects` 是否按预期发出
  - 与 Repository 的交互是否发生
  - 缺失配置、非法输入、请求失败等异常路径
- 涉及协程的 `ViewModel` 测试，统一使用 `runTest`。
- 涉及 `Dispatchers.Main` 的测试，统一复用 `MainDispatcherRule`。
- 对异步状态流转，使用 `advanceUntilIdle()` 等待任务完成后再断言。

### Repository / 数据层测试规范

- 纯解析、映射、拼装逻辑必须优先写成可单测的纯函数或小型类。
- Repository 测试应关注：
  - 输入到输出的转换结果
  - 成功 / 失败结果封装
  - 关键参数是否正确传递
- 如果某段逻辑难以测试，优先重构为可注入依赖，而不是放弃测试。

### Fake / Stub / Mock 原则

- 当前项目测试优先使用 **Fake / Stub**，与现有 `FakeConfigRepository` 风格保持一致。
- 在没有明确必要前，不引入额外 mocking 框架。
- Fake 类应只实现测试真正需要的最小行为，并暴露必要的调用记录字段用于断言。

### 协程与 Flow 测试规范

- 协程测试统一使用：`kotlinx-coroutines-test`
- 推荐模式：

```kotlin
@Test
fun `example`() = runTest {
    // Arrange

    // Act
    advanceUntilIdle()

    // Assert
}
```

- 对 `Flow` / `StateFlow`：
  - 优先断言最终状态
  - 需要验证单次事件时，可使用 `first()` 获取首个结果
  - 必须避免依赖真实时间等待，如 `Thread.sleep`

### 必测场景清单

提交前至少检查是否覆盖以下场景：

- 配置存在 / 配置缺失
- 输入合法 / 输入非法
- 服务端成功 / 服务端失败
- 状态更新是否正确
- Toast / Effect 是否正确发出
- Repository 保存 / 调用是否发生

### 变更准入规则

- **新功能**：必须附带对应测试。
- **Bug 修复**：必须先补一个可复现该 Bug 的测试，再修复代码。
- **重构**：不得降低已有测试覆盖范围；如行为变化，必须同步更新测试。
- 未附测试的逻辑改动，默认视为不完整，除非该改动确实无法以自动化方式验证，并在提交说明中明确理由。

### 执行命令

日常执行单元测试：

```bash
./gradlew testDebugUnitTest
```

编译 Android UI 测试 APK：

```bash
./gradlew assembleDebugAndroidTest
```

在已连接设备 / 模拟器上执行仪器测试：

```bash
./gradlew connectedDebugAndroidTest
```

推荐本地验证顺序：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew assembleDebug
```

执行全部测试与构建时，可使用：

```bash
make release VERSION=1.2.3
```

仅在需要 Android 设备能力时，才执行仪器测试。

### CI 约定

- 仓库 CI 至少应执行：
  - `./gradlew testDebugUnitTest`
  - `./gradlew assembleDebugAndroidTest`
- PR 合并前，默认应保证以上命令可通过。

### 编写测试时的额外要求

- 测试代码本身也应保持可读性，避免过度复用隐藏意图。
- 一个测试文件只聚焦一个被测类。
- 测试中的注释如有需要，必须使用 **English only**。
- 非必要不要测试 UI 框架实现细节，优先测试业务行为与状态。
- 对外部依赖（网络、系统服务、通知、扫码结果等），必须通过抽象和替身隔离，不要在单元测试中访问真实环境。

## 无线调试

### 配对

```bash
make pair HOST=192.168.20.182 PAIR_PORT=44853 PAIR_CODE=347425
```

### 连接

```bash
make connect HOST=192.168.20.182 CONNECT_PORT=41555
```

## App 使用流程

1. 安装并启动 App
2. 打开“通知访问权限”
3. 通过**扫码配置**或**手动配置**填写服务端地址和密钥
4. 点击“检测心跳”验证服务端连通性（请求 `POST /api/v1/system/heartbeat`）
5. 点击“检测监听”验证通知监听权限
6. 用微信 / 支付宝做一笔小额收款测试（支付通知回调到 `POST /api/v1/payments/notify`）
