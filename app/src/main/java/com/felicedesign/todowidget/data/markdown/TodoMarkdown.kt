package com.felicedesign.todowidget.data.markdown

import com.felicedesign.todowidget.model.Priority
import com.felicedesign.todowidget.model.Todo
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Reads and writes tasks in the Obsidian Tasks emoji format, e.g.
 *
 * ```
 * - [ ] Buy milk ⏫ ➕ 2026-08-19 📅 2026-08-21 [duration:: 4h]
 * - [x] Call the landlord 🔽 ✅ 2026-08-18
 * ```
 *
 * The parser is deliberately forgiving: anything it does not recognise stays part of the task
 * description and is written back out verbatim, and lines that are not tasks at all (headings,
 * prose, front matter) are never touched. That keeps a real vault note usable in Obsidian.
 */
object TodoMarkdown {

    /** A due date without an explicit time means "by the end of that day". */
    val DEFAULT_DUE_TIME: LocalTime = LocalTime.of(23, 59)

    private const val CREATED = "➕"
    private const val DUE = "📅"
    private const val DONE = "✅"

    private const val FIELD_DUE_TIME = "due_time"
    private const val FIELD_DUE = "due"
    private const val FIELD_DURATION = "duration"

    private val TASK_LINE = Regex("""^(\s*(?:[-*+]|\d+[.)])\s+)\[([ xX])]\s?(.*)$""")
    private val INLINE_FIELD = Regex("""\[([A-Za-z_][A-Za-z0-9_]*)::\s*([^]]*)]""")
    private val DATE_TOKEN = Regex("""($CREATED|$DUE|$DONE|🛫|⏳|❌)\s*(\d{4}-\d{2}-\d{2})""")
    private val DURATION_PART = Regex("""(\d+)\s*([wdhm])""", RegexOption.IGNORE_CASE)
    private val EXTRA_SPACE = Regex("""\s{2,}""")

    private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parse(text: String): TodoDocument {
        val usesCrLf = text.contains("\r\n")
        val lines = text.split("\n").map { it.removeSuffix("\r") }

        val parsed = lines.mapIndexedNotNull { index, line -> parseLine(line, index) }
        return TodoDocument(lines, assignIds(parsed), usesCrLf)
    }

    /** Renders a task as a complete Markdown line, including its indent and list marker. */
    fun render(todo: Todo): String = buildString {
        append(todo.listPrefix)
        append(if (todo.done) "[x] " else "[ ] ")
        append(todo.text.trim())
        todo.priority?.let { append(' ').append(it.emoji) }
        todo.createdOn?.let { append(' ').append(CREATED).append(' ').append(it) }
        todo.dueAt?.let { due ->
            append(' ').append(DUE).append(' ').append(due.toLocalDate())
            if (due.toLocalTime() != DEFAULT_DUE_TIME) {
                append(" [").append(FIELD_DUE_TIME).append(":: ").append(TIME_FORMAT.format(due)).append(']')
            }
        }
        todo.durationMinutes?.let {
            append(" [").append(FIELD_DURATION).append(":: ").append(formatDuration(it)).append(']')
        }
        if (todo.done) {
            todo.completedOn?.let { append(' ').append(DONE).append(' ').append(it) }
        }
    }

    private fun parseLine(line: String, index: Int): Todo? {
        val match = TASK_LINE.matchEntire(line) ?: return null
        val (prefix, checkbox, body) = match.destructured

        val fields = mutableMapOf<String, String>()
        var rest = INLINE_FIELD.replace(body) { m ->
            fields[m.groupValues[1].lowercase()] = m.groupValues[2].trim()
            ""
        }

        val dates = mutableMapOf<String, LocalDate>()
        rest = DATE_TOKEN.replace(rest) { m ->
            val date = runCatching { LocalDate.parse(m.groupValues[2]) }.getOrNull()
            if (date == null) {
                m.value
            } else {
                // Only the markers we own are consumed; 🛫/⏳/❌ stay in the description so the
                // Tasks plugin keeps seeing them.
                val marker = m.groupValues[1]
                if (marker in setOf(CREATED, DUE, DONE)) {
                    dates[marker] = date
                    ""
                } else {
                    m.value
                }
            }
        }

        var priority: Priority? = null
        for (emoji in Priority.ALL_EMOJI.sortedBy { rest.indexOf(it).takeIf { i -> i >= 0 } ?: Int.MAX_VALUE }) {
            if (rest.contains(emoji)) {
                if (priority == null) priority = Priority.fromEmoji(emoji)
                rest = rest.replace(emoji, "")
            }
        }

        val dueTime = fields[FIELD_DUE_TIME]?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val dueAt = fields[FIELD_DUE]?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
            ?: dates[DUE]?.atTime(dueTime ?: DEFAULT_DUE_TIME)

        return Todo(
            id = "",
            text = EXTRA_SPACE.replace(rest, " ").trim(),
            priority = priority,
            createdOn = dates[CREATED],
            dueAt = dueAt,
            durationMinutes = fields[FIELD_DURATION]?.let(::parseDuration),
            done = !checkbox.equals(" ", ignoreCase = true),
            completedOn = dates[DONE],
            lineIndex = index,
            listPrefix = prefix,
        )
    }

    /**
     * Gives every task a content-derived id that survives rewriting the file. Two tasks that are
     * genuinely identical get a `#2`, `#3`, … suffix so they stay individually addressable.
     */
    private fun assignIds(todos: List<Todo>): List<Todo> {
        val seen = mutableMapOf<String, Int>()
        return todos.map { todo ->
            val base = hash("${todo.text}|${todo.priority}|${todo.createdOn}|${todo.dueAt}")
            val count = seen.merge(base, 1, Int::plus)!!
            todo.copy(id = if (count == 1) base else "$base#$count")
        }
    }

    private fun hash(input: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray())
            .take(8)
            .joinToString("") { "%02x".format(it) }

    fun parseDuration(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        trimmed.toLongOrNull()?.let { return it.takeIf { m -> m > 0 } }

        var total = 0L
        var matched = false
        for (part in DURATION_PART.findAll(trimmed)) {
            matched = true
            val value = part.groupValues[1].toLong()
            total += when (part.groupValues[2].lowercase()) {
                "w" -> value * 7 * 24 * 60
                "d" -> value * 24 * 60
                "h" -> value * 60
                else -> value
            }
        }
        return if (matched && total > 0) total else null
    }

    fun formatDuration(minutes: Long): String {
        if (minutes <= 0) return "0m"
        var remaining = minutes
        return buildString {
            listOf("w" to 7L * 24 * 60, "d" to 24L * 60, "h" to 60L, "m" to 1L).forEach { (unit, size) ->
                val count = remaining / size
                if (count > 0) {
                    append(count).append(unit)
                    remaining -= count * size
                }
            }
        }
    }
}
