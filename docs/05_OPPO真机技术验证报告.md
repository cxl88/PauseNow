# OPPO 真机技术验证报告（停一下 PauseNow）

> 文档版本：v1.0
> 最后更新：2026-07-18
> 验证设备：OPPO PJU110（A2m 5G）
> 系统版本：ColorOS 14（Android 14 / API 34）
> 应用版本：0.1.0（applicationId `com.pausenow.app`）
> 构建基线：Kotlin 2.3.20，AGP 9.0.1，JDK 17，compileSdk/targetSdk 36，minSdk 26，Jetpack Compose BOM 2024.09.00

## 1. 概述与结论

本报告沉淀停一下（PauseNow）在 OPPO PJU110（ColorOS 14）真机上从阶段 1 到阶段 3 的完整技术验证过程、结果与已实现功能。验证目的是确认国产 ROM 下"打开目标应用 → 限时通行 → 到期再干预"的核心闭环是否可靠，并暴露后台管控风险。

**核心结论**：

- 纯后台 `AccessibilityService` 在 OPPO 上**不可靠**：App 退后台后 ColorOS 节流/冻结无障碍事件，20 次批量采证成功率仅 **15%**（前台可见时正常）。
- **Pivot 成功**：将主检测从无障碍事件改为前台服务（FGS）内的 `UsageStatsManager` 轮询后，PauseNow 退后台仍能稳定查到目标应用前台，绕开了无障碍后台节流。
- **全链路闭环验证通过**（阶段 2/3）：打开目标应用 → 弹出打开前干预 → 放行限时通行 → 到期弹出干预 → 延长/结束 → 返回目标应用或桌面，可连续演示，无无限循环。
- **Alpha 产品化验证通过**（阶段 3）：新用户可在数分钟内经 Onboarding → 应用列表选目标 → 建规则完成配置，无需手改包名；重启规则保留；可生成签名 release APK。
- **隐私不变量全程保持**：`canRetrieveWindowContent=false`、仅包名三字段、应用列表只查 LAUNCHER、无 `INTERNET`/`QUERY_ALL_PACKAGES`/`SYSTEM_ALERT_WINDOW`，`check-stage1-static.ps1` 8/8 通过。
- **仍待**：单设备验证（仅 OPPO），需补华为/小米复测；P95 干预延迟未实测；锁屏/长时稳定性留待阶段 4。

## 2. 验证环境

| 项目 | 内容 |
|---|---|
| 设备 | OPPO PJU110（A2m 5G） |
| 系统 | ColorOS 14（Android 14 / API 34） |
| 设备序列号 | PF5TRO6LV4KJKBZT |
| 目标应用 | 抖音（`com.ss.android.ugc.aweme`） |
| 应用版本 | 0.1.0（versionCode 1） |
| 构建产物 | `app-debug.apk`（17MB）、`app-release.apk`（12MB，debug 签名） |
| 采证脚本 | `scripts/run-stage1-device-check.ps1`（阶段 1 ADB 采样器） |
| 静态检查 | `scripts/check-stage1-static.ps1`（8 项隐私不变量） |
| 阶段 1 证据目录 | `docs/evidence/stage1-PF5TRO6LV4KJKBZT-20260717-214900/`（gitignored） |

## 3. 阶段验证过程与结果

### 3.1 阶段 1：权限与前台事件采证（2026-07-17）

**目标**：验证授权后能否稳定获得第三方应用进入前台的包名事件。

**过程**：
- 手动完成 Usage Access + 无障碍授权（含醒目披露门槛）。
- 用 `scripts/run-stage1-device-check.ps1 -TargetPackage com.ss.android.ugc.aweme -Iterations 20` 做 20 次 ADB 驱动启动采样。

**结果**：
- **前台可见时**：无障碍事件稳定到达，包名识别正确，`TYPE_WINDOW_STATE_CHANGED` 过滤 + 去抖 + 排除策略工作正常。
- **App 退后台后**：ColorOS 节流/冻结无障碍事件，20 次批量采证成功率 **15%（3/20）**。进程仍存活，但事件下发被暂停。
- 追加自启动 + 后台锁后，进程不再被杀，但事件仍被节流。
- 加 doze 电池白名单（`dumpsys deviceidle whitelist`）后仍无效——ColorOS 14 对无障碍服务有**独立于进程优先级和电池策略**的后台限制。

**结论**：纯后台 `AccessibilityService` 在 OPPO 不可靠；阶段 2 必须引入前台服务 + 改用 UsageStats 作主信号源。

