package com.felicedesign.todowidget.data

import com.felicedesign.todowidget.model.Priority
import com.felicedesign.todowidget.model.Todo
import java.time.LocalDateTime

/**
 * The order tasks appear in the widget: anything already past its deadline first, then by
 * priority, then by how soon it is due, and finally in file order so the list never shuffles
 * for no reason.
 */
object TodoSorting {

    fun active(todos: List<Todo>, now: LocalDateTime): List<Todo> =
        todos.sortedWith(
            compareBy<Todo> { todo -> if (todo.dueAt != null && !todo.dueAt.isAfter(now)) 0 else 1 }
                .thenBy { rank(it.priority) }
                .thenBy { it.dueAt ?: LocalDateTime.MAX }
                .thenBy { it.lineIndex },
        )

    fun archived(todos: List<Todo>): List<Todo> =
        todos.sortedWith(
            compareByDescending<Todo> { it.completedOn }.thenByDescending { it.lineIndex },
        )

    private fun rank(priority: Priority?): Int = when (priority) {
        Priority.HIGH -> 0
        Priority.MEDIUM -> 1
        Priority.LOW -> 2
        null -> 3
    }
}
