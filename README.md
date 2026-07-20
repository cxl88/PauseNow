# 停一下 PauseNow

PauseNow 是一个面向 Android 的短视频使用干预产品。当前处于阶段 3（Alpha 产品化）。

仓库结构：

- `android/` - 主应用（纯 Kotlin 原生，Jetpack Compose UI + Kotlin 控制引擎，单进程，本地优先）
- `deliberate-app/` - 项目官网（推广，Bun + web）
- `server/` - 后期后端服务（RuoYi scaffold，为前端提供接口；阶段尚未启动）

当前技术栈（android 主线）：

- Kotlin + Jetpack Compose
- 单进程原生：无 Flutter、无跨进程桥接
- 本地优先：不读取页面内容，不上传事件

快速检查（在 android/ 执行）：

```powershell
cd android
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease   # debug 签名 Alpha APK
```

Debug APK 输出：`android/app/build/outputs/apk/debug/app-debug.apk`
Release APK：`android/app/build/outputs/apk/release/app-release.apk`（debug 签名）

真机验证与阶段进展见：

- [项目进度](docs/project-progress.md)
- [OPPO 真机技术验证报告](docs/05_OPPO真机技术验证报告.md)
- [华为真机技术验证报告](docs/06_华为真机技术验证报告.md)
- [阶段 1-3 技术总结与下阶段规划](docs/07_停一下_技术总结与下阶段规划.md)

旧的 iOS/XcodeGen 配置（`project.yml`、`Configuration/`）仍保留在仓库中作为历史探索，不是当前 Android 原生路线的构建入口。
