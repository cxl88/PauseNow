package com.pausenow.app

import android.os.Build
import com.pausenow.app.accessibility.ForegroundEventStore
import com.pausenow.app.bridge.NativeEventStream
import com.pausenow.app.permissions.AndroidPermissionGateway
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val permissionGateway = AndroidPermissionGateway(this)
        val eventStore = ForegroundEventStore(this)

        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            METHOD_CHANNEL,
        ).setMethodCallHandler { call, result ->
            when (call.method) {
                "getPermissionSnapshot" -> result.success(permissionGateway.permissionSnapshot())
                "getDeviceSnapshot" -> result.success(deviceSnapshot())
                "openUsageAccessSettings" -> {
                    permissionGateway.openUsageAccessSettings()
                    result.success(null)
                }
                "openAccessibilitySettings" -> {
                    permissionGateway.openAccessibilitySettings()
                    result.success(null)
                }
                "getRecentEvents" -> result.success(eventStore.recentEvents().map { it.toChannelMap() })
                "clearRecentEvents" -> {
                    eventStore.clear()
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }

        EventChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            EVENT_CHANNEL,
        ).setStreamHandler(NativeEventStream)
    }

    @Suppress("DEPRECATION")
    private fun deviceSnapshot(): Map<String, Any> {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        return mapOf(
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "androidRelease" to Build.VERSION.RELEASE,
            "sdkInt" to Build.VERSION.SDK_INT,
            "buildId" to Build.ID,
            "appVersion" to "${packageInfo.versionName ?: "unknown"} ($versionCode)",
        )
    }

    private companion object {
        const val METHOD_CHANNEL = "pausenow/native_control"
        const val EVENT_CHANNEL = "pausenow/foreground_events"
    }
}
