package com.pausenow.app.accessibility

import android.content.Context
import org.json.JSONArray
import org.json.JSONException

class ForegroundEventStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun append(event: ForegroundPackageEventRecord) {
        synchronized(LOCK) {
            val current = readRecentEvents().toMutableList()
            current.add(0, event)
            val array = JSONArray()
            current.take(MAX_EVENTS).forEach { array.put(it.toJson()) }
            preferences.edit().putString(EVENTS_KEY, array.toString()).apply()
        }
    }

    fun recentEvents(): List<ForegroundPackageEventRecord> = synchronized(LOCK) {
        readRecentEvents()
    }

    private fun readRecentEvents(): List<ForegroundPackageEventRecord> {
        val raw = preferences.getString(EVENTS_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    add(ForegroundPackageEventRecord.fromJson(array.getJSONObject(index)))
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun clear() {
        synchronized(LOCK) {
            preferences.edit().remove(EVENTS_KEY).commit()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "pausenow_spike_events"
        const val EVENTS_KEY = "foreground_events"
        const val MAX_EVENTS = 50
        val LOCK = Any()
    }
}
