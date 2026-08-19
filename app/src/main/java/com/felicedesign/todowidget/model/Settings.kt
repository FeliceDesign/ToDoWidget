package com.felicedesign.todowidget.model

/** Every colour the user can change. Stored as ARGB ints; alpha 0 on [background] means see-through. */
data class Appearance(
    val highlight: Int = DEFAULT_HIGHLIGHT,
    val background: Int = DEFAULT_BACKGROUND,
    val text: Int = DEFAULT_TEXT,
    val highPriority: Int = DEFAULT_HIGH,
    val mediumPriority: Int = DEFAULT_MEDIUM,
    val lowPriority: Int = DEFAULT_LOW,
) {
    fun colorFor(priority: Priority?): Int = when (priority) {
        Priority.HIGH -> highPriority
        Priority.MEDIUM -> mediumPriority
        Priority.LOW -> lowPriority
        null -> text
    }

    companion object {
        const val DEFAULT_HIGHLIGHT: Int = 0xFF3B82F6.toInt()
        const val DEFAULT_BACKGROUND: Int = 0xE60F1419.toInt()
        const val DEFAULT_TEXT: Int = 0xFFE8EDF2.toInt()
        const val DEFAULT_HIGH: Int = 0xFFF75A5A.toInt()
        const val DEFAULT_MEDIUM: Int = 0xFFE3B341.toInt()
        const val DEFAULT_LOW: Int = 0xFF4ECB71.toInt()
    }
}

enum class StorageMode { INTERNAL, DOCUMENT }

/**
 * Where the todo file lives. [DOCUMENT] uses either a single picked `.md` file or a picked folder
 * (an Obsidian vault) plus [fileName].
 */
data class StorageConfig(
    val mode: StorageMode = StorageMode.INTERNAL,
    val documentUri: String? = null,
    val treeUri: String? = null,
    val fileName: String = DEFAULT_FILE_NAME,
) {
    companion object {
        const val DEFAULT_FILE_NAME = "ToDoWidget.md"
    }
}

data class Settings(
    val appearance: Appearance = Appearance(),
    /** How long a ticked item stays visible before it slips into the archive. */
    val autoHideSeconds: Int = DEFAULT_AUTO_HIDE_SECONDS,
    /** Pre-selected duration in the add sheet; null means "no deadline". */
    val defaultDurationMinutes: Long? = null,
    val storage: StorageConfig = StorageConfig(),
) {
    companion object {
        const val DEFAULT_AUTO_HIDE_SECONDS = 5
        val AUTO_HIDE_RANGE = 1..60
    }
}
