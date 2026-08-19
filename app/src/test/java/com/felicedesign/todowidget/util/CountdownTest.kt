package com.felicedesign.todowidget.util

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 19, 12, 0)

    private fun labelIn(days: Long = 0, hours: Long = 0, minutes: Long = 0, seconds: Long = 0) =
        Countdown.format(
            now.plusDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds),
            now,
        )

    @Test
    fun `shows the largest whole unit that fits`() {
        assertEquals("3d", labelIn(days = 3, hours = 5))
        assertEquals("1d", labelIn(hours = 25))
        assertEquals("23h", labelIn(hours = 23, minutes = 59))
        assertEquals("1h", labelIn(minutes = 60))
        assertEquals("59m", labelIn(minutes = 59))
        assertEquals("1m", labelIn(minutes = 1))
    }

    @Test
    fun `sub-minute and elapsed deadlines read sensibly`() {
        assertEquals("<1m", labelIn(seconds = 59))
        assertEquals("now", labelIn())
        assertEquals("now", labelIn(seconds = -30))
        assertEquals("-1m", labelIn(minutes = -1))
        assertEquals("-2h", labelIn(hours = -2))
        assertEquals("-3d", labelIn(days = -3))
    }

    @Test
    fun `overdue detection includes the exact deadline`() {
        assertTrue(Countdown.isOverdue(now, now))
        assertTrue(Countdown.isOverdue(now.minusSeconds(1), now))
        assertFalse(Countdown.isOverdue(now.plusSeconds(1), now))
    }

    @Test
    fun `next change lands exactly on the boundary where the label flips`() {
        val due = now.plusHours(3).plusMinutes(20)
        val flip = Countdown.nextChange(due, now)

        assertEquals("3h", Countdown.format(due, now))
        assertEquals(now.plusMinutes(20).plusSeconds(1), flip)
        assertEquals("2h", Countdown.format(due, flip))
        assertEquals("3h", Countdown.format(due, flip.minusSeconds(1)))
    }

    @Test
    fun `next change works across day, minute and overdue boundaries`() {
        val inTwoDays = now.plusDays(2).plusHours(6)
        assertEquals(now.plusHours(6).plusSeconds(1), Countdown.nextChange(inTwoDays, now))

        val soon = now.plusSeconds(30)
        assertEquals(soon, Countdown.nextChange(soon, now))

        val justOverdue = now.minusSeconds(10)
        assertEquals(justOverdue.plusMinutes(1), Countdown.nextChange(justOverdue, now))

        val longOverdue = now.minusHours(2).minusMinutes(15)
        assertEquals(longOverdue.plusHours(3), Countdown.nextChange(longOverdue, now))
    }

    @Test
    fun `every scheduled wake-up actually changes the label`() {
        var due = now.plusDays(2)
        var probe = now
        repeat(20) {
            val before = Countdown.format(due, probe)
            val next = Countdown.nextChange(due, probe)
            assertTrue("next change must move forward", next.isAfter(probe))
            assertEquals("label must hold until the wake-up", before, Countdown.format(due, next.minusSeconds(1)))
            assertNotEquals("label must differ at the wake-up", before, Countdown.format(due, next))
            probe = next
        }
        due = now.minusDays(1)
        probe = now
        repeat(5) {
            val next = Countdown.nextChange(due, probe)
            assertTrue(next.isAfter(probe))
            probe = next
        }
    }
}
