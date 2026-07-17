# 停一下（PauseNow）Flutter + Kotlin 开发环境与技术验证设计

> 文档版本：v0.1  
> 更新日期：2026-07-16  
> 开发主机：Windows 11  
> 首发平台：Android  
> Flutter 基线：3.44.x Stable  
> Android 目标：compileSdk/targetSdk 36  
> JDK：17  
> 项目模式：Flutter Application + Kotlin 原生控制引擎

---

## 1. 文档目的

本文档用于指导开发人员在 Windows 11 上完成：

1. Flutter 与 Android 开发环境安装；
2. Android 真机调试；
3. Flutter + Kotlin 混合工程初始化；
4. UsageStatsManager 和 AccessibilityService 技术验证；
5. Flutter 与 Kotlin 的类型安全通信；
6. APK/AAB 构建；
7. 多厂商兼容性测试。

本文档优先保证“可验证”，不以第一天就形成最终工程为目标。

---

## 2. 技术基线

### 2.1 推荐版本

| 项目 | 基线 |
|---|---|
| 操作系统 | Windows 11 64 位 |
| Flutter | 3.44.x Stable |
| Dart | 使用 Flutter SDK 自带版本 |
| Android Studio | 当前 Stable |
| Android SDK Platform | Android 16 / API 36 |
| Android SDK Build Tools | 由 Android Studio 安装的最新稳定版本 |
| JDK | 17 |
| Kotlin | 使用 Flutter/Android 模板兼容版本 |
| Gradle | 使用 Flutter 模板生成的 Gradle Wrapper |
| AGP | 使用 Flutter 模板生成的兼容版本 |
| minSdk | 26 |
| targetSdk | 36 |
| compileSdk | 36 |
| 架构 | arm64-v8a 优先，兼容 armeabi-v7a/x86_64 按测试需要 |

选择 targetSdk 36 的原因：Google Play 从 2026-08-31 起要求新应用和更新面向 Android 16 / API 36，项目现在直接按 36 构建可避免临近发布时再次迁移。

### 2.2 为什么 minSdk 26

- 覆盖 Android 8.0 及以上；
- 减少旧系统行为差异；
- 核心验证设备应集中在 Android 13～16；
- 后续可根据真实用户设备数据调整。

---

## 3. Windows 11 环境准备

### 3.1 硬件建议

- 内存：16 GB 推荐，8 GB 可开发但模拟器体验较差；
- 磁盘：至少预留 30 GB；
- CPU 虚拟化：在 BIOS 中开启 Intel VT-x 或 AMD-V；
- 真机：至少一台 Android 13 及以上设备；
- USB 数据线：支持数据传输，不只是充电。

### 3.2 安装 Git

安装 Git for Windows 后验证：

```powershell
git --version
```

建议配置：

```powershell
git config --global core.autocrlf true
git config --global init.defaultBranch main
```

### 3.3 安装 Flutter SDK

推荐目录：

```text
C:\dev\flutter
```

不要放在：

- `C:\Program Files\`；
- 含中文或空格的深层目录；
- OneDrive 自动同步目录。

将以下目录加入用户 PATH：

```text
C:\dev\flutter\bin
```

验证：

```powershell
flutter --version
flutter channel stable
flutter upgrade
```

目标输出应显示 Flutter 3.44.x Stable 或后续兼容稳定版。

### 3.4 安装 Android Studio

安装时确保包含：

- Android SDK；
- Android SDK Platform；
- Android SDK Build-Tools；
- Android SDK Command-line Tools；
- Android Emulator；
- Platform-Tools。

Android Studio 插件：

- Flutter；
- Dart（通常随 Flutter 插件安装）。

### 3.5 配置 SDK

Android Studio：

```text
Settings
  → Languages & Frameworks
  → Android SDK
