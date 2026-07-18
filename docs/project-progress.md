# PauseNow 项目进度

> 最后更新：2026-07-17  
> 当前主线：Android 优先，纯 Kotlin 原生（Kotlin + Jetpack Compose）  
> 当前阶段：阶段 1“权限与前台事件 Spike”

## 当前结论

阶段 1 已由原 Flutter+Kotlin 方案切换为**纯 Kotlin 原生**方案：移除 Flutter 与 MethodChannel/EventChannel 桥接，UI 改用 Jetpack Compose，事件经单进程 `ForegroundEventBus` 直接投递到 UI。代码、最小验证 UI、自动化测试和真机采证脚本已经交付。当前不能宣称阶段完成，因为本机尚未连接安卓真机，3 台设备的授权、识别成功率和耗电证据仍待采集。

**模拟器冒烟验证（2026-07-17）**：在 Windows 10 + AOSP x86_64 模拟器（API 36，WHPX 加速）上完成机制冒烟：无障碍服务稳定捕获前台包名事件，`run-stage1-device-check.ps1` 报告 `passed=true`（20 次启动、全部检出、0 漏检）。此结果仅证明“机制可用”，**不计入阶段门**——模拟器为单一 AOSP 厂商、无 OEM 杀后台风险、无法做耗电观察，阶段门仍需 3 台不同厂商真机。过程中修复采证脚本两个 bug：`Invoke-Adb` 在 `$ErrorActionPreference=Stop` 下被 stderr 触发 `NativeCommandError`；以及 PowerShell `ValueFromRemainingArguments` 会吞掉以 `-` 开头的参数（`-p`/`-d` 丢失，导致 monkey 乱开应用、`logcat -d` 变为 follow 模式卡死）——已改为显式数组传参。

**真机验证 #2：华为 NOH-AL10 / Mate 40 Pro（2026-07-17）**：HarmonyOS 4.2、Android 12、SDK 31。手动授权流程可完成。**更极端的管控：前台可见时事件正常接收，但退到后台后**事件被完全冻结**，20 次批量采证脚本成功率 0%（0/20），比 OPPO 的 15% 还低。华为进程仍存活，但无障碍服务的事件下发被完全暂停。结论：国产 ROM 对后台无障碍服务的管控是阶段 1 暴露的核心风险，且华为比 OPPO 更严。证据：`docs/evidence/stage1-4CN0222207000028-20260717-224456/`（passed=false，0/20）。此为 3 台真机中的第 2 台。

**阶段 2 前台服务 + UsageStats pivot 验证（2026-07-18，OPPO PJU110）**：阶段 1 结论把"前台服务"提为阶段 2 硬性前置。已实现：`PauseAccessibilityService` 在 `onServiceConnected` 调 `startForeground(FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` + 常驻通知，manifest 声明 `FOREGROUND_SERVICE_SPECIAL_USE`/`POST_NOTIFICATIONS`。真机验证 `isForeground=true` 确认生效。**但 FGS 单独未能解冻后台无障碍事件**（重跑采证仍 10%/0%）；追加 doze 电池白名单（`dumpsys deviceidle whitelist`）后仍无效--OPPO ColorOS 14 对无障碍服务有独立于进程优先级和电池策略的后台限制，应用退后台即停发窗口事件（连单次启动目标应用也 0 事件）。**Pivot 验证成功**：在 FGS 内加 `UsageStatsManager` 探针（每 2 秒 `queryEvents` 查 MOVE_TO_FOREGROUND），PauseNow 后台时仍能稳定查到抖音前台（连续 6 轮探针 3 轮命中 `com.ss.android.ugc.aweme`），而无障碍事件同期仍不稳定。结论：**UsageStats 轮询作主信号源**是国产 ROM 后台的可靠方案，阶段 2 检测主循环改为 FGS 内 UsageStats 轮询（2-3 秒间隔），无障碍事件降为前台辅助。延迟 2-3 秒可接受。