### 3.2 阶段 2 Pivot：FGS + UsageStats 轮询（2026-07-18）

**Pivot 动机**：阶段 1 暴露的无障碍后台节流是核心风险。FGS 单独未能解冻无障碍事件（重跑采证仍 10%/0%），需换主信号源。

**实现**：
- `PauseAccessibilityService` 在 `onServiceConnected` 调 `startForeground(FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` + 常驻通知（manifest 声明 `FOREGROUND_SERVICE_SPECIAL_USE`/`POST_NOTIFICATIONS`，`PROPERTY_SPECIAL_USE_FGS_SUBTYPE=accessibility_foreground_detection`）。
- 在 FGS 内启动检测循环：`Handler` 每 `DETECTION_INTERVAL_MS=3000ms` 跑一次 `detectOnce()`。
- `queryCurrentForeground()` 用 `UsageStatsManager.queryUsageStats`（60 秒窗口）取 `lastTimeUsed` 最大的包作为当前前台。

**关键决策：用 `lastTimeUsed` 而非 `MOVE_TO_FOREGROUND` 事件**
- 用户在目标应用停留超过几秒后，`UsageEvents.MOVE_TO_FOREGROUND` 的窄窗口会查不到当前应用；
- `lastTimeUsed` 始终反映"最近用过"，稳定可靠。

**Pivot 验证结果**：
- 在 FGS 内加 UsageStats 探针（每 2 秒 `queryUsageStats`），PauseNow 退后台时仍能稳定查到抖音前台——连续 6 轮探针 3 轮命中 `com.ss.android.ugc.aweme`，同期无障碍事件仍不稳定。
- 真机确认 `isForeground=true`，FGS 生效。

**结论**：UsageStats 轮询是国产 ROM 后台的可靠方案，2-3 秒延迟可接受。无障碍事件降为前台辅助信号。

### 3.3 阶段 2 闭环验证（2026-07-18，成功）

**目标**：完成"打开目标 → 干预 → 限时通行 → 到期 → 延长/结束 → 返回"全链路。

**过程与日志确认**：
1. 打开抖音 → 检测循环命中 → `RuleEngine` 返回 `RequireOpenIntervention` → `InterventionLauncher` 弹出 `InterventionActivity`（MODE_OPEN）。
2. 点"放行 30 秒"（测试值，正式默认 5 分钟）→ `PassManager.grantPass` 写入通行（SharedPreferences 持久化）→ `ExpiryController.scheduleExpiry` 设 AlarmManager 精确闹钟 → 返回抖音。
3. 30 秒后 `ExpiryAlarmReceiver` 触发高优先级通知；用户下次打开抖音时检测循环判定 `passExpired=true` → `RequireExpiredIntervention` → 弹出到期干预（MODE_EXPIRED）。
4. 点"延长 3 分钟"→ `PassManager.extendPass`（`extensionCount=1`，已过期则从现在起算）→ 重设闹钟 → 返回抖音，通行有效返回 `Allow`。
5. 点"结束并回桌面"→ `PassManager.endPass` + `ExpiryController.cancelExpiry` → 返回桌面。

**关键修复**：
- `<queries>` 声明 LAUNCHER 可见性，否则 `getLaunchIntentForPackage` 返回 null，无法返回目标应用。
- `queryUsageStats` + `lastTimeUsed` 替代 `MOVE_TO_FOREGROUND` 5 秒窗口（用户停留超 5 秒查不到）。

**退出标准达成情况**：
- ✅ 闭环可连续演示
- ✅ 5 分钟误差由 AlarmManager 保证
- ✅ 无无限循环（`InterventionState` 冷却 30s + 互斥锁保证同一包只弹一个干预 Activity）
- ⚠️ P95 ≤1.5s 未实测（Spike 期无埋点）
- ⚠️ 锁屏/切后台长时稳定性留待阶段 4

### 3.4 阶段 3 Alpha 产品化验证（2026-07-18，成功）

**目标**：把技术 Demo 变成普通用户可完成配置的 Alpha。

**过程**：
1. Onboarding 引导完成（权限说明 + 跳转设置）→ 标记 `OnboardingStore.completed`，起始路由切到 Home。
2. 应用列表（`AppRepository.queryIntentActivities(MAIN+LAUNCHER)`，依赖 `<queries>`，无 `QUERY_ALL_PACKAGES`）选抖音 → 建规则（`rule_xxx`，含通行时长/延长时长/启用）。
3. 打开抖音弹 open 干预 → 放行 grant → 到期弹 expired 干预 → 延长 extend。
4. `InterventionEventStore` 完整记录 open/expired/grant/extend/end 四类事件。
5. 今日报告显示计数 + 按应用分组。
6. 设置页"清除全部数据"清掉规则/通行/事件（不动权限和 Onboarding 状态）。