```

安装：

- Android 16 / API 36 SDK Platform；
- Android SDK Platform-Tools；
- Android SDK Build-Tools；
- Android SDK Command-line Tools latest；
- Android Emulator。

### 3.6 JDK 配置

优先使用 Android Studio 自带的 JBR/JDK，Gradle JDK 设置为 Android Studio 默认 JDK。

Android Gradle Plugin 8.x 要求 JDK 17；若构建提示 Java 版本错误，检查：

```powershell
java -version
flutter doctor -v
```

不要随意把系统 `JAVA_HOME` 指向旧 JDK 8 或 11。

### 3.7 接受 Android License

```powershell
flutter doctor --android-licenses
```

逐项输入 `y`。

### 3.8 环境验收

```powershell
flutter doctor -v
```

至少应通过：

- Flutter；
- Windows Version；
- Android toolchain；
- Android Studio；
- Connected device（连接真机后）。

本项目暂不要求：

- Visual Studio Windows Desktop；
- Chrome Web；
- Xcode。

---

## 4. Android 真机配置

### 4.1 开启开发者模式

一般路径：

```text
设置 → 关于手机 → 连续点击版本号 7 次
```

然后：

```text
设置 → 开发者选项 → USB 调试
```

不同厂商可能还需要：

- USB 安装；
- 通过 USB 验证应用；
- 关闭“仅充电”；
- 选择“文件传输”。

### 4.2 验证 ADB

连接手机并确认授权：

```powershell
adb devices
```

期望：

```text
List of devices attached
xxxxxxxx    device
```

若为 `unauthorized`：

1. 手机上确认 USB 调试授权；
2. 重新插拔；
3. 执行：

```powershell
adb kill-server
adb start-server
adb devices
```

### 4.3 验证 Flutter 设备

```powershell
flutter devices
```

---

## 5. 初始化项目

### 5.1 创建项目

将 `<company-domain>` 替换为公司反向域名，例如 `com.example`：

```powershell
cd C:\dev
flutter create `
  --org com.<company-domain> `
  --platforms=android `
  --android-language=kotlin `
  pause_now
cd pause_now
```

如果希望最小模板：

```powershell
flutter create --empty --android-language kotlin pause_now
```

### 5.2 运行默认项目

```powershell
flutter pub get
flutter run
```

### 5.3 建议标识

```text
项目目录：pause_now
应用显示名：停一下
英文品牌：PauseNow
Application ID：com.<company-domain>.pausenow
```

Application ID 一旦发布后不应修改。

---

## 6. 工程目录设计

```text
pause_now/
├── android/
│   └── app/src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/<company>/pausenow/
│       │   ├── MainActivity.kt
│       │   ├── bridge/
│       │   │   ├── NativeControlApiImpl.kt
│       │   │   └── NativeEventStream.kt
│       │   ├── accessibility/
│       │   │   ├── PauseAccessibilityService.kt
│       │   │   ├── ForegroundAppDetector.kt
│       │   │   └── EventDebouncer.kt
│       │   ├── usage/
│       │   │   ├── UsageAccessManager.kt
│       │   │   ├── UsageStatsRepository.kt
│       │   │   └── ScreenStateTracker.kt
│       │   ├── rules/
│       │   │   ├── NativeRuleEvaluator.kt
│       │   │   ├── ActivePassStore.kt
│       │   │   └── RuleSnapshotStore.kt
│       │   ├── intervention/
│       │   │   ├── InterventionActivity.kt
│       │   │   ├── InterventionLauncher.kt
│       │   │   └── HomeNavigator.kt
│       │   ├── permissions/
│       │   │   └── AndroidPermissionGateway.kt
│       │   └── boot/
│       │       └── BootReceiver.kt
│       └── res/
│           ├── xml/pause_accessibility_service.xml
│           └── values/strings.xml
├── lib/
│   ├── app/
│   │   ├── app.dart
│   │   ├── router.dart
│   │   └── bootstrap.dart
│   ├── core/
│   │   ├── errors/
│   │   ├── logging/
│   │   ├── storage/
│   │   └── time/
│   ├── features/
│   │   ├── onboarding/
│   │   ├── permissions/
│   │   ├── app_selection/
│   │   ├── rules/
│   │   ├── intervention/
│   │   └── reports/
│   ├── domain/
│   │   ├── models/
│   │   ├── repositories/
│   │   └── services/
│   ├── data/
│   │   ├── local/
│   │   └── repositories/
│   ├── bridge/
│   │   ├── native_control_api.dart
│   │   └── native_events.dart
│   └── main.dart
├── pigeons/
│   └── native_control_api.dart
├── test/
├── integration_test/
├── docs/
└── pubspec.yaml
```

---

## 7. Flutter 依赖策略

### 7.1 MVP 推荐

不固定本文档中的精确小版本，初始化时使用与 Flutter 3.44 兼容的最新稳定版本。

```yaml
dependencies:
  flutter:
    sdk: flutter
  flutter_riverpod:
  go_router:
  shared_preferences:
  uuid:
  intl:

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints:
  pigeon:
  mocktail:
