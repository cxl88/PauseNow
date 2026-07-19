package com.pausenow.app.snapshot

import com.pausenow.app.rule.ProtectionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotStoreTest {

    private class FakeSnapshotStore : SnapshotStore {
        var stored: ProtectionSnapshot = ProtectionSnapshot.DEFAULT
        override fun read(): ProtectionSnapshot = stored
        override fun write(snapshot: ProtectionSnapshot) { stored = snapshot }
    }

    @Test
    fun `default snapshot has five minute pass and empty rules`() {
        val s = ProtectionSnapshot.DEFAULT
        assertEquals(300, s.settings.defaultPassDurationSeconds)
        assertEquals(180, s.settings.defaultExtensionDurationSeconds)
        assertEquals(1000L, s.settings.interventionCooldownMs)
        assertTrue(s.rules.isEmpty())
        assertTrue(s.protectedPackages.isEmpty())
    }

    @Test
    fun `snapshot json round trip preserves rules and settings`() {
        val original = ProtectionSnapshot(
            updatedAt = 12345L,
            rules = listOf(
                ProtectionRule(id = "r1", targetPackageName = "com.ss.android.ugc.aweme", cachedAppLabel = "抖音", passDurationSeconds = 600, extensionDurationSeconds = 60),
                ProtectionRule(id = "r2", targetPackageName = "com.xingin.xhs", passDurationSeconds = 300),
            ),
            settings = ProtectionSettings(defaultPassDurationSeconds = 600, defaultExtensionDurationSeconds = 60, interventionCooldownMs = 500L),
        )
        val restored = ProtectionSnapshot.fromJson(original.toJson())
        assertEquals(2, restored.rules.size)
        assertEquals("r1", restored.rules[0].id)
        assertEquals("抖音", restored.rules[0].cachedAppLabel)
        assertEquals("com.ss.android.ugc.aweme", restored.rules[0].targetPackageName)
        assertEquals(600, restored.rules[0].passDurationSeconds)
        assertEquals(60, restored.rules[0].extensionDurationSeconds)
        assertEquals(setOf("com.ss.android.ugc.aweme", "com.xingin.xhs"), restored.protectedPackages)
        assertEquals(500L, restored.settings.interventionCooldownMs)
        assertEquals(12345L, restored.updatedAt)
        assertEquals(3, restored.schemaVersion)
    }

    @Test
    fun `store write then read returns written snapshot`() {
        val store = FakeSnapshotStore()
        val snapshot = ProtectionSnapshot(
            rules = listOf(ProtectionRule(id = "r1", targetPackageName = "com.ss.android.ugc.aweme", passDurationSeconds = 60)),
            settings = ProtectionSettings.DEFAULT,
        )
        store.write(snapshot)
        assertEquals(snapshot, store.read())
    }

    @Test
    fun `fromJson with missing settings falls back to defaults`() {
        val minimal = org.json.JSONObject()
            .put("rules", org.json.JSONArray(listOf(
                org.json.JSONObject()
                    .put("id", "r1")
                    .put("targetPackageName", "com.example")
                    .put("passDurationSeconds", 60),
            )))
        val parsed = ProtectionSnapshot.fromJson(minimal)
        assertEquals(1, parsed.rules.size)
        assertEquals("com.example", parsed.rules[0].targetPackageName)
        assertEquals(ProtectionSettings.DEFAULT.defaultPassDurationSeconds, parsed.settings.defaultPassDurationSeconds)
    }

    @Test
    fun `v1 legacy protectedPackages migrates to v3 rules`() {
        val v1 = org.json.JSONObject()
            .put("schemaVersion", 1)
            .put("protectedPackages", org.json.JSONArray(listOf("com.ss.android.ugc.aweme")))
            .put("settings", ProtectionSettings.DEFAULT.toJson())
        val parsed = ProtectionSnapshot.fromJson(v1)
        assertEquals(1, parsed.rules.size)
        assertEquals("com.ss.android.ugc.aweme", parsed.rules[0].targetPackageName)
        assertEquals(3, parsed.schemaVersion)
        assertEquals(setOf("com.ss.android.ugc.aweme"), parsed.protectedPackages)
    }

    @Test
    fun `v2 duplicate targetPackages migrates to single v3 rule by main selection`() {
        // v2：同包两条规则，按 §8.1 选主（启用>priority>id 字典序）
        val v2 = org.json.JSONObject()
            .put("schemaVersion", 2)
            .put("rules", org.json.JSONArray(listOf(
                org.json.JSONObject()
                    .put("id", "a")
                    .put("targetPackages", org.json.JSONArray(listOf("com.ss.android.ugc.aweme")))
                    .put("passDurationMs", 60_000)
                    .put("extensionSeconds", 0)
                    .put("enabled", true)
                    .put("priority", 1),
                org.json.JSONObject()
                    .put("id", "b")
                    .put("targetPackages", org.json.JSONArray(listOf("com.ss.android.ugc.aweme")))
                    .put("passDurationMs", 300_000)
                    .put("extensionSeconds", 180)
                    .put("enabled", true)
                    .put("priority", 10),
            )))
            .put("settings", ProtectionSettings.DEFAULT.toJson())
        val parsed = ProtectionSnapshot.fromJson(v2)
        assertEquals(1, parsed.rules.size)
        assertEquals("b", parsed.rules[0].id) // priority 10 胜
        assertEquals(300, parsed.rules[0].passDurationSeconds)
        assertEquals(180, parsed.rules[0].extensionDurationSeconds)
        assertEquals(3, parsed.schemaVersion)
        assertTrue(parsed.migrationNotes.any { it.contains("合并") })
    }
}
