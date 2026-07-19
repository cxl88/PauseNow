package com.pausenow.app.rule

import com.pausenow.app.pass.ActivePass
import org.json.JSONObject

/**
 * 保护规则（docs/09 §5.1）。v3：单值 targetPackageName、秒为单位、cachedAppLabel、时间戳、schemaVersion=3。
 * 删除 v2 的 name/priority（仅迁移用）。R-001 同包名唯一。
 */
data class ProtectionRule(
    val id: String,
    val targetPackageName: String,
    val cachedAppLabel: String = "",
    val passDurationSeconds: Int,
    val extensionDurationSeconds: Int = 0,
    val enabled: Boolean = true,
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L,
    val schemaVersion: Int = 3,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("targetPackageName", targetPackageName)
        .put("cachedAppLabel", cachedAppLabel)
        .put("passDurationSeconds", passDurationSeconds)
        .put("extensionDurationSeconds", extensionDurationSeconds)
        .put("enabled", enabled)
        .put("createdAtMs", createdAtMs)
        .put("updatedAtMs", updatedAtMs)
        .put("schemaVersion", schemaVersion)

    companion object {
        fun fromJson(json: JSONObject): ProtectionRule = ProtectionRule(
            id = json.getString("id"),
            targetPackageName = json.getString("targetPackageName"),
            cachedAppLabel = json.optString("cachedAppLabel", ""),
            passDurationSeconds = json.getInt("passDurationSeconds"),
            extensionDurationSeconds = json.optInt("extensionDurationSeconds", 0),
            enabled = json.optBoolean("enabled", true),
            createdAtMs = json.optLong("createdAtMs", 0L),
            updatedAtMs = json.optLong("updatedAtMs", 0L),
            schemaVersion = 3,
        )
    }
}

/** Rule Engine 输入（docs/03 §6.1）。 */
data class EvaluationInput(
    val permissionsReady: Boolean,
    val packageName: String,
    val excludedPackages: Set<String>,
    val rules: List<ProtectionRule>,
    val activePass: ActivePass?,
    val now: Long,
)

/** Rule Engine 决策输出（docs/03 §6.2）。 */
sealed interface Decision {
    data object Degraded : Decision
    data object Ignore : Decision
    data object Allow : Decision
    data class RequireOpenIntervention(val ruleId: String) : Decision
    data class RequireExpiredIntervention(val ruleId: String) : Decision
}

/**
 * 纯逻辑规则引擎（docs/03 §6.2）。v3：targetPackageName 单值匹配，包名唯一后无优先级冲突。
 * 优先级：权限失效 -> Degraded；排除包 -> Ignore；非目标 -> Allow；
 * 有效通行 -> Allow；通行到期 -> RequireExpiredIntervention；默认 -> RequireOpenIntervention。
 */
object RuleEngine {
    fun evaluate(input: EvaluationInput): Decision {
        if (!input.permissionsReady) return Decision.Degraded
        if (input.packageName in input.excludedPackages) return Decision.Ignore

        val rule = input.rules
            .filter { it.enabled }
            .firstOrNull { it.targetPackageName == input.packageName }
            ?: return Decision.Allow

        val pass = input.activePass
        if (pass != null && pass.packageName == input.packageName) {
            if (pass.isValid(input.now)) return Decision.Allow
            return Decision.RequireExpiredIntervention(rule.id)
        }
        return Decision.RequireOpenIntervention(rule.id)
    }
}