```

### 7.2 后续再引入

当本地数据模型稳定后再评估：

- Drift/SQLite；
- Freezed；
- json_serializable；
- Sentry/Firebase Crashlytics；
- in_app_purchase；
- iCloud/云同步。

原则：技术 Spike 不因架构框架过重而延迟核心验证。

---

## 8. Flutter 与 Kotlin 通信

### 8.1 推荐 Pigeon

技术 Spike 可以先用 `MethodChannel`，Alpha 版本改为 Pigeon，以获得类型安全接口。

建议接口：

```dart
class PermissionSnapshot {
  bool usageAccessGranted;
  bool accessibilityEnabled;
  bool notificationGranted;
}

class ManagedAppInfo {
  String packageName;
  String displayName;
  Uint8List? iconPng;
}

class NativeRuleSnapshot {
  String ruleId;
  List<String> targetPackages;
  int sessionSeconds;
  int extensionSeconds;
  int maxExtensions;
  bool enabled;
}

@HostApi()
abstract class NativeControlHostApi {
  PermissionSnapshot getPermissionSnapshot();
  void openUsageAccessSettings();
  void openAccessibilitySettings();
  List<ManagedAppInfo> listSelectableApps();
  void syncRules(List<NativeRuleSnapshot> rules);
  void endCurrentSession(String packageName);
  void clearNativeState();
}

@FlutterApi()
abstract class NativeControlFlutterApi {
  void onTargetAppDetected(String packageName, int timestampMs);
  void onInterventionRequired(String packageName, String reason);
  void onServiceStateChanged(bool enabled);
}
```

### 8.2 数据同步原则

- Flutter 是规则编辑和展示的主数据源；
- Kotlin 必须保存一份可独立读取的“规则快照”；
- AccessibilityService 不能依赖 Flutter Engine 始终存活；
- 规则修改后 Flutter 主动调用 `syncRules`；
- 原生事件先写本地，再通知 Flutter；
- Flutter 未启动时，核心拦截仍应工作。

---

## 9. AndroidManifest 设计

> 以下是设计示例，实际权限和组件根据 Spike 结果最小化。

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission
        android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <queries>
        <intent>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent>
    </queries>

    <application
        android:label="停一下"
        android:icon="@mipmap/ic_launcher">

        <service
            android:name=".accessibility.PauseAccessibilityService"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/pause_accessibility_service" />
        </service>

        <activity
            android:name=".intervention.InterventionActivity"
            android:excludeFromRecents="true"
            android:exported="false"
            android:launchMode="singleTask"
            android:theme="@style/Theme.PauseNow.Intervention" />

        <receiver
            android:name=".boot.BootReceiver"
            android:enabled="true"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

注意：

- `PACKAGE_USAGE_STATS` 不是普通运行时弹窗权限，需要用户在系统“使用情况访问”页面授权；
- 不申请 `QUERY_ALL_PACKAGES`；
- `<queries>` 只声明业务确实需要的范围；
- BootReceiver 不应偷偷启动不符合系统限制的后台任务，只用于恢复状态检查和通知；
- 是否需要前台服务，应由 Spike 证明，默认不添加。

---

## 10. AccessibilityService 配置

`res/xml/pause_accessibility_service.xml` 示例：

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:notificationTimeout="100"
    android:canRetrieveWindowContent="false"
    android:description="@string/accessibility_service_description"
    android:settingsActivity="com.<company>.pausenow.MainActivity" />
```

核心原则：

