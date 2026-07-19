package com.pausenow.app.intervention

import android.content.Context
import android.content.Intent
import android.util.Log
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore
import com.pausenow.app.report.ProductEventType

/**
 * 干预启动器（在检测循环中调用）。v3：传秒、record ProductEventType。
 * 启动请求/被盖/冷却只记 Trace（阶段 3），首次 STARTED 记 visible 事件（阶段 3 改为 onResume 记）。
 */
class InterventionLauncher(private val context: Context) {
    private val eventStore = InterventionEventStore(context.applicationContext)

    fun launchOpen(
        packageName: String,
        ruleId: String,
        passDurationSeconds: Int,
        extensionDurationSeconds: Int,
        cooldownMs: Long,
    ): Boolean = launch(packageName, ProductEventType.OPEN_INTERVENTION_VISIBLE, ruleId, cooldownMs) {
        InterventionActivity.newIntent(
            context = context,
            mode = InterventionActivity.MODE_OPEN,
            packageName = packageName,
            ruleId = ruleId,
            passDurationSeconds = passDurationSeconds,
            extensionDurationSeconds = extensionDurationSeconds,
        )
    }

    fun launchExpired(
        packageName: String,
        ruleId: String,
        extensionDurationSeconds: Int,
        cooldownMs: Long,
    ): Boolean = launch(packageName, ProductEventType.EXPIRED_INTERVENTION_VISIBLE, ruleId, cooldownMs) {
        InterventionActivity.newIntent(
            context = context,
            mode = InterventionActivity.MODE_EXPIRED,
            packageName = packageName,
            ruleId = ruleId,
            passDurationSeconds = 0,
            extensionDurationSeconds = extensionDurationSeconds,
        )
    }

    private inline fun launch(
        packageName: String,
        type: ProductEventType,
        ruleId: String,
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
                    if (result == InterventionState.LaunchResult.STARTED) record(type, packageName, ruleId)
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

    private fun record(type: ProductEventType, packageName: String, ruleId: String) {
        eventStore.append(
            InterventionEvent(
                eventId = "",
                ruleId = ruleId,
                packageName = packageName,
                type = type,
                occurredAtMs = System.currentTimeMillis(),
            ),
        )
    }

    private companion object {
        const val TAG = "PauseNow.Intervention"
    }
}
