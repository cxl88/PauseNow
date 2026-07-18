package com.pausenow.app.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.pausenow.app.accessibility.PauseAccessibilityService

class AndroidPermissionGateway(private val context: Context) {
    fun permissionSnapshot(): Map<String, Boolean> = mapOf(
        "usageAccessGranted" to hasUsageAccess(),
        "accessibilityEnabled" to isAccessibilityServiceEnabled(),
    )

    fun openUsageAccessSettings() {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openAccessibilitySettings() {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    @Suppress("DEPRECATION")
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val expectedName = PauseAccessibilityService::class.java.name
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { serviceInfo ->
                val info = serviceInfo.resolveInfo.serviceInfo
                info.packageName == context.packageName && info.name == expectedName
            }
    }
}
