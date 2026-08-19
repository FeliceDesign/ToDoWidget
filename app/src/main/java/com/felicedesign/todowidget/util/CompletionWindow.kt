package com.felicedesign.todowidget.util

/**
 * The auto-hide window: which just-ticked tasks are still on screen.
 *
 * Kept as `id|epochMillis` strings because that is what a preferences string set can hold, and kept
 * separate from storage so the rule "visible until its window expires, and only while the task
 * still exists" can be tested on its own.
 */
object CompletionWindow {

    private const val SEPARATOR = "|"

    fun encode(id: String, atMillis: Long): String = "$id$SEPARATOR$atMillis"

    fun idOf(entry: String): String = entry.substringBefore(SEPARATOR)

    fun decode(entry: String): Pair<String, Long>? {
        val at = entry.substringAfter(SEPARATOR, "").toLongOrNull() ?: return null
        return idOf(entry) to at
    }

    /** Entries ticked after [cutoffMillis] whose task is still in the file. */
    fun live(entries: Set<String>, knownIds: Set<String>, cutoffMillis: Long): Map<String, Long> =
        entries.mapNotNull { entry ->
            val (id, at) = decode(entry) ?: return@mapNotNull null
            if (at > cutoffMillis && id in knownIds) id to at else null
        }.toMap()

    fun without(entries: Set<String>, id: String): Set<String> =
        entries.filterNot { idOf(it) == id }.toSet()
}
