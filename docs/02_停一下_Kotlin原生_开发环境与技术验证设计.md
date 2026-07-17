# 停一下（PauseNow）Kotlin 原生 开发环境与技术验证设计

> 文档版本：v0.2  
> 更新日期：2026-07-17  
> 开发主机：Windows 11  
> 首发平台：Android  
> 技术栈：Kotlin + Jetpack Compose（纯原生，单进程）  
> Android 目标：compileSdk/targetSdk 36  
> JDK：17  
> 项目模式：Kotlin 原生 Application（Gradle Kotlin DSL）

---

## 1. 文档目的

本文档用于指导开发人员在 Windows 11 上完成：

1. JDK 与 Android SDK 环境安装；
2. Android 真机调试；
3. 纯 Kotlin 原生（Compose）工程初始化与维护；
4. UsageStatsManager 和 AccessibilityService 技术验证；
5. 单进程内 UI 与控制引擎通信（无跨进程桥接）；
6. APK 构建；
7. 多厂商兼容性测试。

本文档优先保证“可验证”，不以第一天就形成最终工程为目标。

> 历史：本文档原为 Flutter + Kotlin 方案，2026-07-17 修订为纯 Kotlin 原生。移除 Flutter SDK、Dart、MethodChannel/EventChannel/Pigeon 桥接。

---

## 2. 技术基线

### 2.1 推荐版本

| 项目 | 基线 |
|---|---|
| 操作系统 | Windows 11 64 位 |
| JDK | 17 |
| Android Studio | 当前 Stable |
| Android SDK Platform | Android 16 / API 36 |
| Android SDK Build Tools | 36.0.0 |
| Kotlin | 2.3.20 |
| AGP | 9.0.1 |
| Gradle | 由 gradle-wrapper 指定 |
| Jetpack Compose | BOM 2024.09.00（compose-compiler 由 Kotlin 插件内置） |
| minSdk | 26 |
| targetSdk | 36 |
| compileSdk | 36 |
| 架构 | arm64-v8a 优先，兼容 armeabi-v7a/x86_64 按测试需要 |

选择 targetSdk 36 的原因：Google Play 从 2026-08-31 起要求新应用和更新面向 Android 16 / API 36，项目现在直接按 36 构建可避免临近发布时再次迁移。

### 2.2 为什么 minSdk 26

- 覆盖 Android 8.0 及以上；
- 减少旧系统行为差异；
- 核心验证设备应集中在 Android 13～16；
- `java.time` 等现代 API 可直接使用；
- 后续可根据真实用户设备数据调整。

### 2.3 为什么纯 Kotlin 原生（不再用 Flutter）

- 阶段 1 只需 Android，无需跨平台 UI；
- 单进程原生，AccessibilityService 与 UI 同进程，事件经 `ForegroundEventBus` 直达，省去 MethodChannel/Pigeon；
- 移除 Flutter Engine 与 Dart 工具链，降低体积与构建复杂度；
- Kotlin 直接接入系统 API，Compose 满足产品 UI 开发速度。

---

## 3. Windows 11 环境准备

### 3.1 硬件建议

- 内存：16 GB 推荐，8 GB 可开发但模拟器体验较差；
- 磁盘：至少预留 20 GB；
- CPU 虚拟化：在 BIOS 中开启 Intel VT-x 或 AMD-V；
- 真机：至少一台 Android 13 及以上设备；
- USB 数据线：支持数据传输，不只是充电。

### 3.2 安装 Git

```powershell
git --version
```

建议配置用户名和邮箱，并设定行尾为仓库默认（Windows 下 `core.autocrlf=true` 由 Git 自动处理）。

### 3.3 安装 JDK 17

下载并安装 Temurin / Oracle JDK 17，设置 `JAVA_HOME` 指向 JDK 目录。验证：

```powershell
java -version
echo $env:JAVA_HOME
```

输出应为 `17.x`。

### 3.4 安装 Android Studio 与 SDK

Android Studio 自带 SDK Manager。安装后确认：

- Android SDK Platform 36；
- Android SDK Build-Tools 36.0.0；
- Android SDK Platform-Tools（含 ADB）；
- Android SDK Command-line Tools（latest）。

SDK 许可协议必须由账号/设备所有者亲自确认：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" `
  "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

仓库 `android/local.properties`（gitignored）需指向 SDK：

```properties
sdk.dir=C:\\Android\\Sdk
```

### 3.5 验证工具链

```powershell
java -version
adb version
cd android
.\gradlew.bat --version
```

---

## 4. Android 真机调试

1. 手机开启“开发者选项”和“USB 调试”；
2. USB 连接后在手机授权此电脑；
3. 验证连接：

```powershell
adb devices -l
```

状态应为 `device`。若为 `unauthorized`，在手机弹窗授权。

---

## 5. Kotlin 原生工程结构

