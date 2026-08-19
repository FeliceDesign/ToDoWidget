package com.felicedesign.todowidget.model

/** Everything the widget needs for one render. */
data class TodoBoard(
    /** Open tasks, plus just-ticked ones still inside their auto-hide window. */
    val active: List<Todo> = emptyList(),
    /** Ticked tasks that have already slipped out of view. */
    val archived: List<Todo> = emptyList(),
    /** Task id to the moment it was ticked, in epoch millis. Only ids inside the window appear. */
    val completedAt: Map<String, Long> = emptyMap(),
    val showArchive: Boolean = false,
    /** Non-null when the configured file could not be read or written. */
    val error: String? = null,
    val storageLabel: String = "",
) {
    val visible: List<Todo> get() = if (showArchive) archived else active

    /** The task an "undo" would bring back: the most recently ticked one still in its window. */
    val undoableId: String? get() = completedAt.maxByOrNull { it.value }?.key
}
