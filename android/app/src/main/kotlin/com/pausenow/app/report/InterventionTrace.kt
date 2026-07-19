package com.pausenow.app.report

/**
 * 干预追踪（docs/09 §5.3）。用于计算可靠性与延迟，不作为用户行为结果。
 * 延迟口径：detectionToVisibleMs = visibleAtMs - detectedAtMs；launchToVisibleMs = visibleAtMs - launchRequestedAtMs。
 * 只有 visibleAtMs != null 才算用户看到了干预。
 */
enum class InterventionMode { OPEN, EXPIRED }
enum class LaunchResultType { STARTED, RECOVERED, SUPPRESSED, LAUNCH_FAILED }
enum class ActionResultType { GRANTED, EXTENDED, ENDED, EXITED_BEFORE_OPEN }

data class InterventionTrace(
    val traceId: String,
    val ruleId: String? = null,
    val sessionId: String? = null,
    val packageName: String,
    val mode: InterventionMode,
    val detectedAtMs: Long,
    val decisionAtMs: Long? = null,
    val launchRequestedAtMs: Long? = null,
    val visibleAtMs: Long? = null,
    val actionAtMs: Long? = null,
    val launchResult: LaunchResultType? = null,
    val actionResult: ActionResultType? = null,
)
