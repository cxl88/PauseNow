# 阶段 1：权限与前台事件 Spike 验证

> 状态：代码与自动化已完成（纯 Kotlin 原生），等待安卓真机采证  
> 更新日期：2026-07-17  
> 应用 ID：`com.pausenow.app`

## 1. 本阶段目标

仅证明 Android 在用户明确授权后，可以稳定获得第三方应用进入前台时的包名事件。

本阶段不包含：

- 应用拦截；
- 干预 Activity；
- 5 分钟通行；
- 目标应用选择；
- 订阅与账号；
- 页面文字或节点树读取。

上述干预能力属于阶段 2。

## 2. 已实现

- Jetpack Compose 权限状态与事件验证 UI（`SpikeScreen` + `SpikeViewModel`）；
- 返回 App 后自动刷新 Usage Access 与 Accessibility 状态；
- 无障碍权限醒目披露，未勾选确认时不能跳转授权；
- `AndroidPermissionGateway` 权限与本地事件查询（单进程直调，无 MethodChannel）；
- 单进程事件总线 `ForegroundEventBus` 实时包名事件；
- `AccessibilityService` 仅监听 `TYPE_WINDOW_STATE_CHANGED`；
- `canRetrieveWindowContent=false`；
- 自身包、System UI、设置、权限控制器和常见桌面包排除；
- 按前台包切换去重，同一前台会话只记录一次；
- 本地最多保存 50 条事件；
- Logcat 输出时间、事件类型与包名；
- 设备型号、Android/API、构建号与 App 版本证据；
- 一键复制本地验收 JSON；
- Kotlin 去抖/排除策略/事件总线单元测试；
- 单机重复打开与成功率计算脚本。

项目（含 Debug/Profile 变体）未声明 `QUERY_ALL_PACKAGES`、联网权限或悬浮窗权限。

## 3. 隐私核对

无障碍配置：

```xml
android:accessibilityEventTypes="typeWindowStateChanged"
android:canRetrieveWindowContent="false"
android:isAccessibilityTool="false"
```

代码不调用：

- `getRootInActiveWindow()`；
- `event.text`；
- 节点树遍历；
- 自动点击或全局手势；
- 安装应用列表扫描；
- 网络上传。

允许的本地事件字段只有：

```text
detectedAtMs
eventType
packageName
```

## 4. 构建前环境

```powershell
java -version
adb version
cd android
.\gradlew.bat --version
```

项目基线：Kotlin 2.3.20、AGP 9.0.1、JDK 17、compileSdk/targetSdk 36、minSdk 26、Jetpack Compose。

首次准备：

```powershell
cd android
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

APK 默认输出：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## 5. 单机验证步骤

1. 使用 USB 连接安卓真机并开启 USB 调试。
2. 安装 Debug APK。
3. 打开“停一下”，确认两项权限用途。
4. 在系统中开启“使用情况访问”。
5. 阅读醒目披露，勾选确认，再进入系统设置开启“停一下前台应用检测”。
6. 返回 App，确认两个状态都显示“已开启”。
7. 手工切换到抖音或其他目标 App，确认页面出现包名事件。
8. 运行自动采样脚本。

抖音中国版常见包名为 `com.ss.android.ugc.aweme`，必须以设备实际安装包名为准：

```powershell
adb shell pm list packages | Select-String "aweme|douyin"
```

运行 20 次采样：

```powershell
.\scripts\run-stage1-device-check.ps1 `
  -TargetPackage com.ss.android.ugc.aweme `
  -Iterations 20
```

脚本会：

- 检查设备与无障碍服务状态；
- 清空 Logcat；
- 重复启动目标 App 并回到桌面；
- 统计目标包事件；
- 将原始日志、设备信息和 `result.json` 写入 `docs/evidence/`；
- 低于 95% 时返回失败退出码。

脚本不会绕过系统授权，也不会用 ADB 强制开启无障碍服务。

## 6. 三机验收表

| 设备 | Android / API | 厂商 | 打开次数 | 检出次数 | 成功率 | 2 小时耗电观察 | 结果 | 证据路径 |
|---|---|---|---:|---:|---:|---|---|---|
| A 主力机 | Android 14 / API 34 | OPPO PJU110 (A2m 5G, ColorOS 14) | 20 | 3 | 15% | 未做 | 失败（后台事件被 ColorOS 节流；前台可见时正常） | `docs/evidence/stage1-PF5TRO6LV4KJKBZT-20260717-214900/` |
| B 不同厂商 | Android 12 / API 31 | HUAWEI NOH-AL10 (Mate 40 Pro, HarmonyOS 4.2) | 20 | 0 | 0% | 未做 | 失败（后台事件被完全冻结；前台可见时正常） | `docs/evidence/stage1-4CN0222207000028-20260717-224456/` |
| C Android 13～16 | 待填 | 待填 | 20 | 待填 | 待填 | 待填 | 待验 | 待填 |

**A 机（OPPO ColorOS 14）说明**：机制本身可用（前台时事件全到、不读内容），但 App 退后台后 ColorOS 节流/冻结无障碍事件，导致批量采证仅 15%。即使加自启动+后台锁，进程不再被杀但事件仍节流。结论：纯后台 `AccessibilityService` 在 OPPO 不可靠，需阶段 2 前台服务 + 厂商自启动引导后再复测。此为 3 台真机中的第 1 台，已如实暴露阶段 1 核心风险。

**B 机（华为 HarmonyOS 4.2）说明**：机制本身可用（前台可见时事件正常），但退后台后**事件被完全冻结**（0/20，0%），比 OPPO 更极端。进程仍存活但无事件下发。两台国产 ROM 的共同结论：**前台 `AccessibilityService` 可用，后台不可靠**，阶段 2 必须引入前台服务（常驻通知）+ 厂商自启动/后台锁引导。此为 3 台真机中的第 2 台，进一步确认了核心风险的普遍性。

每台设备都必须单独达到至少 95%，不能用三台设备的总平均值掩盖单机失败。

## 7. 完成门槛

- [x] 权限状态查询与设置跳转已实现；
- [x] 醒目披露已实现；
- [x] 无障碍服务只监听必要事件；
- [x] 包名日志、本地记录、排除与去抖已实现；
- [x] 不读取页面内容；
- [x] 自动采证脚本已实现；
- [ ] Android lint 通过；
- [ ] Kotlin 单元测试通过；
- [ ] Debug APK 构建通过；
- [ ] 3 台真机均完成授权；
- [ ] 3 台真机识别成功率均不低于 95%；
- [ ] 2 小时使用及 8 小时待机无明显异常耗电。

只有所有未完成项都有真实证据后，阶段 1 才能标记为“完成”，随后进入阶段 2。

## 8. 当前环境结论

当前 Windows 主机已安装 JDK 17 和 Android Command-line Tools。Android SDK 许可协议必须由账号/设备所有者亲自确认，因此 API 36、Build Tools 和 ADB 尚未完成安装。请在 PowerShell 执行：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" `
  "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```

此外，当前没有安卓真机连接，所以本文件不声称真机验收已经通过。完成许可确认并连接设备后，运行 `scripts/android-env-check.ps1` 与第 5～7 节即可完成阶段门验收。
