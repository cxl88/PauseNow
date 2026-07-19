package com.pausenow.app.report

import android.content.Context
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
        return EventSerializer.parse(raw)
    }

    private fun write(events: List<InterventionEvent>) {
        prefs.edit().putString(KEY, EventSerializer.serialize(events)).apply()
    }

    private companion object {
        const val PREFS = "pausenow_intervention_events"
        const val KEY = "events"
        const val MAX_EVENTS = 2000
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000 // 30 天
        val LOCK = Any()
    }
}