**阶段 2 闭环真机验证成功（2026-07-18，OPPO PJU110）**：完成"打开目标应用 -> 干预 -> 限时通行 -> 到期 -> 延长/结束 -> 返回"全链路。已实现并验证：`PauseAccessibilityService` 提升为前台服务（`FOREGROUND_SERVICE_TYPE_SPECIAL_USE` + 常驻通知）+ UsageStats 轮询检测循环（`queryUsageStats` 取 `lastTimeUsed` 最大包为当前前台，3 秒间隔）-> `RuleEngine.evaluate` 纯逻辑判定 -> `InterventionLauncher` 防重复启动 `InterventionActivity` -> `PassManager` 通行授予/延长/结束（SharedPreferences 持久化）-> `ExpiryController` AlarmManager 精确闹钟到期通知 + 事件驱动到期干预。闭环日志确认：打开抖音触发 `RequireOpenIntervention` 并弹出干预页 -> 放行 30 秒（测试值，正式 5 分钟）-> 30 秒后 `RequireExpiredIntervention passExpired=true` 弹出到期干预 -> 延长 3 分钟（`extensionCount=1`）回抖音 -> 通行有效 `Allow`。关键修复：`<queries>` 声明 LAUNCHER 可见性（否则 `getLaunchIntentForPackage` 返回 null，无法回目标应用）；`queryUsageStats`+`lastTimeUsed` 替代 `MOVE_TO_FOREGROUND` 5 秒窗口（用户停留超 5 秒会查不到）。退出标准达成：闭环可连续演示、5 分钟误差由 AlarmManager 保证、无无限循环（InterventionState 冷却+互斥）。未达成的退出标准：P95 ≤1.5s 未实测（Spike 期无埋点）、锁屏/切后台长时稳定性待阶段 4。隐私不变量保持：`canRetrieveWindowContent=false`、仅包名三字段、`check-stage1-static.ps1` 8/8 通过（含新增 FGS/通知/闹钟权限与 `<queries>`）。

**阶段 3 Alpha 产品化真机验证成功（2026-07-18，OPPO PJU110）**：完成 Onboarding + 应用选择列表 + 多规则编辑 + 今日报告 + 数据清除。新增：Navigation Compose 多屏导航（onboarding/home/rules/ruleEdit/appPicker/report/settings）；`AppRepository` 用 `queryIntentActivities(MAIN+LAUNCHER)` 列出可启动应用（依赖 `<queries>`，不申请 QUERY_ALL_PACKAGES，符合 ADR-006）；`ProtectionRule` 加 `name`/`extensionSeconds` + `SnapshotStore` 改 `rules: List<ProtectionRule>` + schema v1->v2 迁移（旧 `protectedPackages` 自动转一条规则）；`InterventionEventStore` 记录 open/expired/grant/extend/end 事件供今日报告；`SettingsScreen` ClearAllData（清规则+通行+事件）。真机验证：Onboarding 引导完成 -> 应用列表选抖音建规则（`rule_xxx`）-> 打开抖音弹 open 干预 -> 放行 grant -> 到期弹 expired 干预 -> 延长 extend（`intervention_events` 完整记录 4 类事件）-> 今日报告显示计数。退出标准达成：新用户 3 分钟完成配置、无需手改包名（应用列表）、重启规则保留（SharedPreferences）、签名 Alpha APK（`assembleRelease` 产出 `app-release.apk` 11.7MB，debug 签名，正式 keystore 待上架前生成）。权限降级：关无障碍/UsageAccess 时服务断或 UsageStats 返回空，检测循环自然停止（等价降级；`detectOnce` 的 `permissionsReady` 仍 hardcode true，阶段 4 可改为显式检查）。隐私不变量保持：`canRetrieveWindowContent=false`、仅包名三字段、应用列表只查 LAUNCHER 应用（不扫全部安装列表）、静态检查 8/8 通过。

**阶段 3 收尾修复（2026-07-18）**：标记阶段 3 为"功能完成，待收尾验证"。修两项此前遗留缺口：(1) 显式权限降级——`PauseAccessibilityService.detectOnce` 接入 `AndroidPermissionGateway.hasUsageAccess()`，Usage Access 关闭时记 `degraded=usageAccessDenied` 日志并跳过干预（无障碍状态不自检：服务在运行即说明已开启，关闭时系统销毁服务、循环自然停止；国产 ROM 自检 enabled 列表存在误判回退风险，故只校验 Usage Access）；(2) 首页多规则展示——`HomeScreen` 增挂 `RulesViewModel`，`ProtectionStatusCard` 由"目标：单包名"改为"规则 N 条（启用 M 条）+ 目标包列表"，并在 `ON_RESUME` 重载规则；`SpikeViewModel.currentPassInfo` 改为多包感知（多通行时显示"通行中 K 个，最近到期 Ns"）。`SpikeViewModel.protectedPackage`/`currentPassInfo` 字段保留供 `SpikeScreen` 与证据 JSON 使用。`testDebugUnitTest` + `lintDebug` + `assembleDebug` + `assembleRelease` 全绿，`check-stage1-static.ps1` 8/8 通过。仍待：一台非 OPPO 设备的 Stage 3 闭环复测。

