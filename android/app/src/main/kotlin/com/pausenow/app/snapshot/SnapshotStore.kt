package com.pausenow.app.snapshot

import android.content.Context
import com.pausenow.app.rule.ProtectionRule
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** 保护设置（docs/09 §5）。v3：秒为单位。新规则默认值来源。 */
data class ProtectionSettings(
    val defaultPassDurationSeconds: Int = 300, // 5 分钟
    val defaultExtensionDurationSeconds: Int = 180, // 3 分钟
    val interventionCooldownMs: Long = 1000L,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("defaultPassDurationSeconds", defaultPassDurationSeconds)
        .put("defaultExtensionDurationSeconds", defaultExtensionDurationSeconds)
        .put("interventionCooldownMs", interventionCooldownMs)

    companion object {
        val DEFAULT = ProtectionSettings()

        fun fromJson(json: JSONObject): ProtectionSettings {
            // 迁移：旧 v2 settings 用 passDurationMs(Long)/extensionSeconds(Int)
            val passSec = if (json.has("defaultPassDurationSeconds")) {
                json.getInt("defaultPassDurationSeconds")
            } else {
                (json.optLong("passDurationMs", DEFAULT.defaultPassDurationSeconds * 1000L) / 1000L).toInt()
            }
            val extSec = if (json.has("defaultExtensionDurationSeconds")) {
                json.getInt("defaultExtensionDurationSeconds")
            } else {
                json.optInt("extensionSeconds", DEFAULT.defaultExtensionDurationSeconds)
            }
            return ProtectionSettings(
                defaultPassDurationSeconds = passSec,
                defaultExtensionDurationSeconds = extSec,
                interventionCooldownMs = json.optLong("interventionCooldownMs", DEFAULT.interventionCooldownMs),
            )
        }
    }
}

/**
 * 原生快照（docs/03 §4.10 / docs/09 §5）。v3：多规则列表，单值 targetPackageName，schemaVersion=3。
 * AccessibilityService 只读它，UI 写它。读取时自动迁移 v1/v2。
 */
data class ProtectionSnapshot(
    val schemaVersion: Int = 3,
    val updatedAt: Long = 0L,
    val rules: List<ProtectionRule> = emptyList(),
    val settings: ProtectionSettings = ProtectionSettings.DEFAULT,
    val migrationNotes: List<String> = emptyList(),
) {
    val protectedPackages: Set<String> get() = rules.map { it.targetPackageName }.filter { it.isNotEmpty() }.toSet()

    fun toJson(): JSONObject = JSONObject()
        .put("schemaVersion", schemaVersion)
        .put("updatedAt", updatedAt)
        .put("rules", JSONArray(rules.map { it.toJson() }))
        .put("settings", settings.toJson())

    companion object {
        val DEFAULT = ProtectionSnapshot()

        fun fromJson(json: JSONObject): ProtectionSnapshot {
            val notes = mutableListOf<String>()
            val legacyPackages = json.optJSONArray("protectedPackages")?.let { arr ->
                buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }
            }
            val rawRules = json.optJSONArray("rules")
            val v3Rules = mutableListOf<ProtectionRule>()
            val v2Rules = mutableListOf<V2Rule>()
            if (rawRules != null) {
                for (i in 0 until rawRules.length()) {
                    val r = rawRules.getJSONObject(i)
                    if (r.has("targetPackageName")) {
                        v3Rules.add(ProtectionRule.fromJson(r))
                    } else {
                        v2Rules.add(parseV2Rule(r))
                    }
                }
            }
            val settings = json.optJSONObject("settings")?.let { ProtectionSettings.fromJson(it) } ?: ProtectionSettings.DEFAULT

            val finalRules: List<ProtectionRule> = when {
                v3Rules.isNotEmpty() -> v3Rules
                v2Rules.isNotEmpty() -> migrateV2Rules(v2Rules, notes)
                !legacyPackages.isNullOrEmpty() -> legacyPackages.map { pkg ->
                    ProtectionRule(
                        id = "migrated_$pkg",
                        targetPackageName = pkg,
                        passDurationSeconds = settings.defaultPassDurationSeconds,
                        extensionDurationSeconds = settings.defaultExtensionDurationSeconds,
                    )
                }
                else -> emptyList()
            }

            return ProtectionSnapshot(
                schemaVersion = 3,
                updatedAt = json.optLong("updatedAt", 0L),
                rules = finalRules,
                settings = settings,
                migrationNotes = notes,
            )
        }

        private fun parseV2Rule(json: JSONObject): V2Rule {
            val targetPackages = json.optJSONArray("targetPackages")?.let { arr ->
                buildSet { for (i in 0 until arr.length()) add(arr.getString(i)) }
            } ?: emptySet()
            return V2Rule(
                id = json.getString("id"),
                name = json.optString("name", ""),
                targetPackages = targetPackages,
                passDurationMs = json.optLong("passDurationMs", 300_000L),
                extensionSeconds = json.optInt("extensionSeconds", 180),
                enabled = json.optBoolean("enabled", true),
                priority = json.optInt("priority", 0),
            )
        }

        /** docs/09 §8.1：同包名选主规则（启用>priority>id 字典序），重复归档。 */
        private fun migrateV2Rules(v2: List<V2Rule>, notes: MutableList<String>): List<ProtectionRule> {
            data class Expanded(val pkg: String, val rule: V2Rule)
            val expanded = v2.flatMap { rule -> rule.targetPackages.map { pkg -> Expanded(pkg, rule) } }
            return expanded
                .groupBy { it.pkg }
                .map { (pkg, list) ->
                    if (list.size > 1) {
                        notes.add("已合并 $pkg 的 ${list.size} 条重复规则，请确认新的保护时长。")
                    }
                    val main = list.sortedWith(
                        compareByDescending<Expanded> { it.rule.enabled }
                            .thenByDescending { it.rule.priority }
                            .thenBy { it.rule.id },
                    ).first()
                    ProtectionRule(
                        id = main.rule.id,
                        targetPackageName = pkg,
                        cachedAppLabel = main.rule.name,
                        passDurationSeconds = (main.rule.passDurationMs / 1000L).toInt(),
                        extensionDurationSeconds = main.rule.extensionSeconds,
                        enabled = main.rule.enabled,
                    )
                }
                .sortedBy { it.targetPackageName }
        }

        private data class V2Rule(
            val id: String,
            val name: String,
            val targetPackages: Set<String>,
            val passDurationMs: Long,
            val extensionSeconds: Int,
            val enabled: Boolean,
            val priority: Int,
        )
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
