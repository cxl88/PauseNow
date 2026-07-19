package com.pausenow.app.report

import android.content.Context
import com.pausenow.app.pass.PassPurpose
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong

/**
 * 干预事件持久化（docs/09 §7）。v3：ProductEventType 枚举 + 完整字段，最近 2000 条 / 30 天。
 * 旧 v2 字符串事件（open/expired/grant/extend/end）读取时映射为对应枚举，保留诊断、不进新版主动停下率口径（阶段 3 报告重写时区分）。
 */
class InterventionEventStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val idCounter = AtomicLong(System.currentTimeMillis())

    fun append(event: InterventionEvent) {
        synchronized(LOCK) {
            val withId = if (event.eventId.isEmpty()) {
                event.copy(eventId = "evt_${idCounter.incrementAndGet()}")
            } else {
                event
            }
            val current = readAll().toMutableList()
            current.add(0, withId)
            val cutoff = System.currentTimeMillis() - RETENTION_MS
            val trimmed = current.take(MAX_EVENTS).filter { it.occurredAtMs >= cutoff }
            write(trimmed)
        }
    }

    fun recentEvents(): List<InterventionEvent> = synchronized(LOCK) { readAll() }

    fun query(fromMs: Long, toMs: Long): List<InterventionEvent> =
        recentEvents().filter { it.occurredAtMs in fromMs..toMs }

    fun todayEvents(): List<InterventionEvent> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return recentEvents().filter { it.occurredAtMs >= startOfDay }
    }

    fun clear() {
        synchronized(LOCK) { prefs.edit().remove(KEY).commit() }
    }

    private fun readAll(): List<InterventionEvent> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(parseEvent(arr.getJSONObject(i)))
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun parseEvent(o: JSONObject): InterventionEvent {
        val typeStr = o.getString("type")
        val type = runCatching { ProductEventType.valueOf(typeStr) }.getOrNull()
            ?: migrateLegacyType(typeStr)
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

    private fun migrateLegacyType(s: String): ProductEventType? = when (s) {
        "open" -> ProductEventType.OPEN_INTERVENTION_VISIBLE
        "expired" -> ProductEventType.EXPIRED_INTERVENTION_VISIBLE
        "grant" -> ProductEventType.PASS_GRANTED
        "extend" -> ProductEventType.PASS_EXTENDED
        "end" -> ProductEventType.END_AT_EXPIRY
        else -> null
    }

    private fun write(events: List<InterventionEvent>) {
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
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object {
        const val PREFS = "pausenow_intervention_events"
        const val KEY = "events"
        const val MAX_EVENTS = 2000
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000 // 30 天
        val LOCK = Any()
    }
}
