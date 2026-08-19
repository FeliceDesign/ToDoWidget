package com.felicedesign.todowidget.util

import java.time.Duration
import java.time.LocalDateTime

/**
 * Formats the time left on a task, and — just as importantly — says when that text will next
 * change. A home-screen widget cannot tick freely, so the scheduler uses [nextChange] to wake up
 * exactly once per visible change instead of polling.
 */
object Countdown {

    private val UNITS = listOf(
        Duration.ofDays(1) to "d",
        Duration.ofHours(1) to "h",
        Duration.ofMinutes(1) to "m",
    )
    private val MINUTE = UNITS.last().first

    fun format(dueAt: LocalDateTime, now: LocalDateTime): String {
        val left = Duration.between(now, dueAt)
        if (left.isZero || left.isNegative) {
            val over = left.negated()
            val unit = unitFor(over) ?: return "now"
            return "-${count(over, unit.first)}${unit.second}"
        }
        val unit = unitFor(left) ?: return "<1m"
        return "${count(left, unit.first)}${unit.second}"
    }

    fun isOverdue(dueAt: LocalDateTime, now: LocalDateTime): Boolean = !dueAt.isAfter(now)

    /**
     * The first instant at which [format] returns something different from what it returns now.
     *
     * While time is left, the label counts down, so it flips just *after* the remaining time drops
     * below a whole unit — hence the extra second. Once overdue the label counts up, and flips
     * exactly *on* the next whole unit.
     */
    fun nextChange(dueAt: LocalDateTime, now: LocalDateTime): LocalDateTime {
        val left = Duration.between(now, dueAt)
        if (left.isZero || left.isNegative) {
            val over = left.negated()
            val unit = unitFor(over)?.first ?: return dueAt.plus(MINUTE)
            return dueAt.plus(unit.multipliedBy(count(over, unit) + 1))
        }
        // Less than a minute left: the label changes from "<1m" to "now" when the deadline hits.
        val unit = unitFor(left)?.first ?: return dueAt
        return dueAt.minus(unit.multipliedBy(count(left, unit))).plusSeconds(1)
    }

    private fun unitFor(duration: Duration): Pair<Duration, String>? =
        UNITS.firstOrNull { duration >= it.first }

    private fun count(duration: Duration, unit: Duration): Long =
        duration.toMillis() / unit.toMillis()
}
