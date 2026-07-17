package com.pausenow.app.model

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONObject

/**
 * 设备与 App 版本信息，用于验收证据 JSON。对应原 Dart 侧 [DeviceSnapshot]。
 */
data class DeviceSnapshot(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val buildId: String,
    val appVersion: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("manufacturer", manufacturer)
        .put("model", model)
        .put("androidRelease", androidRelease)
        .put("sdkInt", sdkInt)
        .put("buildId", buildId)
        .put("appVersion", appVersion)

    companion object {
        val unavailable = DeviceSnapshot(
            manufacturer = "unknown",
            model = "unknown",
            androidRelease = "unknown",
            sdkInt = 0,
            buildId = "unknown",
            appVersion = "unknown",
        )

        fun from(context: Context): DeviceSnapshot = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            DeviceSnapshot(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidRelease = Build.VERSION.RELEASE,
                sdkInt = Build.VERSION.SDK_INT,
                buildId = Build.ID,
                appVersion = "${packageInfo.versionName ?: "unknown"} ($versionCode)",
            )
        } catch (_: PackageManager.NameNotFoundException) {
            unavailable
        }
    }
}
