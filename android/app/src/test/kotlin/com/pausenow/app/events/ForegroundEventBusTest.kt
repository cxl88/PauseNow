package com.pausenow.app.events

import com.pausenow.app.accessibility.ForegroundPackageEventRecord
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundEventBusTest {

    private val recorded = mutableListOf<ForegroundPackageEventRecord>()

    private val listener: (ForegroundPackageEventRecord) -> Unit = { recorded.add(it) }

    @After
    fun tearDown() {
        ForegroundEventBus.unregister(listener)
        recorded.clear()
    }

    @Test
    fun `registered listener receives published events`() {
        val event = sampleEvent(packageName = "com.example.video", detectedAtMs = 1L)

        ForegroundEventBus.register(listener)
        ForegroundEventBus.publish(event)

        assertEquals(listOf(event), recorded)
    }

    @Test
    fun `unregistered listener stops receiving events`() {
        val event = sampleEvent(packageName = "com.example.video", detectedAtMs = 2L)

        ForegroundEventBus.register(listener)
        ForegroundEventBus.unregister(listener)
        ForegroundEventBus.publish(event)

        assertTrue("unregistered listener should not receive events", recorded.isEmpty())
    }

    @Test
    fun `duplicate registration only delivers each event once`() {
        val event = sampleEvent(packageName = "com.example.video", detectedAtMs = 3L)

        ForegroundEventBus.register(listener)
        ForegroundEventBus.register(listener)
        ForegroundEventBus.publish(event)

        assertEquals(1, recorded.size)
    }

    @Test
    fun `multiple listeners each receive published events`() {
        val first = mutableListOf<ForegroundPackageEventRecord>()
        val second = mutableListOf<ForegroundPackageEventRecord>()
        val firstListener: (ForegroundPackageEventRecord) -> Unit = { first.add(it) }
        val secondListener: (ForegroundPackageEventRecord) -> Unit = { second.add(it) }

        try {
            ForegroundEventBus.register(firstListener)
            ForegroundEventBus.register(secondListener)
            ForegroundEventBus.publish(sampleEvent("com.example.video", 4L))

            assertEquals(1, first.size)
            assertEquals(1, second.size)
        } finally {
            ForegroundEventBus.unregister(firstListener)
            ForegroundEventBus.unregister(secondListener)
        }
    }

    private fun sampleEvent(packageName: String, detectedAtMs: Long) = ForegroundPackageEventRecord(
        packageName = packageName,
        eventType = "TYPE_WINDOW_STATE_CHANGED",
        detectedAtMs = detectedAtMs,
    )
}
