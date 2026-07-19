package com.pausenow.app.report

import com.pausenow.app.pass.PassPurpose

/**
 * 面向报告的产品事件（docs/09 §5.4）。v3：ProductEventType 枚举 + traceId/sessionId/ruleId/cachedAppLabel/purpose/durationSeconds。
 * 技术事件（SUPPRESSED/RECOVERED/LAUNCH_FAILED）只进 Trace，不进本事件流。
 */
data class InterventionEvent(
    val eventId: String,
    val traceId: String? = null,
    val sessionId: String? = null,
    val ruleId: String? = null,
    val packageName: String,
    val cachedAppLabel: String = "",
    val type: ProductEventType,
    val occurredAtMs: Long,
    val purpose: PassPurpose? = null,
    val durationSeconds: Int? = null,
)
