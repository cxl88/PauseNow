package com.pausenow.app.model

import org.json.JSONObject

/**
 * 两项关键授权的当前状态快照。对应原 Dart 侧 [PermissionSnapshot]。
 */
data class PermissionSnapshot(
    val usageAccessGranted: Boolean,
    val accessibilityEnabled: Boolean,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("usageAccessGranted", usageAccessGranted)
        .put("accessibilityEnabled", accessibilityEnabled)

    companion object {
        val unavailable = PermissionSnapshot(
            usageAccessGranted = false,
            accessibilityEnabled = false,
        )

        fun from(map: Map<String, Boolean>): PermissionSnapshot = PermissionSnapshot(
            usageAccessGranted = map["usageAccessGranted"] == true,
            accessibilityEnabled = map["accessibilityEnabled"] == true,
        )
    }
}
