package com.pausenow.app.pass

import org.json.JSONObject

/**
 * 一次限时通行（docs/09 §5.2）。v3：sessionId 唯一键、purpose、status、时长从规则复制（R-008）。
 * R-006：extensionCount 只能 0 或 1，init 校验为 Domain 层不变量。
 */
data class ActivePass(
    val sessionId: String,
    val ruleId: String,
    val packageName: String,
    val purpose: PassPurpose,
    val plannedDurationSeconds: Int,
    val extensionDurationSeconds: Int,
    val grantedAtMs: Long,
    val expiresAtMs: Long,
    val extensionCount: Int = 0,
    val status: PassStatus = PassStatus.ACTIVE,
) {
    init {
        require(extensionCount in 0..1) { "extensionCount must be 0 or 1, was $extensionCount" }
    }

    fun isValid(now: Long): Boolean = now < expiresAtMs

    fun isExpired(now: Long): Boolean = now >= expiresAtMs

    /** 规则关闭延长时，延长时长为 0；它和“已经延长过”都不能再获得通行。 */
    fun canExtend(): Boolean = extensionDurationSeconds > 0 && extensionCount < 1

    fun toJson(): JSONObject = JSONObject()
        .put("sessionId", sessionId)
        .put("ruleId", ruleId)
        .put("packageName", packageName)
        .put("purpose", purpose.name)
        .put("plannedDurationSeconds", plannedDurationSeconds)
        .put("extensionDurationSeconds", extensionDurationSeconds)
        .put("grantedAtMs", grantedAtMs)
        .put("expiresAtMs", expiresAtMs)
        .put("extensionCount", extensionCount)
        .put("status", status.name)

    companion object {
        fun fromJson(json: JSONObject): ActivePass {
            val grantedAt = json.getLong("grantedAtMs")
            val expiresAt = json.getLong("expiresAtMs")
            // 迁移：旧 v2 无 sessionId 用 packageName；无 plannedDurationSeconds 从到期-授予推算
            val sessionId = json.optString("sessionId", "").ifBlank { json.getString("packageName") }
            val planned = json.optInt(
                "plannedDurationSeconds",
                ((expiresAt - grantedAt) / 1000L).toInt().coerceAtLeast(0),
            )
            return ActivePass(
                sessionId = sessionId,
                ruleId = json.getString("ruleId"),
                packageName = json.getString("packageName"),
                purpose = runCatching {
                    PassPurpose.valueOf(json.optString("purpose", "UNSPECIFIED_LEGACY"))
                }.getOrDefault(PassPurpose.UNSPECIFIED_LEGACY),
                plannedDurationSeconds = planned,
                extensionDurationSeconds = json.optInt("extensionDurationSeconds", 0),
                grantedAtMs = grantedAt,
                expiresAtMs = expiresAt,
                extensionCount = json.optInt("extensionCount", 0).coerceIn(0, 1), // R-006 Store 层：拒绝 >1
                status = runCatching {
                    PassStatus.valueOf(json.optString("status", "ACTIVE"))
                }.getOrDefault(PassStatus.ACTIVE),
            )
        }
    }
}