- `canRetrieveWindowContent="false"`；
- 不声明不需要的事件；
- 不读取节点树；
- 不记录页面文本；
- 仅使用事件包名判断用户选择的目标应用进入前台；
- 对重复事件去抖；
- 对系统界面、自身应用、桌面和输入法建立排除列表。

### 10.1 Kotlin 事件处理伪代码

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val packageName = event?.packageName?.toString() ?: return
    if (!supportedEvent(event.eventType)) return
    if (packageName == applicationContext.packageName) return
    if (debouncer.isDuplicate(packageName, event.eventTime)) return

    val snapshot = ruleStore.currentSnapshot()
    val decision = ruleEvaluator.evaluate(
        packageName = packageName,
        now = clock.now(),
        screenInteractive = screenStateTracker.isInteractive(),
        rules = snapshot.rules,
        activePass = passStore.get(packageName),
    )

    when (decision) {
        ALLOW -> Unit
        REQUIRE_INTERVENTION -> interventionLauncher.launch(packageName)
        PASS_EXPIRED -> interventionLauncher.launchExpired(packageName)
    }
}
```

---

## 11. UsageStatsManager 设计

用途：

- 读取用户授权后的应用使用历史；
- 汇总目标应用使用时长；
- 校验前后台事件；
- 每日统计。

不要用法：

- 每 100ms 轮询当前应用；
- 替代无障碍事件做实时控制；
- 扫描并上传全部应用使用情况；
- 未授权时声称统计准确。

权限跳转示例：

```kotlin
val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
context.startActivity(intent)
```

授权检测需要通过 AppOpsManager/UsageStats 查询结果组合验证，避免只判断 Manifest。

---

## 12. 应用列表与包可见性

### 12.1 MVP 原则

- 不申请 `QUERY_ALL_PACKAGES`；
- 只展示可启动应用；
- 过滤系统组件、输入法、桌面和“停一下”自身；
- 仅把用户选择的包名保存为目标；
- 不上传完整安装列表。

### 12.2 实现方式

通过 Launcher Intent 查询：

```kotlin
val intent = Intent(Intent.ACTION_MAIN).apply {
    addCategory(Intent.CATEGORY_LAUNCHER)
}
val activities = packageManager.queryIntentActivities(intent, 0)
```

结合 Manifest `<queries>` 中的 MAIN/LAUNCHER Intent。

若后续应用商店政策或设备行为导致列表不完整，应改为：

- 内置常见应用目录；
- 用户搜索并选择系统可见应用；
- 不以扩大包可见性为第一解决方式。

---

## 13. 干预 Activity 设计

### 13.1 为什么优先原生 Activity

AccessibilityService 需要在 Flutter 引擎未运行时也能快速触发。技术 Spike 阶段建议：

- 干预页先使用 Kotlin 原生 Activity；
- 验证启动限制、时延、返回目标应用和返回桌面；
- 稳定后再决定是否用缓存 FlutterEngine 渲染统一 UI。

### 13.2 两种实现

#### 方案 A：原生 InterventionActivity（Spike 推荐）

优点：

- 启动快；
- 原生依赖少；
- Flutter 进程状态影响小；
- 更容易定位系统问题。

缺点：

- UI 需要 Kotlin/Compose 或 XML 单独维护；
- 与 Flutter 主题需要同步。

#### 方案 B：缓存 FlutterEngine

优点：

- UI 一致；
- 业务逻辑复用。

缺点：

- 引擎冷启动延迟；
- 后台拉起限制更复杂；
- 内存占用更高；
- Flutter 引擎生命周期更难管理。

决策门：方案 A 的 P95 启动时延稳定后，再评估方案 B。

---

## 14. 通行会话与计时

### 14.1 不依赖单一 Timer

不能只依赖 Dart `Timer` 或 Kotlin 内存定时器，因为：

- 进程可能被杀；
- 手机可能锁屏；
- 系统可能休眠；
- 时间可能被用户修改。

必须持久化：

```text
startedAtWallClock
startedAtElapsedRealtime
expiresAtWallClock
durationSeconds
packageName
extensionCount
bootSessionId
```

### 14.2 双时钟校验

- 同一开机周期优先使用 `SystemClock.elapsedRealtime()`；
- 跨重启恢复时使用壁钟时间并重新计算；
- 发现系统时间异常跳变时，将会话标记为需要重新确认；
- 实际使用时长由前台事件和屏幕交互状态估算，不等同于通行窗口总时长。

---

## 15. 本地存储

### 15.1 Spike

- Kotlin：SharedPreferences/DataStore 保存规则快照和活动通行；
- Flutter：SharedPreferences 保存引导、UI 偏好和简化规则。

### 15.2 Alpha

统一为：

- Flutter 层：Drift/SQLite 作为业务主库；
- Kotlin 层：DataStore 保存原生执行所需快照；
- 原生事件通过队列同步回 Flutter 主库。

### 15.3 事件保留

- 原始事件默认保留 30 天；
- 每日聚合长期保留；
- 用户可全部清除；
- 日志不得包含第三方页面内容。

---

## 16. 启动 Spike 的最小代码顺序

### Step 1：创建 Flutter 项目

```powershell
flutter create --org com.<company-domain> --platforms=android --android-language=kotlin pause_now
cd pause_now
flutter run
```

### Step 2：创建权限状态页

只实现：

- Usage Access 状态；
- Accessibility 状态；
- 跳转系统设置；
- 返回自动刷新。

### Step 3：创建 AccessibilityService

先只打印：

```text
timestamp
eventType
packageName
```

调试日志中不得打印页面文本。

### Step 4：目标包检测

临时在本地调试配置中写一个目标包名；正式版本改为用户选择。

### Step 5：启动干预 Activity

打开目标应用后，验证：

- 能触发；
- 不无限循环；
- 不重复弹；
- 能返回桌面；
- 能领取通行后返回目标应用。

### Step 6：加入 5 分钟通行

持久化 ActivePass，验证：

- 5 分钟内不拦截；
- 到时再次拦截；
- 重启应用后通行状态合理。

### Step 7：接入 Flutter UI

Flutter 负责：

- 目标应用选择；
- 规则编辑；
- 今日报告；
- 权限流程。

---

## 17. 调试命令

### 17.1 Flutter

```powershell
flutter analyze
flutter test
flutter run -v
flutter logs
```

### 17.2 ADB

```powershell
adb devices
adb logcat
adb shell dumpsys accessibility
adb shell dumpsys usagestats
adb shell dumpsys package com.<company-domain>.pausenow
```

筛选日志：

```powershell
adb logcat | Select-String "PauseNow"
```

清空日志：

```powershell
adb logcat -c
```

### 17.3 检查 Activity

```powershell
adb shell dumpsys activity activities
```

### 17.4 卸载重装

```powershell
adb uninstall com.<company-domain>.pausenow
flutter run
```

注意：卸载会清除授权和本地数据，适合首次流程回归。

---

## 18. 测试矩阵

| 分类 | 测试项 |
|---|---|
| 权限 | 首次拒绝、允许、关闭、再次开启 |
| 生命周期 | Flutter 进程存活/被杀、Service 存活/重启 |
| 屏幕 | 锁屏、解锁、息屏、分屏 |
| 导航 | 目标应用→干预页→目标应用、目标应用→桌面 |
| 时间 | 5 分钟到期、系统时间修改、时区修改、重启 |
| 事件 | 快速来回切换、连续打开 50 次、通知跳转 |
| 厂商 | 小米、OPPO、vivo、华为/荣耀、原生 Android |
| 电量 | 8 小时待机、2 小时重度使用 |
| 数据 | 清空、升级、崩溃后恢复 |
| 隐私 | 不读取节点文本、不上传安装列表 |

---

## 19. 构建

### 19.1 Debug APK

```powershell
flutter build apk --debug
```

### 19.2 Release APK

```powershell
flutter build apk --release
```

按 ABI 分包：

```powershell
flutter build apk --release --split-per-abi
```

### 19.3 AAB

```powershell
flutter build appbundle --release
```

输出通常位于：

```text
build\app\outputs\flutter-apk\
build\app\outputs\bundle\release\
```

### 19.4 安装到真机

```powershell
flutter install
```

或：

```powershell
adb install -r path\to\app-release.apk
```

---

## 20. 签名与密钥

正式测试前创建独立 keystore：

```powershell
keytool -genkeypair -v `
  -keystore pausenow-release.jks `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000 `
  -alias pausenow
```

