package com.felicedesign.todowidget.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletionWindowTest {

    private val now = 1_800_000_000_000L
    private val fiveSecondsAgo = now - 5_000

    @Test
    fun `entries round-trip`() {
        val entry = CompletionWindow.encode("abc123", now)

        assertEquals("abc123" to now, CompletionWindow.decode(entry))
        assertEquals("abc123", CompletionWindow.idOf(entry))
    }

    @Test
    fun `ids containing the separator still decode to the right timestamp`() {
        // Duplicate tasks get a "#2" suffix, so ids are safe, but be explicit about the contract.
        assertEquals("plain" to now, CompletionWindow.decode(CompletionWindow.encode("plain", now)))
        assertNull(CompletionWindow.decode("no-timestamp"))
        assertNull(CompletionWindow.decode("id|notanumber"))
    }

    @Test
    fun `a task stays visible until its window expires`() {
        val entries = setOf(
            CompletionWindow.encode("fresh", now - 1_000),
            CompletionWindow.encode("expired", now - 9_000),
        )

        val live = CompletionWindow.live(entries, setOf("fresh", "expired"), fiveSecondsAgo)

        assertEquals(setOf("fresh"), live.keys)
    }

    @Test
    fun `a task deleted from the file drops out of the window`() {
        val entries = setOf(CompletionWindow.encode("gone", now))

        assertEquals(emptyMap<String, Long>(), CompletionWindow.live(entries, emptySet(), fiveSecondsAgo))
    }

    @Test
    fun `unreadable entries are ignored rather than crashing the widget`() {
        val entries = setOf("garbage", CompletionWindow.encode("good", now))

        assertEquals(setOf("good"), CompletionWindow.live(entries, setOf("good", "garbage"), fiveSecondsAgo).keys)
    }

    @Test
    fun `removing an id leaves every other entry untouched`() {
        val keep = CompletionWindow.encode("keep", now)
        val entries = setOf(keep, CompletionWindow.encode("drop", now))

        assertEquals(setOf(keep), CompletionWindow.without(entries, "drop"))
    }
}
