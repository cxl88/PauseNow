package com.pausenow.app.rule

import com.pausenow.app.pass.ActivePass
import org.json.JSONArray
import org.json.JSONObject

/**
 * 保护规则。阶段 3：用户可读名 + 每规则延长时长。
 * schedule / 每日限额仍属后续阶段。
 */
data class ProtectionRule(
    val id: String,
    val name: String = "",
    val targetPackages: Set<String>,
    val passDurationMs: Long,
    val extensionSeconds: Int = 180,
    val enabled: Boolean = true,
    val priority: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("targetPackages", JSONArray(targetPackages.toList()))
        .put("passDurationMs", passDurationMs)
        .put("extensionSeconds", extensionSeconds)
        .put("enabled", enabled)
        .put("priority", priority)

    companion object {
        fun fromJson(json: JSONObject): ProtectionRule = ProtectionRule(
            id = json.getString("id"),
            name = json.optString("name", ""),
            targetPackages = json.optJSONArray("targetPackages")?.let { arr ->
                buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }
            } ?: emptySet(),
            passDurationMs = json.getLong("passDurationMs"),
            extensionSeconds = json.optInt("extensionSeconds", 180),
            enabled = json.optBoolean("enabled", true),
            priority = json.optInt("priority", 0),
        )
    }
}

/**
 * Rule Engine 的输入。对应 docs/03 §6.1 优先级判定所需字段。
 */
data class EvaluationInput(
    val permissionsReady: Boolean,
    val packageName: String,
    val excludedPackages: Set<String>,
    val rules: List<ProtectionRule>,
    val activePass: ActivePass?,
    val now: Long,
)

/**
 * Rule Engine 的决策输出（按 docs/03 §6.2 伪代码，不产 END_AND_GO_HOME--
 * 返回桌面由 InterventionActivity 负责）。
 */
sealed interface Decision {
    data object Degraded : Decision
    data object Ignore : Decision
    data object Allow : Decision
    data class RequireOpenIntervention(val ruleId: String) : Decision
    data class RequireExpiredIntervention(val ruleId: String) : Decision
}

/**
 * 纯逻辑规则引擎，方便单元测试。事件线程串行调用。
 * 优先级：权限失效 -> Degraded；排除包 -> Ignore；非目标 -> Allow；
 * 有效通行 -> Allow；通行到期 -> RequireExpiredIntervention；默认 -> RequireOpenIntervention。
 */
object RuleEngine {
    fun evaluate(input: EvaluationInput): Decision {
        if (!input.permissionsReady) return Decision.Degraded
        if (input.packageName in input.excludedPackages) return Decision.Ignore

        val rule = input.rules
            .filter { it.enabled }
            .filter { input.packageName in it.targetPackages }
            .maxByOrNull { it.priority }
            ?: return Decision.Allow

        val pass = input.activePass
        if (pass != null && pass.packageName == input.packageName) {
            if (pass.isValid(input.now)) return Decision.Allow
            return Decision.RequireExpiredIntervention(rule.id)
        }
        return Decision.RequireOpenIntervention(rule.id)
    }
}