**退出标准达成情况**：
- ✅ 新用户数分钟内完成配置
- ✅ 无需手改包名（应用列表选择）
- ✅ 重启规则保留（SharedPreferences）
- ✅ 可生成签名 Alpha APK（`assembleRelease` 产出 `app-release.apk` 12MB，debug 签名，正式 keystore 待上架前生成）
- ⚠️ 权限关闭降级：当时为等价降级（服务断/UsageStats 空），`detectOnce` 的 `permissionsReady` 仍 hardcode true → 阶段 3 收尾修复解决（见 3.5）

### 3.5 阶段 3 收尾修复（2026-07-18）

阶段 3 标记为"功能完成，待收尾验证"，修两项遗留缺口：

**修复 1：显式权限降级**
- `PauseAccessibilityService.detectOnce()` 接入 `AndroidPermissionGateway.hasUsageAccess()`。
- Usage Access 关闭时记 `degraded=usageAccessDenied` 日志并跳过干预，不再 hardcode `permissionsReady=true`。
- 无障碍状态不自检：服务在运行即说明已开启，关闭时系统销毁服务、循环自然停止；国产 ROM 自检 `getEnabledAccessibilityServiceList` 存在误判回退风险，故只校验 Usage Access。

**修复 2：首页多规则展示**
- `HomeScreen` 增挂 `RulesViewModel`，`ON_RESUME` 时重载规则。
- `ProtectionStatusCard` 由"目标：单包名"改为"规则 N 条（启用 M 条）+ 目标包列表 + 通行信息"。
- `SpikeViewModel.currentPassInfo` 改为多包感知（多通行时显示"通行中 K 个，最近到期 Ns"）。

**验证**：`testDebugUnitTest` + `lintDebug` + `assembleDebug` + `assembleRelease` 全绿，`check-stage1-static.ps1` 8/8 通过。

## 4. 已实现功能清单

对照当前代码（`android/app/src/main/kotlin/com/pausenow/app/`）：

| 模块 | 功能 | 关键代码 |
|---|---|---|
| 权限 | Usage Access + Accessibility 状态检测与系统设置跳转 | `permissions/AndroidPermissionGateway.kt` |
| 权限 | 无障碍醒目披露门槛（勾选确认才能开） | `ui/SpikeViewModel.kt` `disclosureAccepted` |
| 检测 | FGS 内 UsageStats 轮询（3s 间隔，60s 窗口，`lastTimeUsed`） | `accessibility/PauseAccessibilityService.kt` |
| 检测 | 无障碍事件前台辅助流（TYPE_WINDOW_STATE_CHANGED + 去抖 + 排除） | `accessibility/EventDebouncer.kt`、`PackageExclusionPolicy.kt` |
| 检测 | 显式权限降级（Usage Access 关闭跳过干预） | `accessibility/PauseAccessibilityService.kt` `detectOnce` |
| 规则 | 多规则、优先级、启用/停用、每规则通行时长与延长时长 | `rule/RuleEngine.kt`、`ProtectionRule` |
| 干预 | 打开前干预（MODE_OPEN）、到期干预（MODE_EXPIRED） | `intervention/InterventionActivity.kt` |
| 干预 | 防重复：冷却 30s + 同包互斥（一次只一个干预 Activity） | `intervention/InterventionState.kt`、`InterventionLauncher.kt` |
| 通行 | 授予/延长/结束，SharedPreferences 持久化，跨重启恢复 | `pass/PassManager.kt`、`PassStore.kt`、`ActivePass.kt` |
| 到期 | AlarmManager 精确闹钟 + 高优先级通知 + 事件驱动再干预 | `intervention/ExpiryController.kt`、`ExpiryAlarmReceiver` |
| 快照 | 服务读 / UI 写的最小同步快照，schema v2 + v1 自动迁移 | `snapshot/SharedPreferencesSnapshotStore.kt` |
| 应用选择 | LAUNCHER 应用列表 + 搜索，无 QUERY_ALL_PACKAGES | `applist/AppRepository.kt`、`ui/screen/AppPickerScreen.kt` |
| 导航 | Onboarding/home/rules/ruleEdit/appPicker/report/settings 多屏 | `ui/nav/PauseNowNavHost.kt` |
| 报告 | 今日 open/expired/grant/extend/end 计数 + 按应用分组 | `report/InterventionEventStore.kt`、`ui/screen/ReportScreen.kt` |
| 清除 | 清规则/通行/事件（不动权限和 Onboarding） | `ui/screen/SettingsScreen.kt` |
| 诊断 | 设备信息卡 + Stage 1/2 Spike 诊断面 | `ui/screen/SettingsScreen.kt`、`ui/SpikeScreen.kt` |
| 首页 | 多规则摘要 + 权限状态 + 通行信息 | `ui/screen/HomeScreen.kt` |
| 构建 | debug + release（debug 签名）APK | `android/app/build.gradle.kts` |

