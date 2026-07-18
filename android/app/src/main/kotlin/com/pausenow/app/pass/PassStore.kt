package com.pausenow.app.pass

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

/**
 * 通行持久化抽象。单测可注入内存实现；生产用 [SharedPreferencesPassStore]。
 */
interface PassStore {
    fun load(): Map<String, ActivePass>
    fun upsert(pass: ActivePass)
    fun remove(packageName: String)
}

/**
 * SharedPreferences JSON 持久化，与 ForegroundEventStore 同模式。
 * 结构：{ "packageName": {ActivePass json}, ... }。synchronized 保证一致。
 */
class SharedPreferencesPassStore(context: Context) : PassStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(): Map<String, ActivePass> = synchronized(LOCK) {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        try {
            val obj = JSONObject(raw)
            buildMap {
                for (key in obj.keys()) {
                    put(key, ActivePass.fromJson(obj.getJSONObject(key)))
                }
            }
        } catch (_: JSONException) {
            emptyMap()
        }
    }

    override fun upsert(pass: ActivePass) = synchronized(LOCK) {
        val current = load().toMutableMap()
        current[pass.packageName] = pass
        write(current)
    }

    override fun remove(packageName: String) = synchronized(LOCK) {
        val current = load().toMutableMap()
        current.remove(packageName)
        write(current)
    }

    private fun write(map: Map<String, ActivePass>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v.toJson()) }
        prefs.edit().putString(KEY, obj.toString()).apply()
    }

    private companion object {
        const val PREFS = "pausenow_passes"
        const val KEY = "passes"
        val LOCK = Any()
    }
}
