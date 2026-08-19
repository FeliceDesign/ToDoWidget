package com.felicedesign.todowidget.data.markdown

import com.felicedesign.todowidget.model.Todo

/**
 * A parsed Markdown file: every original line, plus the tasks found in them.
 *
 * Edits rewrite exactly one line (or insert one), leave every other line byte-identical, and then
 * re-parse so ids and line indices stay in sync.
 */
data class TodoDocument(
    val lines: List<String>,
    val todos: List<Todo>,
    val usesCrLf: Boolean = false,
) {
    private val lineSeparator: String get() = if (usesCrLf) "\r\n" else "\n"

    fun toText(): String = lines.joinToString(lineSeparator)

    fun find(id: String): Todo? = todos.firstOrNull { it.id == id }

    fun withUpdated(todo: Todo): TodoDocument {
        if (todo.lineIndex !in lines.indices) return this
        val updated = lines.toMutableList()
        updated[todo.lineIndex] = TodoMarkdown.render(todo)
        return reparse(updated)
    }

    fun withRemoved(id: String): TodoDocument {
        val todo = find(id) ?: return this
        if (todo.lineIndex !in lines.indices) return this
        val updated = lines.toMutableList()
        updated.removeAt(todo.lineIndex)
        return reparse(updated)
    }

    /** Appends a task directly below the last existing one, keeping the list contiguous. */
    fun withAdded(todo: Todo): TodoDocument {
        val updated = lines.toMutableList()
        val insertAt = todos.maxOfOrNull { it.lineIndex }?.plus(1) ?: contentEnd()
        updated.add(insertAt.coerceIn(0, updated.size), TodoMarkdown.render(todo))
        return reparse(updated)
    }

    /** The index just past the last non-blank line, so we insert before any trailing blank lines. */
    private fun contentEnd(): Int {
        var end = lines.size
        while (end > 0 && lines[end - 1].isBlank()) end--
        return end
    }

    private fun reparse(updated: List<String>): TodoDocument =
        TodoMarkdown.parse(updated.joinToString(lineSeparator))

    companion object {
        val EMPTY = TodoDocument(listOf(""), emptyList())
    }
}
