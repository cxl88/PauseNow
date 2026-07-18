package com.pausenow.app.pass

import org.json.JSONObject

/**
 * 一次"限时通行"。Spike 期只承载最必要字段：目标包、授予/到期时间、延长次数。
 * 持久化由 [PassManager] 负责（SharedPreferences JSON），跨重启可恢复。
 */
data class ActivePass(
    val ruleId: String,
    val packageName: String,
    val grantedAtMs: Long,
    val expiresAtMs: Long,
    val extensionCount: Int = 0,
) {
    fun isValid(now: Long): Boolean = now < expiresAtMs

    fun isExpired(now: Long): Boolean = now >= expiresAtMs

    fun toJson(): JSONObject = JSONObject()
        .put("ruleId", ruleId)
        .put("packageName", packageName)
        .put("grantedAtMs", grantedAtMs)
        .put("expiresAtMs", expiresAtMs)
        .put("extensionCount", extensionCount)

    companion object {
        fun fromJson(json: JSONObject): ActivePass = ActivePass(
            ruleId = json.getString("ruleId"),
            packageName = json.getString("packageName"),
            grantedAtMs = json.getLong("grantedAtMs"),
            expiresAtMs = json.getLong("expiresAtMs"),
            extensionCount = json.optInt("extensionCount", 0),
        )
    }
}