要求：

- keystore 不提交 Git；
- 密码不写入仓库；
- 至少保留两份离线备份；
- 建立 `key.properties.example`，不建立真实 `key.properties`；
- Google Play 发布时启用 Play App Signing。

`.gitignore`：

```gitignore
*.jks
*.keystore
android/key.properties
.env
```

---

## 21. 代码质量门槛

每次合并前：

```powershell
dart format --set-exit-if-changed .
flutter analyze
flutter test
flutter build apk --debug
```

最低要求：

- 无 analyzer error；
- 核心 RuleEvaluator 有单元测试；
- Pigeon API 变更同步生成；
- Manifest 权限变更需要代码评审；
- 无障碍服务中禁止加入节点内容读取；
- 日志中不包含用户应用内容。

---

## 22. 常见问题

### 22.1 `flutter doctor` 找不到 Android SDK

检查：

- Android Studio 是否安装 SDK；
- `ANDROID_HOME` 是否错误；
- SDK Command-line Tools 是否安装；
- 重启终端。

### 22.2 Gradle 报 Java 版本错误

确保 Gradle 使用 JDK 17，不要使用旧 Java 11/8。

### 22.3 手机能被 ADB 看到但 Flutter 看不到

```powershell
adb kill-server
adb start-server
flutter devices
```