仓库 `android/` 即为 Gradle 根工程：

```
android/
├── settings.gradle.kts        # 插件与仓库声明
├── build.gradle.kts           # 根工程（仅声明插件 apply false）
├── gradle.properties          # AndroidX 等
├── gradle/wrapper/           # Gradle Wrapper
├── gradlew / gradlew.bat
└── app/
    ├── build.gradle.kts       # :app 模块（application + compose）
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── kotlin/com/pausenow/app/
        │   │   ├── MainActivity.kt          # ComponentActivity + setContent
        │   │   ├── ui/                       # SpikeScreen, SpikeViewModel
        │   │   ├── model/                    # PermissionSnapshot, DeviceSnapshot
        │   │   ├── events/                   # ForegroundEventBus
        │   │   ├── accessibility/            # 检测核心
        │   │   └── permissions/              # AndroidPermissionGateway
        │   └── res/                          # 主题、字符串、无障碍配置
        └── test/kotlin/                      # JVM 单元测试
```

关键依赖（`app/build.gradle.kts`）：

- `androidx.compose:compose-bom`
- `androidx.compose.material3:material3`
- `androidx.activity:activity-compose`
- `androidx.lifecycle:lifecycle-viewmodel-compose` / `lifecycle-runtime-compose`
- `androidx.core:core-ktx`
- `junit:junit`（testImplementation）

Compose 编译器由 `org.jetbrains.kotlin.plugin.compose`（与 Kotlin 同版本）提供，无需单独指定 `composeOptions.kotlinCompilerExtensionVersion`。

---

## 6. 技术验证项

### 6.1 Usage Access

- 权限：`PACKAGE_USAGE_STATS`（`tools:ignore="ProtectedPermissions"`）；
- 状态查询：`AppOpsManager.checkOpNoThrow(OPSTR_GET_USAGE_STATS, ...)`；
- 跳转：`Settings.ACTION_USAGE_ACCESS_SETTINGS`；
- 阶段 1 仅验证授权状态，不读取使用时长。

### 6.2 AccessibilityService

- 仅监听 `typeWindowStateChanged`；
- `canRetrieveWindowContent="false"`、`isAccessibilityTool="false"`；
- 自身包、System UI、设置、权限控制器、桌面、输入法排除；
- 同一前台会话包名去重（`EventDebouncer`）；
- 事件落盘（`ForegroundEventStore`，SharedPreferences，最近 50 条）；
- 事件推送（`ForegroundEventBus`，单进程 observer）。

代码不调用 `getRootInActiveWindow()`、不读 `event.text`、不遍历节点树、不自动点击、不扫描安装列表、不联网。允许的本地事件字段只有 `detectedAtMs`、`eventType`、`packageName`。

### 6.3 单进程通信（无桥接）

UI（`SpikeViewModel`）直接持有 `AndroidPermissionGateway` 与 `ForegroundEventStore`，并注册 `ForegroundEventBus` 监听者。AccessibilityService 在事件线程 `publish`，ViewModel 的监听者切到主线程更新 `StateFlow`。

不再使用 MethodChannel / EventChannel / Pigeon。

---

## 7. 构建与验证命令

仓库根目录执行：

```powershell
cd android
.\gradlew.bat testDebugUnitTest      # JVM 单元测试
.\gradlew.bat lintDebug              # Android lint（含隐私静态约束建议人工配合 scripts/check-stage1-static.ps1）
.\gradlew.bat assembleDebug          # Debug APK
```

Debug APK 输出：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

安装到真机：

```powershell
adb install -r android\app\build\outputs\apk\debug\app-debug.apk
```

一键环境+构建检查：

```powershell
.\scripts\android-env-check.ps1
```

隐私静态检查（manifest / 无障碍配置不变量）：

```powershell
.\scripts\check-stage1-static.ps1
```

---

## 8. 多厂商兼容性测试

阶段 1 验收要求三台不同厂商/系统真机，每台单机识别成功率不低于 95%，不能用三台平均值掩盖单机失败。采样脚本：

```powershell
.\scripts\run-stage1-device-check.ps1 `
  -TargetPackage com.ss.android.ugc.aweme `
  -Iterations 20
```

详细步骤与三机验收表见 [阶段 1 验证文档](04_阶段1_权限与前台事件_Spike验证.md)。

厂商已知风险：

- 部分厂商省电策略会回收无障碍服务，需长时观察；
- 部分厂商桌面/系统 UI 包名不同，必要时扩展 `PackageExclusionPolicy`；
- 不为兼容厂商而读取页面内容或使用违规后台启动手段。

---

## 9. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v0.1 | 2026-07-16 | 创建 Flutter + Kotlin 开发环境与技术验证设计 |
| v0.2 | 2026-07-17 | 改为纯 Kotlin 原生（Compose）：移除 Flutter/Dart/桥接，重写环境、工程结构、通信与构建说明 |
