package com.pausenow.app.intervention

import android.content.Context
import android.content.Intent
import android.util.Log
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore

/**
 * 干预启动器（在检测循环中调用）。负责防重复 + 启动 [InterventionActivity]。
 * 用户在 Activity 的选择（放行/延长/结束）由 Activity 直接调 PassManager 并返回桌面/目标，
 * Activity onDestroy 调 [InterventionState.release] 释放互斥。
 */
class InterventionLauncher(private val context: Context) {
    private val eventStore = InterventionEventStore(context.applicationContext)

    fun launchOpen(
        packageName: String,
        ruleId: String,
        passDurationMs: Long,
        cooldownMs: Long,
    ): Boolean = launch(packageName, "open", cooldownMs) {
        InterventionActivity.newIntent(
            context = context,
            mode = InterventionActivity.MODE_OPEN,
            packageName = packageName,
            ruleId = ruleId,
            passDurationMs = passDurationMs,
            extensionSeconds = 0,
        )
    }

    fun launchExpired(
        packageName: String,
        ruleId: String,
        extensionSeconds: Int,
        cooldownMs: Long,
    ): Boolean = launch(packageName, "expired", cooldownMs) {
        InterventionActivity.newIntent(
            context = context,
            mode = InterventionActivity.MODE_EXPIRED,
            packageName = packageName,
            ruleId = ruleId,
            passDurationMs = 0L,
            extensionSeconds = extensionSeconds,
        )
    }

    private inline fun launch(
        packageName: String,
        type: String,
        cooldownMs: Long,
        buildIntent: () -> Intent,
    ): Boolean {
        when (val result = InterventionState.tryStart(packageName, cooldownMs)) {
            InterventionState.LaunchResult.SUPPRESSED -> {
                Log.i(TAG, "launchSuppressed packageName=$packageName (cooldown or in-flight showing)")
                return false
            }
            InterventionState.LaunchResult.STARTED, InterventionState.LaunchResult.RECOVER -> {
                val intent = buildIntent().apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                return try {
                    context.startActivity(intent)
                    if (result == InterventionState.LaunchResult.STARTED) record(type, packageName)
                    Log.i(TAG, "launched packageName=$packageName recover=${result == InterventionState.LaunchResult.RECOVER}")
                    true
                } catch (e: Exception) {
                    InterventionState.release(packageName)
                    Log.e(TAG, "launchFailed packageName=$packageName error=${e.message}")
                    false
                }
            }
        }
    }

    private fun record(type: String, packageName: String) {
        eventStore.append(InterventionEvent(type, packageName, System.currentTimeMillis()))
    }

    private companion object {
        const val TAG = "PauseNow.Intervention"
    }
}
