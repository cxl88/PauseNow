package com.pausenow.app.report

import com.pausenow.app.pass.PassPurpose
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** InterventionEvent 序列化纯逻辑（docs/09 §7），便于单测。 */
object EventSerializer {
    fun serialize(events: List<InterventionEvent>): String {
        val arr = JSONArray()
        events.forEach { e ->
            val o = JSONObject()
                .put("eventId", e.eventId)
                .put("packageName", e.packageName)
                .put("type", e.type.name)
                .put("occurredAtMs", e.occurredAtMs)
                .put("cachedAppLabel", e.cachedAppLabel)
            e.traceId?.let { o.put("traceId", it) }
            e.sessionId?.let { o.put("sessionId", it) }
            e.ruleId?.let { o.put("ruleId", it) }
            e.purpose?.let { o.put("purpose", it.name) }
            e.durationSeconds?.let { o.put("durationSeconds", it) }
            arr.put(o)
        }
        return arr.toString()
    }

    fun parse(json: String): List<InterventionEvent> = try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                add(parseEvent(arr.getJSONObject(i)))
            }
        }
    } catch (_: JSONException) {
        emptyList()
    }

    private fun parseEvent(o: JSONObject): InterventionEvent {
        val typeStr = o.getString("type")
        val type = runCatching { ProductEventType.valueOf(typeStr) }.getOrNull() ?: migrateLegacyType(typeStr)
        val occurredAt = o.optLong("occurredAtMs", o.optLong("timestamp", 0L))
        return InterventionEvent(
            eventId = o.optString("eventId", "").ifBlank { "evt_$occurredAt" },
            traceId = o.optString("traceId", "").ifBlank { null },
            sessionId = o.optString("sessionId", "").ifBlank { null },
            ruleId = o.optString("ruleId", "").ifBlank { null },
            packageName = o.getString("packageName"),
            cachedAppLabel = o.optString("cachedAppLabel", ""),
            type = type ?: ProductEventType.PASS_GRANTED,
            occurredAtMs = occurredAt,
            purpose = o.optString("purpose", "").ifBlank { null }?.let {
                runCatching { PassPurpose.valueOf(it) }.getOrNull()
            },
            durationSeconds = if (o.has("durationSeconds")) o.getInt("durationSeconds") else null,
        )
    }

    /** docs/09 §8.3：旧 v2 字符串事件迁移为对应枚举。 */
    private fun migrateLegacyType(s: String): ProductEventType? = when (s) {
        "open" -> ProductEventType.OPEN_INTERVENTION_VISIBLE
        "expired" -> ProductEventType.EXPIRED_INTERVENTION_VISIBLE
        "grant" -> ProductEventType.PASS_GRANTED
        "extend" -> ProductEventType.PASS_EXTENDED
        "end" -> ProductEventType.END_AT_EXPIRY
        else -> null
    }
}
