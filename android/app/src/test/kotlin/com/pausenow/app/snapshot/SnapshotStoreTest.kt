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
        assertEquals(5 * 60 * 1000L, s.settings.passDurationMs)
        assertEquals(180, s.settings.extensionSeconds)
        assertEquals(1000L, s.settings.interventionCooldownMs)
        assertTrue(s.rules.isEmpty())
        assertTrue(s.protectedPackages.isEmpty())
    }

    @Test
    fun `snapshot json round trip preserves rules and settings`() {
        val original = ProtectionSnapshot(
            updatedAt = 12345L,
            rules = listOf(
                ProtectionRule(id = "r1", name = "抖音", targetPackages = setOf("com.ss.android.ugc.aweme"), passDurationMs = 30_000L, extensionSeconds = 60),
                ProtectionRule(id = "r2", name = "小红书", targetPackages = setOf("com.xingin.xhs"), passDurationMs = 60_000L),
            ),
            settings = ProtectionSettings(passDurationMs = 30_000L, extensionSeconds = 60, interventionCooldownMs = 500L),
        )
        val restored = ProtectionSnapshot.fromJson(original.toJson())
        assertEquals(2, restored.rules.size)
        assertEquals("r1", restored.rules[0].id)
        assertEquals("抖音", restored.rules[0].name)
        assertEquals(setOf("com.ss.android.ugc.aweme"), restored.rules[0].targetPackages)
        assertEquals(30_000L, restored.rules[0].passDurationMs)
        assertEquals(60, restored.rules[0].extensionSeconds)
        assertEquals(setOf("com.ss.android.ugc.aweme", "com.xingin.xhs"), restored.protectedPackages)
        assertEquals(500L, restored.settings.interventionCooldownMs)
        assertEquals(12345L, restored.updatedAt)
    }

    @Test
    fun `store write then read returns written snapshot`() {
        val store = FakeSnapshotStore()
        val snapshot = ProtectionSnapshot(
            rules = listOf(ProtectionRule(id = "r1", targetPackages = setOf("com.ss.android.ugc.aweme"), passDurationMs = 60_000L)),
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
                    .put("targetPackages", org.json.JSONArray(listOf("com.example")))
                    .put("passDurationMs", 60_000),
            )))
        val parsed = ProtectionSnapshot.fromJson(minimal)
        assertEquals(1, parsed.rules.size)
        assertEquals(setOf("com.example"), parsed.rules[0].targetPackages)
        assertEquals(ProtectionSettings.DEFAULT.passDurationMs, parsed.settings.passDurationMs)
    }

    @Test
    fun `v1 legacy protectedPackages migrates to a single rule`() {
        val v1 = org.json.JSONObject()
            .put("schemaVersion", 1)
            .put("protectedPackages", org.json.JSONArray(listOf("com.ss.android.ugc.aweme")))
            .put("settings", ProtectionSettings.DEFAULT.toJson())
        val parsed = ProtectionSnapshot.fromJson(v1)
        assertEquals(1, parsed.rules.size)
        assertEquals(setOf("com.ss.android.ugc.aweme"), parsed.rules[0].targetPackages)
        assertEquals(2, parsed.schemaVersion)
        assertEquals(setOf("com.ss.android.ugc.aweme"), parsed.protectedPackages)
    }
}
