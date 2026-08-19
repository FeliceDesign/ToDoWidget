package com.felicedesign.todowidget.data.markdown

import com.felicedesign.todowidget.model.Priority
import com.felicedesign.todowidget.model.Todo
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoMarkdownTest {

    private val vaultNote = """
        ---
        tags: [inbox]
        ---

        # Today

        Some prose that must survive untouched.

        - [ ] Buy milk ⏫ ➕ 2026-08-19 📅 2026-08-21
        - [ ] Submit the form 🔼 📅 2026-08-20 [due_time:: 14:30] [duration:: 4h]
        - [x] Call the landlord 🔽 ✅ 2026-08-18
        - [ ] Untagged task with #tag and 🔁 every day

        ## Notes

        More prose.
    """.trimIndent()

    @Test
    fun `parses every task and leaves other lines alone`() {
        val doc = TodoMarkdown.parse(vaultNote)

        assertEquals(4, doc.todos.size)
        assertEquals(vaultNote, doc.toText())
    }

    @Test
    fun `reads all supported fields`() {
        val todo = TodoMarkdown.parse(vaultNote).todos[1]

        assertEquals("Submit the form", todo.text)
        assertEquals(Priority.MEDIUM, todo.priority)
        assertEquals(LocalDateTime.of(2026, 8, 20, 14, 30), todo.dueAt)
        assertEquals(240L, todo.durationMinutes)
        assertFalse(todo.done)
    }

    @Test
    fun `a due date without a time means end of day`() {
        val todo = TodoMarkdown.parse(vaultNote).todos[0]

        assertEquals(LocalDate.of(2026, 8, 19), todo.createdOn)
        assertEquals(LocalDateTime.of(2026, 8, 21, 23, 59), todo.dueAt)
    }

    @Test
    fun `keeps unrecognised content in the description`() {
        val todo = TodoMarkdown.parse(vaultNote).todos[3]

        assertEquals("Untagged task with #tag and 🔁 every day", todo.text)
        assertNull(todo.priority)
        assertNull(todo.dueAt)
    }

    @Test
    fun `every task round-trips through render unchanged`() {
        val doc = TodoMarkdown.parse(vaultNote)

        doc.todos.forEach { todo ->
            assertEquals(doc.lines[todo.lineIndex], TodoMarkdown.render(todo))
        }
    }

    @Test
    fun `completing a task only rewrites its own line`() {
        val doc = TodoMarkdown.parse(vaultNote)
        val target = doc.todos.first()

        val updated = doc.withUpdated(
            target.copy(done = true, completedOn = LocalDate.of(2026, 8, 19)),
        )

        assertEquals(
            "- [x] Buy milk ⏫ ➕ 2026-08-19 📅 2026-08-21 ✅ 2026-08-19",
            updated.lines[target.lineIndex],
        )
        assertEquals(doc.lines.size, updated.lines.size)
        doc.lines.indices.filter { it != target.lineIndex }.forEach {
            assertEquals(doc.lines[it], updated.lines[it])
        }
    }

    @Test
    fun `re-opening a task drops the completion date`() {
        val doc = TodoMarkdown.parse(vaultNote)
        val done = doc.todos.first { it.done }

        val updated = doc.withUpdated(done.copy(done = false, completedOn = null))

        assertEquals("- [x] Call the landlord 🔽 ✅ 2026-08-18", doc.lines[done.lineIndex])
        assertEquals("- [ ] Call the landlord 🔽", updated.lines[done.lineIndex])
    }

    @Test
    fun `a new task lands directly below the last one`() {
        val doc = TodoMarkdown.parse(vaultNote)
        val lastTaskLine = doc.todos.maxOf { it.lineIndex }

        val updated = doc.withAdded(
            Todo(id = "", text = "Water the plants", priority = Priority.LOW),
        )

        assertEquals("- [ ] Water the plants 🔽", updated.lines[lastTaskLine + 1])
        assertEquals(5, updated.todos.size)
        assertTrue(updated.toText().endsWith("More prose."))
    }

    @Test
    fun `adding to an empty file still produces a task`() {
        val updated = TodoDocument.EMPTY.withAdded(Todo(id = "", text = "First"))

        assertEquals(1, updated.todos.size)
        assertEquals("First", updated.todos.single().text)
    }

    @Test
    fun `removing a task deletes exactly one line`() {
        val doc = TodoMarkdown.parse(vaultNote)
        val target = doc.todos[1]

        val updated = doc.withRemoved(target.id)

        assertEquals(doc.lines.size - 1, updated.lines.size)
        assertEquals(3, updated.todos.size)
        assertTrue(updated.todos.none { it.text == "Submit the form" })
    }

    @Test
    fun `identical tasks get distinct ids`() {
        val doc = TodoMarkdown.parse("- [ ] Repeat me\n- [ ] Repeat me")

        assertEquals(2, doc.todos.map { it.id }.distinct().size)
    }

    @Test
    fun `preserves indentation, list markers and CRLF line endings`() {
        val source = "# List\r\n\r\n    * [ ] Indented task ⏫\r\n"
        val doc = TodoMarkdown.parse(source)

        val todo = doc.todos.single()
        assertEquals("    * ", todo.listPrefix)
        assertEquals(source, doc.toText())
        assertEquals(source, doc.withUpdated(todo.copy(text = "Indented task")).toText())
    }

    @Test
    fun `reads the five Tasks priority levels into three levels`() {
        val doc = TodoMarkdown.parse(
            """
            - [ ] Highest 🔺
            - [ ] High ⏫
            - [ ] Medium 🔼
            - [ ] Low 🔽
            - [ ] Lowest ⏬
            """.trimIndent(),
        )

        assertEquals(
            listOf(Priority.HIGH, Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.LOW),
            doc.todos.map { it.priority },
        )
    }

    @Test
    fun `durations round-trip`() {
        listOf("30m" to 30L, "4h" to 240L, "1d" to 1440L, "1w" to 10080L, "1h30m" to 90L)
            .forEach { (text, minutes) ->
                assertEquals(minutes, TodoMarkdown.parseDuration(text))
                assertEquals(text, TodoMarkdown.formatDuration(minutes))
            }
        assertEquals(45L, TodoMarkdown.parseDuration("45"))
        assertNull(TodoMarkdown.parseDuration("soon"))
    }
}
