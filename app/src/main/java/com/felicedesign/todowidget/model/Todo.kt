package com.felicedesign.todowidget.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A single task, as it exists in the Markdown file.
 *
 * Everything here round-trips through Markdown. Transient UI state — such as the moment an
 * item was ticked, which drives the auto-hide animation — deliberately lives outside this
 * model so the user's vault does not fill up with second-precision timestamps.
 */
data class Todo(
    val id: String,
    val text: String,
    val priority: Priority? = null,
    val createdOn: LocalDate? = null,
    val dueAt: LocalDateTime? = null,
    val durationMinutes: Long? = null,
    val done: Boolean = false,
    val completedOn: LocalDate? = null,
    /** Index into the source document's lines; -1 for a task not yet written to the file. */
    val lineIndex: Int = -1,
    /** Indent + list marker + checkbox prefix, preserved verbatim (e.g. `"- "`, `"    * "`). */
    val listPrefix: String = "- ",
)
