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
        override fun upsert(pass: ActivePass) {
            // 同包旧 entry 清理（与生产 SharedPreferencesPassStore 一致）
            map.entries.removeIf { it.value.packageName == pass.packageName && it.key != pass.sessionId }
            map[pass.sessionId] = pass
        }
        override fun remove(sessionId: String) { map.remove(sessionId) }
    }

    private var nowMs = 1_000_000L
    private val store = FakePassStore()
    private val manager = PassManager(store, clock = { nowMs })

    private fun grant(
        pkg: String = "com.example.app",
        planned: Int = 300,
        extension: Int = 180,
        sessionId: String = "sess-${System.nanoTime()}",
    ) = manager.grant(
        GrantPassCommand(
            sessionId = sessionId,
            ruleId = "rule-1",
            packageName = pkg,
            purpose = PassPurpose.RELAX_BRIEFLY,
            plannedDurationSeconds = planned,
            extensionDurationSeconds = extension,
        ),
    )

    @Test
    fun `grant creates pass expiring after planned duration`() {
        val pass = grant(planned = 300)
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
    fun `extendOnce on valid pass extends from original expiry`() {
        val pass = grant(planned = 60, extension = 180) // expires 1_060_000
        nowMs = 1_020_000L
        val result = manager.extendOnce(pass.sessionId)
        assertTrue(result is ExtendResult.Extended)
        val extended = (result as ExtendResult.Extended).pass
        assertEquals(1_060_000L + 180_000L, extended.expiresAtMs)
        assertEquals(1, extended.extensionCount)
    }

    @Test
    fun `extendOnce on expired pass extends from now`() {
        val pass = grant(planned = 60, extension = 180) // expires 1_060_000
        nowMs = 2_000_000L
        val extended = (manager.extendOnce(pass.sessionId) as ExtendResult.Extended).pass
        assertEquals(2_000_000L + 180_000L, extended.expiresAtMs)
        assertEquals(1, extended.extensionCount)
    }

    @Test
    fun `end removes the pass`() {
        val pass = grant()
        assertNotNull(manager.currentPass("com.example.app"))
        manager.end(pass.sessionId, PassEndReason.USER_ENDED)
        assertNull(manager.currentPass("com.example.app"))
    }

    @Test
    fun `isExpired reflects clock`() {
        val pass = grant(planned = 60) // expires 1_060_000
        nowMs = 1_050_000L
        assertFalse(manager.isExpired(pass))
        nowMs = 1_060_000L
        assertTrue(manager.isExpired(pass))
    }

    @Test
    fun `pass persists across store reload`() {
        grant()
        val reloaded = PassManager(store, clock = { nowMs }).currentPass("com.example.app")
        assertNotNull(reloaded)
        assertEquals("rule-1", reloaded!!.ruleId)
    }

    @Test
    fun `granting pass for same package overwrites previous`() {
        grant(sessionId = "sess-1")
        nowMs = 2_000_000L
        grant(sessionId = "sess-2")
        val current = manager.currentPass("com.example.app")
        assertEquals(2_000_000L, current!!.grantedAtMs)
        assertEquals("sess-2", current.sessionId)
    }

    // R-006 三层测试（Domain + Store + 重启）
    @Test
    fun `extendOnce twice returns AlreadyExtended`() {
        val pass = grant(planned = 60, extension = 180)
        assertTrue(manager.extendOnce(pass.sessionId) is ExtendResult.Extended)
        nowMs = 1_020_000L
        assertEquals(ExtendResult.AlreadyExtended, manager.extendOnce(pass.sessionId))
    }

    @Test
    fun `extendOnce on unknown session returns NotFound`() {
        assertEquals(ExtendResult.NotFound, manager.extendOnce("nonexistent"))
    }

    @Test
    fun `extensionCount capped at 1 after reload`() {
        val pass = grant(planned = 60, extension = 180)
        manager.extendOnce(pass.sessionId)
        val reloadedManager = PassManager(store, clock = { nowMs })
        val reloaded = reloadedManager.currentPass("com.example.app")!!
        assertEquals(1, reloaded.extensionCount)
        assertEquals(ExtendResult.AlreadyExtended, reloadedManager.extendOnce(reloaded.sessionId))
    }

    @Test
    fun `concurrent extendOnce only one succeeds`() {
        val pass = grant(planned = 60, extension = 180)
        val latch = java.util.concurrent.CountDownLatch(1)
        val results = java.util.Collections.synchronizedList(mutableListOf<ExtendResult>())
        val t1 = Thread {
            latch.await()
            results.add(manager.extendOnce(pass.sessionId))
        }
        val t2 = Thread {
            latch.await()
            results.add(manager.extendOnce(pass.sessionId))
        }
        t1.start(); t2.start()
        latch.countDown()
        t1.join(); t2.join()
        assertEquals(2, results.size)
        assertEquals(1, results.count { it is ExtendResult.Extended })
        assertEquals(1, results.count { it is ExtendResult.AlreadyExtended })
    }
}
