package com.pausenow.app.report

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** InterventionTrace 序列化纯逻辑（docs/09 §5.3 / §7），便于单测。 */
object TraceSerializer {
    fun serialize(traces: List<InterventionTrace>): String {
        val arr = JSONArray()
        traces.forEach { t ->
            val o = JSONObject()
                .put("traceId", t.traceId)
                .put("packageName", t.packageName)
                .put("mode", t.mode.name)
                .put("detectedAtMs", t.detectedAtMs)
            t.ruleId?.let { o.put("ruleId", it) }
            t.sessionId?.let { o.put("sessionId", it) }
            t.decisionAtMs?.let { o.put("decisionAtMs", it) }
            t.launchRequestedAtMs?.let { o.put("launchRequestedAtMs", it) }
            t.visibleAtMs?.let { o.put("visibleAtMs", it) }
            t.actionAtMs?.let { o.put("actionAtMs", it) }
            t.launchResult?.let { o.put("launchResult", it.name) }
            t.actionResult?.let { o.put("actionResult", it.name) }
            arr.put(o)
        }
        return arr.toString()
    }

    fun parse(json: String): List<InterventionTrace> = try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                add(parseTrace(arr.getJSONObject(i)))
            }
        }
    } catch (_: JSONException) {
        emptyList()
    }

    private fun parseTrace(o: JSONObject): InterventionTrace = InterventionTrace(
        traceId = o.getString("traceId"),
        ruleId = o.optString("ruleId", "").ifBlank { null },
        sessionId = o.optString("sessionId", "").ifBlank { null },
        packageName = o.getString("packageName"),
        mode = runCatching { InterventionMode.valueOf(o.optString("mode", "OPEN")) }.getOrDefault(InterventionMode.OPEN),
        detectedAtMs = o.getLong("detectedAtMs"),
        decisionAtMs = o.optLong("decisionAtMs", 0L).takeIf { it > 0 },
        launchRequestedAtMs = o.optLong("launchRequestedAtMs", 0L).takeIf { it > 0 },
        visibleAtMs = o.optLong("visibleAtMs", 0L).takeIf { it > 0 },
        actionAtMs = o.optLong("actionAtMs", 0L).takeIf { it > 0 },
        launchResult = o.optString("launchResult", "").ifBlank { null }?.let {
            runCatching { LaunchResultType.valueOf(it) }.getOrNull()
        },
        actionResult = o.optString("actionResult", "").ifBlank { null }?.let {
            runCatching { ActionResultType.valueOf(it) }.getOrNull()
        },
    )
}
