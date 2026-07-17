package com.pausenow.app.accessibility

import org.json.JSONObject

data class ForegroundPackageEventRecord(
    val packageName: String,
    val eventType: String,
    val detectedAtMs: Long,
) {
    fun toChannelMap(): Map<String, Any> = mapOf(
        "packageName" to packageName,
        "eventType" to eventType,
        "detectedAtMs" to detectedAtMs,
    )

    fun toJson(): JSONObject = JSONObject()
        .put("packageName", packageName)
        .put("eventType", eventType)
        .put("detectedAtMs", detectedAtMs)

    companion object {
        fun fromJson(json: JSONObject): ForegroundPackageEventRecord =
            ForegroundPackageEventRecord(
                packageName = json.getString("packageName"),
                eventType = json.getString("eventType"),
                detectedAtMs = json.getLong("detectedAtMs"),
            )
    }
}
