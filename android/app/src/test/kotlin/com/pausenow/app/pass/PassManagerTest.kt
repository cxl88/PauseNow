package com.pausenow.app.pass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PassManagerTest {

    private class FakePassStore : PassStore {
        val map = mutableMapOf<String, ActivePass>()
        override fun load(): Map<String, ActivePass> = map.toMap()
        override fun upsert(pass: ActivePass) { map[pass.packageName] = pass }
        override fun remove(packageName: String) { map.remove(packageName) }
    }

    private var nowMs = 1_000_000L
    private val store = FakePassStore()
    private val manager = PassManager(store, clock = { nowMs })

    @Test
    fun `grantPass creates pass expiring after duration`() {
        val pass = manager.grantPass("com.example.app", "rule-1", 5 * 60 * 1000L)
        assertEquals("rule-1", pass.ruleId)
        assertEquals("com.example.app", pass.packageName)
        assertEquals(1_000_000L, pass.grantedAtMs)
        assertEquals(1_300_000L, pass.expiresAtMs)
        assertEquals(pass, manager.currentPass("com.example.app"))
    }

    @Test
    fun `currentPass returns null when none granted`() {
        assertNull(manager.currentPass("com.other.app"))
    }

    @Test
    fun `extendPass on valid pass extends from original expiry`() {
        val pass = manager.grantPass("com.example.app", "rule-1", 60_000L) // expires 1_060_000
        nowMs = 1_020_000L // 未到期
        val extended = manager.extendPass(pass, 180)
        assertEquals(1_060_000L + 180_000L, extended.expiresAtMs)
        assertEquals(1, extended.extensionCount)
        assertEquals(extended, manager.currentPass("com.example.app"))
    }

    @Test
    fun `extendPass on expired pass extends from now`() {
        val pass = manager.grantPass("com.example.app", "rule-1", 60_000L) // expires 1_060_000
        nowMs = 2_000_000L // 已过期
        val extended = manager.extendPass(pass, 180)
        // 从 now(2_000_000) 起算延长 180s，而不是从已过期的 1_060_000
        assertEquals(2_000_000L + 180_000L, extended.expiresAtMs)
        assertEquals(1, extended.extensionCount)
    }

    @Test
    fun `endPass removes the pass`() {
        manager.grantPass("com.example.app", "rule-1", 60_000L)
        assertNotNull(manager.currentPass("com.example.app"))
        manager.endPass("com.example.app")
        assertNull(manager.currentPass("com.example.app"))
    }

    @Test
    fun `isExpired reflects clock`() {
        val pass = manager.grantPass("com.example.app", "rule-1", 60_000L) // expires 1_060_000
        nowMs = 1_050_000L
        assertFalse(manager.isExpired(pass))
        nowMs = 1_060_000L
        assertTrue(manager.isExpired(pass))
        nowMs = 1_100_000L
        assertTrue(manager.isExpired(pass))
    }

    @Test
    fun `pass persists across store reload`() {
        manager.grantPass("com.example.app", "rule-1", 60_000L)
        val reloaded = PassManager(store, clock = { nowMs }).currentPass("com.example.app")
        assertNotNull(reloaded)
        assertEquals("rule-1", reloaded!!.ruleId)
    }

    @Test
    fun `granting pass for same package overwrites previous`() {
        manager.grantPass("com.example.app", "rule-1", 60_000L)
        nowMs = 2_000_000L
        manager.grantPass("com.example.app", "rule-1", 60_000L)
        val current = manager.currentPass("com.example.app")
        assertEquals(2_000_000L, current!!.grantedAtMs)
    }
}
