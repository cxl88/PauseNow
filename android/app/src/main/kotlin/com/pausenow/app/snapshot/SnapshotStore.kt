package com.pausenow.app.snapshot

import android.content.Context
import com.pausenow.app.rule.ProtectionRule
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** 保护设置。默认值：5 分钟通行、3 分钟延长、1 秒干预冷却（新规则的默认值）。 */
data class ProtectionSettings(
    val passDurationMs: Long = 5 * 60 * 1000L,
    val extensionSeconds: Int = 180,
    val interventionCooldownMs: Long = 1000L,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("passDurationMs", passDurationMs)
        .put("extensionSeconds", extensionSeconds)
        .put("interventionCooldownMs", interventionCooldownMs)

    companion object {
        val DEFAULT = ProtectionSettings()
        fun fromJson(json: JSONObject): ProtectionSettings = ProtectionSettings(
            passDurationMs = json.optLong("passDurationMs", DEFAULT.passDurationMs),
            extensionSeconds = json.optInt("extensionSeconds", DEFAULT.extensionSeconds),
            interventionCooldownMs = json.optLong("interventionCooldownMs", DEFAULT.interventionCooldownMs),
        )
    }
}

/**
 * 原生快照（docs/03 §4.10）。阶段 3：多规则列表（替代阶段 2 的 flat protectedPackages）。
 * AccessibilityService 只读它，UI 写它。schema v2；读取时自动迁移 v1 的 protectedPackages。
 */
data class ProtectionSnapshot(
    val schemaVersion: Int = 2,
    val updatedAt: Long = 0L,
    val rules: List<ProtectionRule> = emptyList(),
    val settings: ProtectionSettings = ProtectionSettings.DEFAULT,
) {
    val protectedPackages: Set<String> get() = rules.flatMap { it.targetPackages }.toSet()

    fun toJson(): JSONObject = JSONObject()
        .put("schemaVersion", schemaVersion)
        .put("updatedAt", updatedAt)
        .put("rules", JSONArray(rules.map { it.toJson() }))
        .put("settings", settings.toJson())

    companion object {
        val DEFAULT = ProtectionSnapshot()

        fun fromJson(json: JSONObject): ProtectionSnapshot {
            val legacyPackages = json.optJSONArray("protectedPackages")?.let { arr ->
                buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }
            }
            val rules: List<ProtectionRule> = json.optJSONArray("rules")?.let { arr ->
                buildList { for (i in 0 until arr.length()) add(ProtectionRule.fromJson(arr.getJSONObject(i))) }
            } ?: emptyList()
            val settings = json.optJSONObject("settings")?.let { ProtectionSettings.fromJson(it) } ?: ProtectionSettings.DEFAULT
            // v1 -> v2 迁移：旧 protectedPackages 非空且无 rules，转成一条默认规则。
            val migratedRules = if (rules.isEmpty() && !legacyPackages.isNullOrEmpty()) {
                listOf(
                    ProtectionRule(
                        id = "migrated",
                        name = "已迁移规则",
                        targetPackages = legacyPackages,
                        passDurationMs = settings.passDurationMs,
                        extensionSeconds = settings.extensionSeconds,
                    ),
                )
            } else {
                rules
            }
            return ProtectionSnapshot(
                schemaVersion = 2,
                updatedAt = json.optLong("updatedAt", 0L),
                rules = migratedRules,
                settings = settings,
            )
        }
    }
}

interface SnapshotStore {
    fun read(): ProtectionSnapshot
    fun write(snapshot: ProtectionSnapshot)
}

/** SharedPreferences JSON 持久化，原子读写。 */
class SharedPreferencesSnapshotStore(context: Context) : SnapshotStore {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun read(): ProtectionSnapshot = synchronized(LOCK) {
        val raw = prefs.getString(KEY, null) ?: return ProtectionSnapshot.DEFAULT
        try {
            ProtectionSnapshot.fromJson(JSONObject(raw))
        } catch (_: JSONException) {
            ProtectionSnapshot.DEFAULT
        }
    }

    override fun write(snapshot: ProtectionSnapshot) = synchronized(LOCK) {
        prefs.edit().putString(KEY, snapshot.toJson().toString()).apply()
    }

    private companion object {
        const val PREFS = "pausenow_snapshot"
        const val KEY = "protection_snapshot"
        val LOCK = Any()
    }
}
