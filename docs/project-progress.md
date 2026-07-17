# PauseNow 项目进度

> 最后更新：2026-07-17  
> 当前主线：Android 优先，纯 Kotlin 原生（Kotlin + Jetpack Compose）  
> 当前阶段：阶段 1“权限与前台事件 Spike”

## 当前结论

阶段 1 已由原 Flutter+Kotlin 方案切换为**纯 Kotlin 原生**方案：移除 Flutter 与 MethodChannel/EventChannel 桥接，UI 改用 Jetpack Compose，事件经单进程 `ForegroundEventBus` 直接投递到 UI。代码、最小验证 UI、自动化测试和真机采证脚本已经交付。当前不能宣称阶段完成，因为本机尚未连接安卓真机，3 台设备的授权、识别成功率和耗电证据仍待采集。

**模拟器冒烟验证（2026-07-17）**：在 Windows 10 + AOSP x86_64 模拟器（API 36，WHPX 加速）上完成机制冒烟：无障碍服务稳定捕获前台包名事件，`run-stage1-device-check.ps1` 报告 `passed=true`（20 次启动、全部检出、0 漏检）。此结果仅证明“机制可用”，**不计入阶段门**——模拟器为单一 AOSP 厂商、无 OEM 杀后台风险、无法做耗电观察，阶段门仍需 3 台不同厂商真机。过程中修复采证脚本两个 bug：`Invoke-Adb` 在 `$ErrorActionPreference=Stop` 下被 stderr 触发 `NativeCommandError`；以及 PowerShell `ValueFromRemainingArguments` 会吞掉以 `-` 开头的参数（`-p`/`-d` 丢失，导致 monkey 乱开应用、`logcat -d` 变为 follow 模式卡死）——已改为显式数组传参。

**真机验证 #2：华为 NOH-AL10 / Mate 40 Pro（2026-07-17）**：HarmonyOS 4.2、Android 12、SDK 31。手动授权流程可完成。**更极端的管控：前台可见时事件正常接收，但退到后台后**事件被完全冻结**，20 次批量采证脚本成功率 0%（0/20），比 OPPO 的 15% 还低。华为进程仍存活，但无障碍服务的事件下发被完全暂停。结论：国产 ROM 对后台无障碍服务的管控是阶段 1 暴露的核心风险，且华为比 OPPO 更严。证据：`docs/evidence/stage1-4CN0222207000028-20260717-224456/`（passed=false，0/20）。此为 3 台真机中的第 2 台。

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
