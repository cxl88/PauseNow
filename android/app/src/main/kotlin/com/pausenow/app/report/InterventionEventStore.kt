package com.pausenow.app.report

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar

/** 干预事件持久化（SharedPreferences JSON，最近 500 条）。 */
class InterventionEventStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun append(event: InterventionEvent) {
        synchronized(LOCK) {
            val current = readAll().toMutableList()
            current.add(0, event)
            val arr = org.json.JSONArray()
            current.take(MAX_EVENTS).forEach {
                arr.put(JSONObject().put("type", it.type).put("packageName", it.packageName).put("timestamp", it.timestamp))
            }
            prefs.edit().putString(KEY, arr.toString()).apply()
        }
    }

    fun recentEvents(): List<InterventionEvent> = synchronized(LOCK) { readAll() }

    fun todayEvents(): List<InterventionEvent> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return recentEvents().filter { it.timestamp >= startOfDay }
    }

    fun clear() {
        synchronized(LOCK) { prefs.edit().remove(KEY).commit() }
    }

    private fun readAll(): List<InterventionEvent> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(InterventionEvent(o.getString("type"), o.getString("packageName"), o.getLong("timestamp")))
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private companion object {
        const val PREFS = "pausenow_intervention_events"
        const val KEY = "events"
        const val MAX_EVENTS = 500
        val LOCK = Any()
    }
}