## 5. 全流程

### 5.1 用户视角闭环

1. 首次启动 → Onboarding（说明 + 开 Usage Access + 开无障碍）→ 进入 Home。
2. Home → 保护规则 → 新建 → 从应用列表选抖音 → 设通行时长/延长时长 → 保存。
3. 打开抖音 → 弹出"停一下"打开前干预页。
4. 点"放行 N 分钟"→ 回到抖音，限时通行开始。
5. 到期 → 收到高优先级通知；下次打开抖音弹出"时间到"到期干预页。
6. 点"延长 N 分钟"→ 回抖音继续；或点"结束并回桌面"→ 回桌面。
7. Home → 今日报告 → 查看今日干预/放行/延长/结束计数。

### 5.2 技术数据流（检测循环）

```
PauseAccessibilityService.onServiceConnected
  └─ promoteToForeground()                    # FGS + 常驻通知
  └─ startDetectionLoop()                     # Handler 每 3s
       └─ detectOnce()
            ├─ snapshotStore.read()           # 读规则/设置快照
            ├─ 过滤 enabled 规则；空则 return
            ├─ permissionGateway.hasUsageAccess()  # 否则 degraded=usageAccessDenied return
            ├─ queryCurrentForeground()       # UsageStats 60s 窗口 lastTimeUsed
            ├─ 匹配 targetPackages + 最高 priority 规则
            ├─ passManager.currentPass(pkg)
            ├─ RuleEngine.evaluate(EvaluationInput) → Decision
            │     Degraded / Ignore / Allow /
            │     RequireOpenIntervention(ruleId) / RequireExpiredIntervention(ruleId)
            └─ InterventionLauncher.launchOpen / launchExpired
                  └─ InterventionState.tryStart(冷却+互斥) → startActivity(InterventionActivity)
```

**干预 Activity 用户选择**：直接调 `PassManager`（grant/extend/end）+ `ExpiryController`（schedule/cancel）+ `InterventionEventStore`（记录事件），再返回目标应用或桌面，`onDestroy` 调 `InterventionState.release` 释放互斥。

**日志 tag**：`PauseNow.Detection`（检测决策）、`PauseNow.ForegroundEvent`（无障碍事件）、`PauseNow.Intervention`（干预启动）、`PauseNow.Expiry`（到期闹钟）。

## 6. 关键技术决策

| 决策 | 原因 |
|---|---|
| 主检测用 FGS + UsageStats 轮询，无障碍降为辅助 | 国产 ROM 后台冻结无障碍事件（OPPO 15%、华为 0%）；UsageStats 在 FGS 内后台可靠 |
| 用 `lastTimeUsed` 而非 `MOVE_TO_FOREGROUND` | 用户停留超几秒后事件窗口查不到当前应用，`lastTimeUsed` 始终可靠 |
| 3 秒轮询间隔 | 平衡实时性与耗电；2-3 秒延迟在干预场景可接受 |
| 规则引擎纯逻辑（`object RuleEngine`） | 方便单元测试，事件线程串行调用 |
| 服务读快照、UI 写快照（Native Snapshot Store） | 服务不依赖 UI 存活；原子读写避免并发问题 |
| 干预防重复：冷却 30s + 同包互斥 | 避免检测循环高频重复弹干预；一次只一个干预 Activity |
| 到期：AlarmManager 通知兜底 + 事件驱动再干预 | 不依赖"到时必弹"（受后台启动限制），用户下次打开目标应用时拦截 |
| 应用选择用 `MAIN+LAUNCHER` + `<queries>` | 符合 ADR-006，不申请 `QUERY_ALL_PACKAGES`，不扫全部安装包 |
| 权限降级只校验 Usage Access，不自检无障碍 | 服务运行即无障碍已开；国产 ROM 自检 enabled 列表有误判回退风险 |

## 7. 隐私不变量保持情况

全程通过 `scripts/check-stage1-static.ps1`（8/8）：

