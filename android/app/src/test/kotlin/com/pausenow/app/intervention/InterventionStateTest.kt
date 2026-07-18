package com.pausenow.app.intervention

import com.pausenow.app.intervention.InterventionState.LaunchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 覆盖 [InterventionState] 的互斥、冷却、被盖重盖与泄漏自愈。
 */
class InterventionStateTest {

    private val pkg = "com.test.app"
    private val cooldown = 30_000L
    private val stale = 5 * 60 * 1000L // 与 InterventionState.STALE_MS 一致
    private val base = 100_000L // 远大于 cooldown，避免初始 lastLaunch=0 误判 cooldown

    @Before
    fun setUp() {
        InterventionState.reset()
    }

    @Test
    fun `first tryStart returns STARTED`() {
        assertEquals(LaunchResult.STARTED, InterventionState.tryStart(pkg, cooldown, now = base))
    }

    @Test
    fun `inFlight showing blocks second tryStart`() {
        InterventionState.tryStart(pkg, cooldown, now = base)
        InterventionState.onShowing(pkg, true) // 干预页到前台
        assertEquals(LaunchResult.SUPPRESSED, InterventionState.tryStart(pkg, cooldown, now = base + 1_000))
    }

    @Test
    fun `inFlight but hidden returns RECOVER for re-cover`() {
        // 干预页被目标应用盖到后台（showing=false），允许重盖
        InterventionState.tryStart(pkg, cooldown, now = base)
        InterventionState.onShowing(pkg, true)
        InterventionState.onShowing(pkg, false) // 被盖
        assertEquals(LaunchResult.RECOVER, InterventionState.tryStart(pkg, cooldown, now = base + 1_000))
    }

    @Test
    fun `release allows tryStart again after cooldown`() {
        assertEquals(LaunchResult.STARTED, InterventionState.tryStart(pkg, cooldown, now = base))
        InterventionState.release(pkg)
        // release 只清 inFlight，cooldown（lastLaunch）仍生效
        assertEquals(LaunchResult.SUPPRESSED, InterventionState.tryStart(pkg, cooldown, now = base + cooldown - 1))
        assertEquals(LaunchResult.STARTED, InterventionState.tryStart(pkg, cooldown, now = base + cooldown))
    }

    @Test
    fun `stale inFlight self heals after timeout`() {
        // 模拟 Activity 泄漏：tryStart 成功但 release 从未调用（华为上 onDestroy 缺失）
        assertEquals(LaunchResult.STARTED, InterventionState.tryStart(pkg, cooldown, now = base))
        // 未超时：被盖重盖
        assertEquals(LaunchResult.RECOVER, InterventionState.tryStart(pkg, cooldown, now = base + stale - 1))
        // 超过 STALE_MS：自愈，重新 STARTED
        assertEquals(LaunchResult.STARTED, InterventionState.tryStart(pkg, cooldown, now = base + stale))
    }

    @Test
    fun `isBlocked reflects inFlight`() {
        assertFalse(InterventionState.isBlocked(pkg))
        InterventionState.tryStart(pkg, cooldown, now = base)
        assertTrue(InterventionState.isBlocked(pkg))
        InterventionState.release(pkg)
        assertFalse(InterventionState.isBlocked(pkg))
    }
}