### 22.4 无障碍服务已开但收不到事件

检查：

- 服务 XML；
- Manifest；
- 事件类型；
- 厂商电池限制；
- 服务是否崩溃；
- `adb shell dumpsys accessibility`。

### 22.5 干预页无限弹出

常见原因：

- 未排除自身包名；
- 返回目标应用后未创建有效通行；
- 同一事件未去抖；
- WindowStateChanged 多次触发；
- 启动干预页本身又产生监听事件。

### 22.6 统计比系统屏幕时间偏差大

UsageStats 是聚合和事件数据，厂商实现也可能存在差异。产品应：

- 标记为估算；
- 只统计用户选中的目标应用；
- 将 Accessibility 事件与 UsageStats 交叉校验；
- 不承诺与系统数字完全一致。

---

## 23. 环境安装完成验收表

- [ ] `git --version` 正常；
- [ ] `flutter --version` 正常；
- [ ] Flutter 为 Stable；
- [ ] `flutter doctor -v` Android toolchain 通过；
- [ ] API 36 已安装；
- [ ] JDK 17 生效；
- [ ] Android Studio Flutter/Dart 插件安装；
- [ ] 真机 USB 调试连接；
- [ ] `flutter run` 可启动默认应用；
- [ ] `flutter build apk --debug` 成功；
- [ ] Usage Access 设置可跳转；
- [ ] Accessibility 设置可跳转；
- [ ] 能接收目标应用包名事件；
- [ ] 不读取页面节点内容。

---

## 24. 官方参考

- Flutter 安装：<https://docs.flutter.dev/install>
- Flutter Android 环境：<https://docs.flutter.dev/platform-integration/android/setup>
- 创建 Flutter 应用：<https://docs.flutter.dev/reference/create-new-app>
- Flutter Android 构建发布：<https://docs.flutter.dev/deployment/android>
- Flutter 3.44：<https://docs.flutter.dev/release/whats-new>
- Android JDK：<https://developer.android.com/build/jdks>
- Android UsageStatsManager：<https://developer.android.com/reference/android/app/usage/UsageStatsManager>
- Android AccessibilityService：<https://developer.android.com/guide/topics/ui/accessibility/service>
- Android 包可见性：<https://developer.android.com/training/package-visibility>
- Android 前台服务：<https://developer.android.com/develop/background-work/services/fgs>
- Google Play 目标 API：<https://developer.android.com/google/play/requirements/target-sdk>
- Google Play Accessibility API 政策：<https://support.google.com/googleplay/android-developer/answer/10964491>

---

## 25. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v0.1 | 2026-07-16 | 创建 Windows 11、Flutter 3.44、Kotlin、API 36 技术验证文档 |
