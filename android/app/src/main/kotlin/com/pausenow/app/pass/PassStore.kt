package com.pausenow.app.pass

import android.content.Context
import org.json.JSONException
import org.json.JSONObject

/**
 * 通行持久化抽象（docs/09 §9）。v3：key=sessionId。单测可注入内存实现；生产用 [SharedPreferencesPassStore]。
 */
interface PassStore {
    fun load(): Map<String, ActivePass> // key=sessionId
    fun upsert(pass: ActivePass)
    fun remove(sessionId: String)
    fun getByPackage(packageName: String): ActivePass? = load().values.firstOrNull { it.packageName == packageName }
}

/**
 * SharedPreferences JSON 持久化。结构：{ "sessionId": {ActivePass json}, ... }。
 * 迁移：旧 v2 key=packageName，ActivePass.fromJson 的 sessionId 默认=packageName，load 后按 sessionId 重新索引，兼容旧数据。
 */
class SharedPreferencesPassStore(context: Context) : PassStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(): Map<String, ActivePass> = synchronized(LOCK) {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        try {
            val obj = JSONObject(raw)
            buildMap {
                for (key in obj.keys()) {
                    val pass = ActivePass.fromJson(obj.getJSONObject(key))
                    put(pass.sessionId, pass) // 旧数据 sessionId=packageName，新 key 自动对齐
                }
            }
        } catch (_: JSONException) {
            emptyMap()
        }
    }

    override fun upsert(pass: ActivePass) = synchronized(LOCK) {
        val current = load().toMutableMap()
        // 迁移清理：同 packageName 的旧 key（packageName=sessionId）若与新 sessionId 不同则移除
        current.entries.removeIf { it.value.packageName == pass.packageName && it.key != pass.sessionId }
        current[pass.sessionId] = pass
        write(current)
    }

    override fun remove(sessionId: String) = synchronized(LOCK) {
        val current = load().toMutableMap()
        current.remove(sessionId)
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
