package com.pausenow.app.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDebouncerTest {
    @Test
    fun `same package only produces one event per foreground session`() {
        val debouncer = EventDebouncer()

        assertTrue(debouncer.shouldAccept("com.example.video"))
        assertFalse(debouncer.shouldAccept("com.example.video"))
        assertFalse(debouncer.shouldAccept("com.example.video"))
    }

    @Test
    fun `leaving and reopening package starts a new session`() {
        val debouncer = EventDebouncer()

        assertTrue(debouncer.shouldAccept("com.example.video"))
        assertTrue(debouncer.shouldAccept("com.android.launcher3"))
        assertTrue(debouncer.shouldAccept("com.example.video"))
    }
}
