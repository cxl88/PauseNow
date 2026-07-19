package com.pausenow.app.intervention

import android.content.Context
import android.content.Intent
import android.util.Log
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore
import com.pausenow.app.report.InterventionTraceStore
import com.pausenow.app.report.LaunchResultType
import com.pausenow.app.report.ProductEventType

/**
 * 干预启动器（在检测循环中调用）。v3：传 traceId，launch 结果写 Trace（docs/09 §5.3）。
 * 首次 STARTED 记 visible 事件（阶段 3 后续可改为 onResume 记 visible）；SUPPRESSED/RECOVER/LAUNCH_FAILED 只进 Trace。
 */
class InterventionLauncher(private val context: Context) {
    private val eventStore = InterventionEventStore(context.applicationContext)
    private val traceStore = InterventionTraceStore(context.applicationContext)

    fun launchOpen(
        packageName: String,
        ruleId: String,
        passDurationSeconds: Int,
        extensionDurationSeconds: Int,
        cooldownMs: Long,
        traceId: String,
    ): Boolean = launch(packageName, ProductEventType.OPEN_INTERVENTION_VISIBLE, ruleId, cooldownMs, traceId) {
        InterventionActivity.newIntent(
            context = context,
            mode = InterventionActivity.MODE_OPEN,
            packageName = packageName,
            ruleId = ruleId,
            passDurationSeconds = passDurationSeconds,
            extensionDurationSeconds = extensionDurationSeconds,
            traceId = traceId,
        )
    }

    fun launchExpired(
        packageName: String,
        ruleId: String,
        extensionDurationSeconds: Int,
        cooldownMs: Long,
        traceId: String,
    ): Boolean = launch(packageName, ProductEventType.EXPIRED_INTERVENTION_VISIBLE, ruleId, cooldownMs, traceId) {
        InterventionActivity.newIntent(
            context = context,
            mode = InterventionActivity.MODE_EXPIRED,
            packageName = packageName,
            ruleId = ruleId,
            passDurationSeconds = 0,
            extensionDurationSeconds = extensionDurationSeconds,
            traceId = traceId,
        )
    }

    private inline fun launch(
        packageName: String,
        type: ProductEventType,
        ruleId: String,
        cooldownMs: Long,
        traceId: String,
        buildIntent: () -> Intent,
    ): Boolean {
        when (val result = InterventionState.tryStart(packageName, cooldownMs)) {
            InterventionState.LaunchResult.SUPPRESSED -> {
                traceStore.updateLaunch(traceId, System.currentTimeMillis(), LaunchResultType.SUPPRESSED)
                Log.i(TAG, "launchSuppressed packageName=$packageName (cooldown or in-flight showing)")
                return false
            }
            InterventionState.LaunchResult.STARTED, InterventionState.LaunchResult.RECOVER -> {
                val intent = buildIntent().apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                return try {
                    context.startActivity(intent)
                    val launchResult = if (result == InterventionState.LaunchResult.STARTED) {
                        LaunchResultType.STARTED
                    } else {
                        LaunchResultType.RECOVERED
                    }
                    traceStore.updateLaunch(traceId, System.currentTimeMillis(), launchResult)
                    if (result == InterventionState.LaunchResult.STARTED) record(type, packageName, ruleId)
                    Log.i(TAG, "launched packageName=$packageName recover=${result == InterventionState.LaunchResult.RECOVER}")
                    true
                } catch (e: Exception) {
                    InterventionState.release(packageName)
                    traceStore.updateLaunch(traceId, System.currentTimeMillis(), LaunchResultType.LAUNCH_FAILED)
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
