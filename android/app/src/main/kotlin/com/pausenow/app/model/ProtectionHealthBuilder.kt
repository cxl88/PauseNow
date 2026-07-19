package com.pausenow.app.model

import com.pausenow.app.pass.ActivePass
import com.pausenow.app.rule.ProtectionRule

/** 统一保护健康度构建（docs/09 §5.6）。优先级：Usage Access > 无障碍 > 无启用规则 > 正常。 */
object ProtectionHealthBuilder {
    fun build(
        usageAccessGranted: Boolean,
        accessibilityEnabled: Boolean,
        rules: List<ProtectionRule>,
        activePasses: List<ActivePass>,
    ): ProtectionHealth {
        val blocking = mutableListOf<ProtectionIssue>()
        val warnings = mutableListOf<ProtectionIssue>()
        if (!usageAccessGranted) {
            blocking.add(ProtectionIssue(IssueLevel.BLOCKING, "使用情况访问未开启", "打开使用情况访问"))
        }
        if (!accessibilityEnabled) {
            blocking.add(ProtectionIssue(IssueLevel.BLOCKING, "无障碍服务未开启", "打开无障碍设置"))
        }
        val enabledRules = rules.filter { it.enabled }
        if (blocking.isEmpty() && enabledRules.isEmpty()) {
            warnings.add(ProtectionIssue(IssueLevel.WARNING, "还没有保护规则", "添加保护应用"))
        }
        return ProtectionHealth(
            blockingIssues = blocking,
            warnings = warnings,
            enabledRuleCount = enabledRules.size,
            activePasses = activePasses,
        )
    }
}
