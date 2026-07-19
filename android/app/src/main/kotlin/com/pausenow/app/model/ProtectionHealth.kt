package com.pausenow.app.model

import com.pausenow.app.pass.ActivePass

enum class IssueLevel { BLOCKING, WARNING }

data class ProtectionIssue(val level: IssueLevel, val title: String, val action: String? = null)

/**
 * 统一保护健康度（docs/09 §5.6）。
 * 优先级：Usage Access 关闭 > 无障碍未运行 > 无启用规则 > 厂商步骤未确认 > 通知关闭 > 正常保护或通行中。
 */
data class ProtectionHealth(
    val blockingIssues: List<ProtectionIssue>,
    val warnings: List<ProtectionIssue>,
    val enabledRuleCount: Int,
    val activePasses: List<ActivePass>,
) {
    val isBlocking: Boolean get() = blockingIssues.isNotEmpty()
}
