package com.pausenow.app.report

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 干预追踪持久化（docs/09 §5.3 / §7）。环形 300 条，按 traceId 增量更新。
 * 用于可靠性与延迟统计，不作为用户行为结果（用户结果进 InterventionEvent）。
 */
class InterventionTraceStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun appendOrUpdate(trace: InterventionTrace) = synchronized(LOCK) {
        val current = readAll().toMutableList()
        val idx = current.indexOfFirst { it.traceId == trace.traceId }
        if (idx >= 0) current[idx] = merge(current[idx], trace) else current.add(0, trace)
        write(current.take(MAX_TRACES))
    }

    fun updateLaunch(traceId: String, launchRequestedAtMs: Long, result: LaunchResultType) = synchronized(LOCK) {
        updateById(traceId) { it.copy(launchRequestedAtMs = launchRequestedAtMs, launchResult = result) }
    }

    fun updateVisible(traceId: String, visibleAtMs: Long) = synchronized(LOCK) {
        updateById(traceId) { it.copy(visibleAtMs = visibleAtMs) }
    }

    fun updateAction(traceId: String, actionAtMs: Long, result: ActionResultType) = synchronized(LOCK) {
        updateById(traceId) { it.copy(actionAtMs = actionAtMs, actionResult = result) }
    }

    fun latest(limit: Int): List<InterventionTrace> = synchronized(LOCK) { readAll().take(limit) }

    fun clear() = synchronized(LOCK) { prefs.edit().remove(KEY).commit() }

    private fun updateById(traceId: String, transform: (InterventionTrace) -> InterventionTrace) {
        val current = readAll().toMutableList()
        val idx = current.indexOfFirst { it.traceId == traceId }
        if (idx >= 0) {
            current[idx] = transform(current[idx])
            write(current)
        }
    }

    private fun merge(old: InterventionTrace, new: InterventionTrace): InterventionTrace = new.copy(
        detectedAtMs = if (new.detectedAtMs > 0) new.detectedAtMs else old.detectedAtMs,
        decisionAtMs = new.decisionAtMs ?: old.decisionAtMs,
        launchRequestedAtMs = new.launchRequestedAtMs ?: old.launchRequestedAtMs,
        visibleAtMs = new.visibleAtMs ?: old.visibleAtMs,
        actionAtMs = new.actionAtMs ?: old.actionAtMs,
        launchResult = new.launchResult ?: old.launchResult,
        actionResult = new.actionResult ?: old.actionResult,
    )

    private fun readAll(): List<InterventionTrace> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return TraceSerializer.parse(raw)
    }

    private fun write(traces: List<InterventionTrace>) {
        prefs.edit().putString(KEY, TraceSerializer.serialize(traces)).apply()
    }

    private companion object {
        const val PREFS = "pausenow_intervention_trace"
        const val KEY = "traces"
        const val MAX_TRACES = 300
        val LOCK = Any()
    }
}
