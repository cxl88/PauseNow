# 停一下 PauseNow

PauseNow 是一个面向 Android 的短视频使用干预产品。当前仓库处于阶段 1 技术 Spike：验证用户明确授权后，能否稳定获得第三方应用进入前台时的包名事件。

当前技术栈：

- Flutter：权限披露、状态与事件验证 UI；
- Kotlin：Usage Access 状态、AccessibilityService、事件排除/去抖和本地记录；
- MethodChannel / EventChannel：Flutter 与原生通信；
- 本地优先：不读取页面内容，不上传事件。

快速检查：

```powershell
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
```

阶段 1 的真机步骤、自动采证命令和退出标准见 [docs/04_阶段1_权限与前台事件_Spike验证.md](docs/04_阶段1_权限与前台事件_Spike验证.md)。

旧的 iOS/XcodeGen 配置仍保留在仓库中作为历史探索，不是当前 Android 优先路线的构建入口。
