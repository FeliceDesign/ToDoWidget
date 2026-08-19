package com.felicedesign.todowidget.util

import com.felicedesign.todowidget.model.Priority
import com.felicedesign.todowidget.model.Todo
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoSortingTest {

    private val now: LocalDateTime = LocalDateTime.of(2026, 8, 19, 12, 0)

    private fun todo(
        text: String,
        priority: Priority? = null,
        dueIn: Long? = null,
        line: Int = 0,
    ) = Todo(
        id = text,
        text = text,
        priority = priority,
        dueAt = dueIn?.let { now.plusHours(it) },
        lineIndex = line,
    )

    @Test
    fun `anything past its deadline comes first`() {
        val order = TodoSorting.active(
            listOf(
                todo("high, next week", Priority.HIGH, dueIn = 24 * 7),
                todo("low, an hour ago", Priority.LOW, dueIn = -1),
            ),
            now,
        )

        assertEquals(listOf("low, an hour ago", "high, next week"), order.map { it.text })
    }

    @Test
    fun `then priority, then how soon, then file order`() {
        val order = TodoSorting.active(
            listOf(
                todo("no priority", null, dueIn = 1, line = 0),
                todo("low", Priority.LOW, dueIn = 1, line = 1),
                todo("medium later", Priority.MEDIUM, dueIn = 10, line = 2),
                todo("medium sooner", Priority.MEDIUM, dueIn = 2, line = 3),
                todo("high, no deadline", Priority.HIGH, dueIn = null, line = 4),
                todo("high, tomorrow", Priority.HIGH, dueIn = 24, line = 5),
            ),
            now,
        )

        assertEquals(
            listOf(
                "high, tomorrow",
                "high, no deadline",
                "medium sooner",
                "medium later",
                "low",
                "no priority",
            ),
            order.map { it.text },
        )
    }

    @Test
    fun `equal tasks keep their order in the file`() {
        val order = TodoSorting.active(
            listOf(
                todo("second", Priority.HIGH, dueIn = 5, line = 7),
                todo("first", Priority.HIGH, dueIn = 5, line = 3),
            ),
            now,
        )

        assertEquals(listOf("first", "second"), order.map { it.text })
    }

    @Test
    fun `the archive shows the most recently completed first`() {
        val order = TodoSorting.archived(
            listOf(
                todo("older", line = 1).copy(done = true, completedOn = LocalDate.of(2026, 8, 10)),
                todo("newer", line = 2).copy(done = true, completedOn = LocalDate.of(2026, 8, 18)),
                todo("undated", line = 3).copy(done = true),
            ),
        )

        assertEquals(listOf("newer", "older", "undated"), order.map { it.text })
    }
}
