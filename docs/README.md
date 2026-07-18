# PauseNow 项目文档

最后更新：2026-07-17

## 当前 Android 主线

- [产品需求文档](01_停一下_产品需求文档_PRD.md)
- [Kotlin 原生 开发环境与技术验证设计](02_停一下_Kotlin原生_开发环境与技术验证设计.md)
- [系统架构与项目推进方案](03_停一下_系统架构与项目推进方案.md)
- [阶段 1：权限与前台事件 Spike 验证](04_阶段1_权限与前台事件_Spike验证.md)
- [OPPO 真机技术验证报告](05_OPPO真机技术验证报告.md)
- [华为真机技术验证报告](06_华为真机技术验证报告.md)
- [阶段 1-3 技术总结与下阶段规划](07_停一下_技术总结与下阶段规划.md)
- [项目进度](project-progress.md)

当前技术路线为纯 Kotlin 原生（Kotlin + Jetpack Compose），单进程，无 Flutter、无跨进程桥接。

当前阶段门：代码和自动采证工具已交付；必须在 3 台安卓真机上完成授权、每台事件识别成功率不低于 95%，并完成耗电观察后，才能进入阶段 2“干预与通行 Spike”。

## 历史 iOS 探索

以下文档与根目录 `project.yml`、`Configuration/` 属于早期 iOS Screen Time 技术探索，不是当前 Windows 11 + Android 原生路线：

- [iOS 技术方案](technical-solution.md)
- [iOS 第 1-3 天真机验收](day-1-3-acceptance.md)
- [Family Controls 权限申请](family-controls-entitlement.md)
