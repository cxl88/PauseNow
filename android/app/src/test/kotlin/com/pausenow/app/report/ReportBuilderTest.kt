package com.pausenow.app.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportBuilderTest {

    private fun event(type: ProductEventType, pkg: String = "com.example", ms: Long = System.currentTimeMillis()) =
        InterventionEvent(eventId = "e${ms}_$pkg", packageName = pkg, type = type, occurredAtMs = ms)

    @Test
    fun `interventions today counts visible open and expired`() {
        val events = listOf(
            event(ProductEventType.OPEN_INTERVENTION_VISIBLE),
            event(ProductEventType.EXPIRED_INTERVENTION_VISIBLE),
            event(ProductEventType.PASS_GRANTED),
        )
        val r = ReportBuilder.build(events)
        assertEquals(2, r.interventionsToday)
    }

    @Test
    fun `stopped today counts end at expiry and exit before open`() {
        val events = listOf(
            event(ProductEventType.END_AT_EXPIRY),
            event(ProductEventType.EXIT_BEFORE_OPEN),
            event(ProductEventType.PASS_GRANTED),
        )
        val r = ReportBuilder.build(events)
        assertEquals(2, r.endedToday)
    }

    @Test
    fun `stop rate null when interventions less than 3`() {
        val events = listOf(
            event(ProductEventType.OPEN_INTERVENTION_VISIBLE),
            event(ProductEventType.END_AT_EXPIRY),
        )
        val r = ReportBuilder.build(events)
        assertNull(r.stopRate)
    }

    @Test
    fun `stop rate computed when interventions at least 3`() {
        val events = listOf(
            event(ProductEventType.OPEN_INTERVENTION_VISIBLE),
            event(ProductEventType.OPEN_INTERVENTION_VISIBLE),
            event(ProductEventType.OPEN_INTERVENTION_VISIBLE),
            event(ProductEventType.END_AT_EXPIRY),
        )
        val r = ReportBuilder.build(events)
        // 3 干预，1 主动停下 -> 1/3
        assertEquals(1.0 / 3.0, r.stopRate!!, 0.001)
    }

    @Test
    fun `passes and extensions counted`() {
        val events = listOf(
            event(ProductEventType.PASS_GRANTED, pkg = "com.a"),
            event(ProductEventType.PASS_EXTENDED, pkg = "com.a"),
            event(ProductEventType.PASS_EXTENDED, pkg = "com.b"),
        )
        val r = ReportBuilder.build(events)
        assertEquals(1, r.passesToday)
        assertEquals(2, r.extensionsToday)
    }

    @Test
    fun `trend has 7 days ending today`() {
        val events = listOf(event(ProductEventType.END_AT_EXPIRY))
        val r = ReportBuilder.build(events)
        assertEquals(7, r.trend.size)
        assertEquals("今天", r.trend.last().label)
    }

    @Test
    fun `apps grouped by package sorted by ended desc`() {
        val events = listOf(
            event(ProductEventType.PASS_GRANTED, pkg = "com.a"),
            event(ProductEventType.END_AT_EXPIRY, pkg = "com.a"),
            event(ProductEventType.PASS_GRANTED, pkg = "com.b"),
        )
        val r = ReportBuilder.build(events)
        assertEquals(2, r.apps.size)
        // com.a 有 1 主动停下，应排前
        assertEquals("com.a", r.apps[0].packageName)
        assertTrue(r.apps[0].ended >= r.apps[1].ended)
    }

    @Test
    fun `empty events produce zero report`() {
        val r = ReportBuilder.build(emptyList())
        assertEquals(0, r.interventionsToday)
        assertEquals(0, r.endedToday)
        assertNull(r.stopRate)
        assertTrue(r.apps.isEmpty())
    }
}
