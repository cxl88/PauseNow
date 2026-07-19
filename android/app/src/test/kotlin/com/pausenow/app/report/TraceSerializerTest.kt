package com.pausenow.app.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TraceSerializerTest {

    private fun trace(
        traceId: String = "t1",
        mode: InterventionMode = InterventionMode.OPEN,
        visibleAtMs: Long? = null,
        actionResult: ActionResultType? = null,
    ) = InterventionTrace(
        traceId = traceId,
        ruleId = "rule-1",
        packageName = "com.example",
        mode = mode,
        detectedAtMs = 1000L,
        decisionAtMs = 1100L,
        launchRequestedAtMs = 1200L,
        visibleAtMs = visibleAtMs,
        actionAtMs = visibleAtMs?.let { it + 500 },
        launchResult = LaunchResultType.STARTED,
        actionResult = actionResult,
    )

    @Test
    fun `round trip preserves trace`() {
        val traces = listOf(trace(visibleAtMs = 1500L, actionResult = ActionResultType.GRANTED))
        val parsed = TraceSerializer.parse(TraceSerializer.serialize(traces))
        assertEquals(1, parsed.size)
        assertEquals("t1", parsed[0].traceId)
        assertEquals(InterventionMode.OPEN, parsed[0].mode)
        assertEquals(1500L, parsed[0].visibleAtMs)
        assertEquals(ActionResultType.GRANTED, parsed[0].actionResult)
        assertEquals(LaunchResultType.STARTED, parsed[0].launchResult)
    }

    @Test
    fun `parse empty or invalid returns empty`() {
        assertTrue(TraceSerializer.parse("").isEmpty())
        assertTrue(TraceSerializer.parse("not json").isEmpty())
    }

    @Test
    fun `nullable fields are null when absent`() {
        val traces = listOf(trace(visibleAtMs = null, actionResult = null))
        val parsed = TraceSerializer.parse(TraceSerializer.serialize(traces))
        assertNull(parsed[0].visibleAtMs)
        assertNull(parsed[0].actionAtMs)
        assertNull(parsed[0].actionResult)
    }

    @Test
    fun `expired mode round trips`() {
        val traces = listOf(trace(mode = InterventionMode.EXPIRED))
        val parsed = TraceSerializer.parse(TraceSerializer.serialize(traces))
        assertEquals(InterventionMode.EXPIRED, parsed[0].mode)
    }
}
