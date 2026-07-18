package com.pausenow.app.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

    /**
     * 华为/鸿蒙需要用户在"启动管理"里手动放行后台运行，否则退后台 20-30 秒进程被杀
     * （连前台服务都保不住）。检测到 HUAWEI 厂商时在 Onboarding 引导用户去开。
     */
    fun isHuawei(): Boolean =
        Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true) ||
            Build.BRAND.equals("HUAWEI", ignoreCase = true) ||
            Build.BRAND.equals("HONOR", ignoreCase = true)

    fun openStartupManager() {
        // 直跳华为启动管理列表页（component 方式，从 app 内通常可跳）。
        val direct = Intent().setClassName(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(direct)
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }
        // 回退：打开电池设置，由用户手动找"启动管理"。
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * 打开停一下的应用详情页，由用户进入"权限"开启"后台弹出界面"
     * （华为默认禁止后台弹界面，不开则干预页弹不到前台、被目标应用盖住）。
     */
    fun openAppDetails() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
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

