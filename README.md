# vmq-apk

`vmq-apk` 是一个用于**自用收款通知监听**的 Android App。

它通过 `NotificationListenerService` 监听微信 / 支付宝收款通知，并将结果回调到你的自建服务端。

当前网络回调默认请求以下服务端接口：

- `POST /api/v1/system/heartbeat`
- `POST /api/v1/payments/notify`

## 当前状态

- 已迁移到 **AndroidX**
- 已升级到现代 **Gradle / AGP** 构建链
- 已支持 Android 13+ 通知权限
- 主界面已整理为 **ViewModel + UseCase + Repository** 的轻量分层结构
- 通知监听逻辑已拆分为 **Service + Handler + Network Service**
- 已补充 JVM 单元测试与 Compose `androidTest` 基础设施
- 运行时代码主要位于 `app/src/main/java/vmq/`
- Android `namespace` / `applicationId` 当前保持为 `com.vone.qrcode`，以维持既有 App 身份与升级兼容性
- Release 构建已启用代码压缩与资源裁剪
- 已支持以下服务端地址格式：
  - `example.com:8080/key`
  - `http://example.com:8080/key`
  - `https://example.com/key`
  - `https://example.com/api/key`

## 环境要求

- macOS / Linux
- Java 17+
- Android SDK

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
make tag VERSION=1.2.3
make release
make release VERSION=1.2.3
```

### 推荐发版

```bash
bash scripts/release.sh 1.2.3
bash scripts/release.sh
```

说明：
- `bash scripts/release.sh 1.2.3`：按指定版本发版
- `bash scripts/release.sh`：自动递增版本号后发版（默认 patch）
- `make release`：只是对 `bash scripts/release.sh` 的包装

## 测试

### JVM 单元测试

```bash
./gradlew testDebugUnitTest
```

### 编译 Android UI 测试 APK

```bash
./gradlew assembleDebugAndroidTest
```

### 在已连接设备 / 模拟器上运行 UI 测试

```bash
./gradlew connectedDebugAndroidTest
```

### 推荐本地验证顺序

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew assembleDebug
```

## CI

仓库包含基础 GitHub Actions 工作流：

- 运行 `testDebugUnitTest`
- 编译 `assembleDebugAndroidTest`

工作流文件：

```text
.github/workflows/android.yml
```

## 常用命令

```bash
make clean
make apk-path
```

## 代码结构

当前源码目录：

```text
app/src/main/java/vmq/
```

Android 包标识说明：

```text
namespace      = com.vone.qrcode
applicationId  = com.vone.qrcode
source package = vmq.*
```

主要分组：

```text
vmq/
├── data/
├── di/
├── model/
├── network/
├── notification/
├── parser/
├── ui/
│   ├── common/
│   ├── main/
│   ├── notification/
│   └── scan/
├── usecase/
└── util/
```

说明：
- `ui/main`：主界面、ViewModel、消息映射、Compose 页面
- `ui/notification`：通知监听 Android Service
- `ui/scan`：扫码界面
- `ui/common`：共享 UI 文本抽象
- `usecase`：配置提交、配置加载、心跳触发等业务用例
- `notification`：通知分类与动作判定
- `parser`：配置 / 通知解析规则
- `network`：心跳与支付回调网络请求
- `di`：轻量依赖组装
- `data`：配置存储与仓库抽象

## 测试结构

```text
app/src/test/java/         # JVM unit tests
app/src/androidTest/java/  # Instrumentation / Compose UI tests
```

当前重点覆盖：
- `MainViewModel`
- `usecase/*`
- `parser/*`
- `network/*`
- `notification/*`
- `ui/main/MainScreen` Compose UI 测试

## 配置格式

App 当前支持以下配置内容：

```text
example.com:8080/your-key
http://example.com:8080/your-key
https://example.com/your-key
https://example.com/api/your-key
```

说明：
- 最后一个 `/` 后面的内容会被当作通讯密钥
- 如果地址不带 `http://` 或 `https://`，应用会默认补 `http://`
- 纯 `https://example.com` 这类**不带 key 的地址**会被判定为非法配置
- App 会把剩余主机部分作为服务端基础地址，并在其后拼接：
  - `/api/v1/system/heartbeat`
  - `/api/v1/payments/notify`
- 这两个接口都会使用 `POST` + `application/json` 请求体。
- 如果你配置的是 `https://example.com/api/your-key`，最终请求会是：
  - `https://example.com/api/api/v1/system/heartbeat`
  - `https://example.com/api/api/v1/payments/notify`

## 核心限制

这是一个**基于通知文案解析**的自用方案，不是官方支付接口，因此会受到以下因素影响：

- 微信 / 支付宝通知文案变化
- 手机厂商后台限制
- 通知权限是否稳定
- Android 系统版本差异

更适合：
- 小规模、自维护场景

## License

MIT
