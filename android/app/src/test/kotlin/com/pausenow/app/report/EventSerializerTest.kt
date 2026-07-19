package com.pausenow.app.report

import com.pausenow.app.pass.PassPurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSerializerTest {

    private fun event(type: ProductEventType, pkg: String = "com.example", ms: Long = 1000L) =
        InterventionEvent(
            eventId = "e1",
            packageName = pkg,
            type = type,
            occurredAtMs = ms,
            purpose = PassPurpose.RELAX_BRIEFLY,
            durationSeconds = 300,
        )

    @Test
    fun `round trip preserves events`() {
        val events = listOf(
            event(ProductEventType.PASS_GRANTED, pkg = "com.a"),
            event(ProductEventType.END_AT_EXPIRY, pkg = "com.b"),
        )
        val parsed = EventSerializer.parse(EventSerializer.serialize(events))
        assertEquals(2, parsed.size)
        assertEquals(ProductEventType.PASS_GRANTED, parsed[0].type)
        assertEquals("com.a", parsed[0].packageName)
        assertEquals(PassPurpose.RELAX_BRIEFLY, parsed[0].purpose)
        assertEquals(300, parsed[0].durationSeconds)
    }

    @Test
    fun `parse empty or invalid json returns empty`() {
        assertTrue(EventSerializer.parse("").isEmpty())
        assertTrue(EventSerializer.parse("not json").isEmpty())
    }

    @Test
    fun `legacy string events migrate to enum`() {
        val legacy = """[{"type":"grant","packageName":"com.a","timestamp":1000},{"type":"end","packageName":"com.b","timestamp":2000}]"""
        val parsed = EventSerializer.parse(legacy)
        assertEquals(2, parsed.size)
        assertEquals(ProductEventType.PASS_GRANTED, parsed[0].type)
        assertEquals(ProductEventType.END_AT_EXPIRY, parsed[1].type)
        assertEquals(1000L, parsed[0].occurredAtMs) // timestamp -> occurredAtMs
    }

    @Test
    fun `legacy open expired extend migrate`() {
        val legacy = """[{"type":"open","packageName":"com.a","timestamp":1000},{"type":"expired","packageName":"com.a","timestamp":2000},{"type":"extend","packageName":"com.a","timestamp":3000}]"""
        val parsed = EventSerializer.parse(legacy)
        assertEquals(ProductEventType.OPEN_INTERVENTION_VISIBLE, parsed[0].type)
        assertEquals(ProductEventType.EXPIRED_INTERVENTION_VISIBLE, parsed[1].type)
        assertEquals(ProductEventType.PASS_EXTENDED, parsed[2].type)
    }
}
