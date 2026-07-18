package com.pausenow.app.report

/** 干预事件记录（用于今日报告）。type: open/expired/grant/extend/end。 */
data class InterventionEvent(
    val type: String,
    val packageName: String,
    val timestamp: Long,
)