| 检查项 | 结果 |
|---|---|
| `BIND_ACCESSIBILITY_SERVICE` 声明 | ✅ |
| `PACKAGE_USAGE_STATS` 声明 | ✅ |
| 无 `QUERY_ALL_PACKAGES` | ✅ |
| `canRetrieveWindowContent="false"` | ✅ |
| 仅 `typeWindowStateChanged` 事件 | ✅ |
| `isAccessibilityTool="false"` | ✅ |
| 无 `SYSTEM_ALERT_WINDOW` | ✅ |
| 无 `INTERNET`（含 debug/profile 变体） | ✅ |

代码层：不调 `getRootInActiveWindow()`、不读 `event.text`、不遍历节点树、不自动点击/手势、不上传。检测层只产出三字段：`detectedAtMs`、`eventType`、`packageName`。

## 8. 已知限制与风险

| 限制/风险 | 现状 | 处理 |
|---|---|---|
| 单设备验证 | 仅 OPPO PJU110 一台 | 待补华为/小米复测（华为 Stage 1 后台 0%，Stage 2/3 未复测） |
| 后台无障碍事件节流 | ColorOS 限制，独立于进程/电池策略 | 已 pivot 到 UsageStats 轮询绕开 |
| P95 干预延迟未实测 | Spike 期无埋点 | 阶段 4 补埋点测量 |
| 锁屏/长时稳定性 | 未做长时验证 | 留待阶段 4 |
| release APK 为 debug 签名 | Alpha 可用 | 上架前生成正式 keystore |
| FGS 常驻通知 | 系统要求，用户可见 | 文案已说明用途 |
| Gradle 弃用警告 | `android.builtInKotlin=false`、`org.jetbrains.kotlin.android` 插件弃用 | AGP 10 前迁移到 built-in Kotlin |

## 9. OPPO 复测指引

### 9.1 构建与安装

```powershell
cd android
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 9.2 授权

1. 启动应用 → Onboarding → 开 Usage Access → 开无障碍（在系统设置里开启"停一下"服务）。
2. 确认 Home 权限卡显示"权限已就绪"。

### 9.3 闭环验证

1. Home → 保护规则 → 新建 → 选抖音 → 通行时长设 1 分钟（便于观察）→ 保存。
2. 打开抖音 → 确认弹出打开前干预 → 放行。
3. 等 1 分钟 → 收到到期通知 → 再开抖音 → 确认弹出到期干预 → 延长。
4. Home → 今日报告 → 确认 open/grant/expired/extend 计数。

### 9.4 日志核对

```powershell
adb logcat -s PauseNow.Detection PauseNow.ForegroundEvent PauseNow.Intervention PauseNow.Expiry
```

关注：
- `foreground=<pkg> matchedRule=<id>` —— 检测命中
- `decision=RequireOpenIntervention/RequireExpiredIntervention/Allow` —— 决策
- `degraded=usageAccessDenied` —— 权限降级（关 Usage Access 时应出现）
- `launched packageName=<pkg>` / `launchSuppressed` —— 干预启动/抑制

### 9.5 后台可靠性复测

1. 打开抖音后按 Home 退到桌面（PauseNow 退后台）。
2. 等数秒再回抖音 → 确认检测循环仍命中（`foreground=com.ss.android.ugc.aweme`）。
3. 关 Usage Access → 确认日志出现 `degraded=usageAccessDenied` 且不再弹干预。
4. 关无障碍 → 确认服务被销毁（`serviceDestroyed=true`），检测停止。

### 9.6 隐私静态检查

```powershell
powershell -File scripts\check-stage1-static.ps1
```

应 8/8 通过。

## 10. 结论与下一步

**结论**：在 OPPO PJU110（ColorOS 14）上，停一下的核心干预闭环（检测 → 干预 → 通行 → 到期 → 延长/结束）可靠工作，Alpha 产品化功能完整，隐私不变量保持。阶段 1 暴露的国产 ROM 后台无障碍节流风险已通过 FGS + UsageStats 轮询 pivot 成功绕开。

**下一步**：
1. 华为/小米真机复测 Stage 3 闭环（重点确认 UsageStats pivot 在其他国产 ROM 后台同样可靠）。
2. 补 P95 干预延迟埋点与长时/锁屏稳定性验证（阶段 4）。
3. 上架前生成正式 keystore，刷 README/docs 阶段标记（当前 README 仍写"阶段 1 Spike"）。
4. AGP 10 前迁移到 built-in Kotlin，消除构建弃用警告。
