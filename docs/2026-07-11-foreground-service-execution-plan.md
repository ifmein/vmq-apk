# VMQ 前台常驻增强执行方案

## 1. 目标 🎯

在当前 `NotificationListenerService` 架构不大改方向的前提下，新增一个独立的前台常驻服务，用来提升进程存活性，从而**尽可能增强 `NeNotificationService` 不掉线的概率**。

本方案面向当前仓库的**自用场景**，默认你会自行处理电池优化白名单、后台保护、锁后台等系统设置。

---

## 2. 推荐方案摘要 ✅

采用**方案 A：独立 `PersistentForegroundService`**。

职责拆分如下：

- `NeNotificationService`
  - 只负责通知监听
  - 只负责解析通知
  - 只负责支付回调
  - 只负责上报监听连接状态

- `PersistentForegroundService`
  - 负责常驻通知
  - 负责 `startForeground(...)`
  - 负责心跳循环
  - 负责 `WakeLock`
  - 负责显示简要运行状态

这样做的核心收益：

1. 把“监听”与“保活”职责拆开，结构更稳
2. 即使监听服务发生系统重绑，前台服务仍可维持进程活跃
3. 心跳逻辑不再绑死在 `NotificationListenerService` 生命周期上

---

## 3. 本次范围 / 非范围 📌

### 本次范围

1. 新增前台常驻服务
2. 将心跳循环从 `NeNotificationService` 挪到前台服务
3. 将 `WakeLock` 从 `NeNotificationService` 挪到前台服务
4. 增加前台通知渠道与通知内容
5. 增加监听状态记录，用于通知展示
6. 在 `MainActivity` 启动时拉起前台服务
7. 补充对应单元测试

### 非范围

1. 不做 Boot 开机自启
2. 不做复杂自动恢复逻辑
3. 不做通知按钮停止服务
4. 不引入 `WorkManager`
5. 不改动支付解析规则与网络协议

说明：以上非范围并不是不能做，而是当前阶段不必做，先用最小方案换取最直接的稳定性收益。

---

## 4. 前台通知风格 🪧

采用**状态型通知**，这是当前最平衡的选择。

通知建议包含：

- 标题：`V免签监控运行中`
- 文案：
  - 监听状态：已连接 / 未连接
  - 配置状态：已配置 / 未配置
  - 心跳状态：最近成功时间 / 最近失败

示例：

- `监听已连接 · 已配置 · 心跳 12:30:15`
- `监听未连接 · 已配置 · 等待恢复`
- `监听已连接 · 未配置 · 等待配置`

目的不是做复杂运维面板，而是让你从状态栏一眼判断应用是否大致正常。

---

## 5. 架构与数据流 🧩

### 5.1 启动链路

1. `MainActivity.onCreate()` 启动时
2. 通过 `ForegroundServiceController` 拉起 `PersistentForegroundService`
3. `PersistentForegroundService` 创建通知渠道并调用 `startForeground(...)`
4. 服务启动后进入心跳循环
5. 服务以 `START_STICKY` 返回，尽量在系统回收后被重建

### 5.2 监听链路

1. 系统继续绑定 `NeNotificationService`
2. `onListenerConnected()` 时记录“监听已连接”
3. `onListenerDisconnected()` 时记录“监听已断开”
4. `onNotificationPosted()` 时继续处理支付通知，并顺手记录最近监听事件时间

### 5.3 心跳链路

1. 前台服务循环读取 `ConfigStore`
2. 若配置完整，则调用现有 `HeartbeatService.sendHeartbeat(config)`
3. 根据结果更新“最近成功/失败状态”
4. 每轮后刷新前台通知文案

---

## 6. 文件变更清单 📂

### 需要修改

1. `app/src/main/AndroidManifest.xml`
   - 新增前台服务权限
   - 注册 `PersistentForegroundService`

2. `app/src/main/java/vmq/ui/main/MainActivity.kt`
   - 启动前台服务
   - 保留现有 listener toggle 逻辑

3. `app/src/main/java/vmq/ui/notification/NeNotificationService.kt`
   - 移除心跳循环
   - 移除 `WakeLock`
   - 仅保留监听/解析/支付回调/状态记录

4. `app/src/main/java/vmq/di/AppContainer.kt`
   - 视最终落地方式，补充状态存储对象获取方法

5. `app/src/main/res/values/strings.xml`
   - 新增前台服务通知相关文案

### 需要新增

1. `app/src/main/java/vmq/ui/foreground/PersistentForegroundService.kt`
   - 前台常驻服务主体

2. `app/src/main/java/vmq/ui/foreground/ForegroundServiceController.kt`
   - 统一封装启动前台服务入口

3. `app/src/main/java/vmq/ui/foreground/ForegroundNotificationFactory.kt`
   - 负责通知渠道与通知内容构建

4. `app/src/main/java/vmq/data/ListenerStatusStore.kt`
   - 记录监听连接状态、最近事件时间、最近心跳状态

5. `app/src/test/java/vmq/data/ListenerStatusStoreTest.kt`
   - 状态存储测试

6. `app/src/test/java/vmq/ui/foreground/ForegroundNotificationFactoryTest.kt`
   - 文案拼装测试

如实现时发现 `ForegroundNotificationFactory` 测试不易稳定，可改成抽离纯文本格式化类进行测试，避免死测 Android 通知对象本身。

---

## 7. Manifest 调整方案 🛠️

### 7.1 新增权限

建议新增：

- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC`

原因：

- 当前目标 SDK 为 34
- 新增服务将使用 `foregroundServiceType="dataSync"`
- 当前用途最接近“持续网络心跳/同步型常驻任务”

### 7.2 新增服务声明

新增一个服务：

- `vmq.ui.foreground.PersistentForegroundService`
- `android:exported="false"`
- `android:foregroundServiceType="dataSync"`

说明：

- 该服务不对外暴露
- 只由应用内部启动
- 不替代 `NeNotificationService`，两者并存

---

## 8. 核心实现步骤 🚀

### 阶段 1：引入前台服务骨架

目标：先让应用具备稳定的前台常驻能力。

实施点：

1. 新增 `PersistentForegroundService`
2. 在 `onCreate()` / `onStartCommand()` 中创建通知渠道并 `startForeground(...)`
3. 返回 `START_STICKY`
4. 通知点击回到 `MainActivity`

阶段结果：

- 应用安装运行后可以看到常驻通知
- 系统会把应用视为正在执行前台任务

### 阶段 2：迁移心跳与 WakeLock

目标：把“保活相关逻辑”从 listener 中抽离。

实施点：

1. 将 `NeNotificationService` 内的 `heartbeatJob` 逻辑迁移到 `PersistentForegroundService`
2. 将 `acquireWakeLock()` / `releaseWakeLock()` 同步迁移
3. 保持原有 30 秒心跳间隔不变
4. 配置缺失时不报错，只更新通知文案为“等待配置”

阶段结果：

- 即使 listener 发生重绑，心跳仍由前台服务维持
- listener 文件职责显著收敛

### 阶段 3：增加监听状态记录

目标：让前台通知具备最基本的状态可见性。

实施点：

1. 新增 `ListenerStatusStore`
2. 在 `onListenerConnected()` 记录已连接
3. 在 `onListenerDisconnected()` 记录已断开
4. 在 `onNotificationPosted()` 记录最近事件时间
5. 在前台服务心跳成功/失败后记录最近心跳结果

建议保存字段：

- `isListenerConnected`
- `lastListenerConnectedAt`
- `lastNotificationEventAt`
- `lastHeartbeatSuccessAt`
- `lastHeartbeatErrorMessage`

阶段结果：

- 前台通知可显示“监听是否连接”和“最近心跳状态”
- 不需要复杂 UI，也能快速判断运行情况

### 阶段 4：收敛 `NeNotificationService`

目标：让监听服务只处理监听本身。

实施点：

1. 删除 listener 内的心跳启动/停止逻辑
2. 删除 listener 内的 `WakeLock`
3. 保留支付通知处理
4. 保留连接/断开 toast
5. 增加状态上报到 `ListenerStatusStore`

阶段结果：

- `NeNotificationService` 生命周期更干净
- 减少“监听服务既监听又保活”的耦合

### 阶段 5：主界面启动前台服务

目标：让前台常驻成为默认工作模式。

实施点：

1. 在 `MainActivity.onCreate()` 中启动前台服务
2. 启动时不要求配置已存在
3. 若未来需要，可再加“仅在监听权限开启后才启动”的判断

当前建议：

- 先直接启动
- 由前台通知自己显示“未配置”或“监听未连接”

阶段结果：

- 行为更直接
- 用户只需打开 App 一次即可进入常驻模式

---

## 9. 测试方案 🧪

遵循当前仓库测试规范，优先补**本地单元测试**。

### 必测项

1. `ListenerStatusStore`
   - 默认状态读取
   - 连接状态写入后可正确读取
   - 心跳成功/失败状态更新正确

2. 通知状态文本
   - 已连接 + 已配置 + 有成功心跳
   - 未连接 + 已配置
   - 已连接 + 未配置
   - 心跳失败文案

3. 心跳循环抽取后
   - 配置缺失时不发请求
   - 配置存在时调用 `HeartbeatService`
   - 失败时正确更新状态

### 本地验证命令

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

如需额外确认打包稳定，再执行：

```bash
./gradlew assembleDebugAndroidTest
```

---

## 10. 真机验收清单 📱

在你的自用机上至少检查以下项目：

1. 安装后打开 App，状态栏出现常驻通知
2. 通知标题和状态文案符合预期
3. listener 权限开启后，通知能显示“监听已连接”
4. 配置有效时，心跳状态会更新
5. 微信 / 支付宝通知到来时，支付回调仍正常工作
6. 锁屏一段时间后，前台通知仍存在
7. 切后台数小时后，再做一笔测试通知，listener 仍能收到

---

## 11. 风险与注意事项 ⚠️

1. 前台服务会显式显示常驻通知，这是方案的既定代价
2. 该方案是“显著增强不掉概率”，不是“绝对不掉”
3. `foregroundServiceType="dataSync"` 对当前用途最接近，但本质上仍属于自用型保活方案
4. 本次不做 Boot 自启，因此重启手机后仍需你手动打开一次 App

---

## 12. 实施顺序建议 🧭

建议按以下顺序改造：

1. Manifest + 前台服务骨架
2. 常驻通知与启动控制器
3. 心跳循环迁移
4. `WakeLock` 迁移
5. 监听状态存储
6. listener 收敛
7. 单元测试
8. 真机验证

这样可以每一步都保持可运行、可回退、可观察。

---

## 13. 最终判断 ✅

对于当前这个仓库，**最划算、最直接、最符合你目标的增强方式**，就是：

> **保留 `NotificationListenerService` 负责监听，再新增一个独立 `PersistentForegroundService` 负责前台常驻、心跳和保活。**

这会比继续把心跳和 `WakeLock` 塞在 `NeNotificationService` 里更稳，也更方便后续继续加你真正关心的“尽量不掉线”能力。