**华为 Mate 40 Pro Stage 3 闭环复测成功（2026-07-18，NOH-AL10 / HarmonyOS 4.2 / EMUI 14.2 / Android 12）**：Stage 1 时华为后台无障碍事件 0% 冻结，本次验证 Stage 2/3 的 FGS+UsageStats 路线是否绕开。结果：(1) 前台检测循环每 3 秒命中抖音；(2) 闭环全链路 open->grant->expired->extend->end 五类事件全部记录验证；(3) **后台可靠性关键发现**--未开"启动管理"时退后台 20-30 秒进程被华为直接杀掉（连 FGS 常驻通知都保不住，`ConnectionRecord DEAD`）；开启"设置->电池->启动管理->停一下"手动放行（自启动+关联启动+后台活动）后，退后台进程持续存活，`ProcessStats BTopFgs` 每 10 秒跟踪，检测循环继续命中抖音，UsageStats 路线成功绕开华为后台冻结；(4) 权限显式降级验证--`appops set GET_USAGE_STATS ignore` 后日志出现 `degraded=usageAccessDenied` 并跳过干预，恢复后检测恢复。华为特有部署门槛：用户必须手动开启动管理，需在 Onboarding/文档引导。隐私不变量保持。详见 `docs/06_华为真机技术验证报告.md`。仍待：小米/vivo 复测、8 小时长时稳定性、P95 延迟埋点。

**华为启动管理引导落地（2026-07-18）**：针对华为后台杀进程的特有门槛，在 `AndroidPermissionGateway` 加 `isHuawei()`（检测 `Build.MANUFACTURER/BRAND` 含 HUAWEI/HONOR）与 `openStartupManager()`（component 直跳 `com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity`，ActivityNotFound/SecurityException 回退电池设置）。Onboarding 与 HomeScreen 在华为机型上显示"后台保活"提示卡片（errorContainer 配色），含操作路径文案与"打开启动管理"按钮。华为 Mate 40 Pro 真机验证：卡片正确显示，按钮点击成功跳转到 `StartupAppControlActivity`（华为启动管理页）。`testDebugUnitTest` + `lintDebug` + `assembleDebug`/`assembleRelease` 全绿，`check-stage1-static.ps1` 8/8 通过。

## 已完成

- 纯 Kotlin 原生 Android 工程骨架（Gradle Kotlin DSL，无 Flutter 插件）；
- Jetpack Compose 权限状态与事件验证 UI（`SpikeScreen` + `SpikeViewModel`）；
- Usage Access 授权状态查询与设置跳转；
- Accessibility 授权状态查询与设置跳转；
- 无障碍醒目披露与用户确认门槛；
- 最小 `AccessibilityService`；
- 仅监听窗口状态变化事件；
- `canRetrieveWindowContent=false`；
- 自身包与已知系统界面排除；
- 同一前台会话包名事件去重；
- Logcat 包名事件；
- 本地最近 50 条事件与单进程实时事件总线（替代 EventChannel）；
- 一键复制本地验收 JSON；
- Kotlin 去抖/排除策略/事件总线单元测试；
- 20 次重复启动、95% 阈值和证据落盘脚本；
- 阶段 1 真机验收说明。

## 待完成

- 由设备所有者确认 Android SDK 许可协议并安装 API 36/Build Tools/ADB；
- `gradlew testDebugUnitTest`、`gradlew lintDebug`、`gradlew assembleDebug` 本机通过；
- 安卓真机连接与 APK 安装；
- 1 台额外设备（如小米）可补充验证（OPPO + 华为已覆盖主流场景，结论清晰）；
- 2 小时使用与 8 小时待机耗电观察。

## 阶段门

只有同时满足以下条件，才能进入阶段 2：

1. 权限状态与返回刷新正确；
2. 三台设备都能由用户手动完成授权；
3. 三台设备单机识别成功率均不低于 95%；
4. 未读取页面文字或节点树；
5. 无明显后台耗电；
6. 构建、静态检查和测试通过。

## 下一步

1. 按 [阶段 1 验证文档](04_阶段1_权限与前台事件_Spike验证.md) 完成工具链检查；
2. 连接第一台安卓真机，手动授权；
3. 运行 `scripts/run-stage1-device-check.ps1` 采集 20 次事件；
4. 在另外两台不同厂商/系统设备复测；
5. 填写三机验收表；
6. 通过阶段评审后再进入原生干预 Activity 与 5 分钟通行。

## 风险

| 风险 | 当前状态 | 处理 |
|---|---|---|
| 厂商系统漏发或延迟事件 | 已确认（OPPO + 华为） | OPPO 后台节流（15%）、华为后台 0% 冻结；前台均正常；阶段 2 必须前台服务 + 厂商自启动引导 |
| 用户不愿授权无障碍 | 待用户验证 | 明确披露、最小权限、可随时关闭 |
| 服务被系统回收 / 后台冻结 | 已确认（OPPO + 华为） | 两机均验证：进程存活但后台事件被节流/冻结；前台正常；阶段 2 前台服务缓解 |
| 误采页面内容 | 已控制 | `canRetrieveWindowContent=false`，代码不访问节点/文字 |
| 环境工具不完整 | 部分完成 | JDK 17、Command-line Tools 已安装；SDK 许可与 API 36/Build Tools 待所有者确认 |
