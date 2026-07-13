# vmq-apk

`vmq-apk` 是一个用于**自用收款通知监听**的 Android App。

通过 `NotificationListenerService` 监听微信 / 支付宝收款通知，并将结果回调到你的自建服务端。

默认请求的服务端接口：

- `POST /api/v1/system/heartbeat`
- `POST /api/v1/payments/notify`

## 当前状态

- 基于 **AndroidX** + 现代 **Gradle / AGP** 构建链
- 主界面采用 **ViewModel + UseCase + Repository** 轻量分层
- 支持 Android 13+ 通知权限
- Release 构建启用代码压缩与资源裁剪
- 已补充 JVM 单元测试与 Compose `androidTest` 基础设施

## 环境要求

- macOS / Linux
- Java 17+
- Android SDK

## 构建

```bash
./gradlew assembleDebug      # 调试包
./gradlew assembleRelease    # 发布包
```

推荐使用 `scripts/release.sh` 发版：

```bash
bash scripts/release.sh 1.2.3   # 按指定版本发版
bash scripts/release.sh         # 自动递增版本号后发版（默认 patch）
```

## 测试

```bash
./gradlew testDebugUnitTest          # JVM 单元测试
./gradlew assembleDebugAndroidTest   # 编译 UI 测试 APK
./gradlew connectedDebugAndroidTest  # 在已连接设备上运行 UI 测试
```

本地推荐验证顺序：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebugAndroidTest
./gradlew assembleDebug
```

## CI

`.github/workflows/android.yml` 在 PR 上运行 `testDebugUnitTest` 与 `assembleDebugAndroidTest`。

## 代码结构

```text
app/src/main/java/vmq/
├── data/         # 配置存储与仓库抽象
├── di/           # 轻量依赖组装
├── model/
├── network/      # 心跳与支付回调网络请求
├── notification/ # 通知分类与动作判定
├── parser/       # 配置 / 通知解析规则
├── ui/
│   ├── main/         # 主界面、ViewModel、Compose 页面
│   ├── notification/ # 通知监听 Service
│   ├── scan/         # 扫码界面
│   └── common/       # 共享 UI 文本抽象
├── usecase/      # 配置提交、心跳触发等业务用例
└── util/
```

## 配置格式

```text
example.com:8080/your-key
http://example.com:8080/your-key
https://example.com/your-key
https://example.com/api/your-key
```

- 最后一个 `/` 后面的内容会被当作通讯密钥
- 不带 `http(s)://` 时默认补 `http://`
- 纯 `https://example.com`（不带 key）会被判定为非法配置
- App 会把剩余主机部分作为服务端基础地址，并拼接：
  - `/api/v1/system/heartbeat`
  - `/api/v1/payments/notify`
- 请求方式：`POST` + `application/json`

## 核心限制

这是**基于通知文案解析**的自用方案，不是官方支付接口，受限于：

- 微信 / 支付宝通知文案变化
- 手机厂商后台限制
- 通知权限稳定性
- Android 系统版本差异

适合小规模、自维护场景。

## License

MIT