package com.felicedesign.todowidget.model

/**
 * Three user-facing priority levels, mapped to the Obsidian Tasks emoji vocabulary.
 *
 * Tasks knows five levels; we read all of them but only ever write the three we expose,
 * folding "highest" into [HIGH] and "lowest" into [LOW].
 */
enum class Priority(val emoji: String) {
    HIGH("⏫"),
    MEDIUM("🔼"),
    LOW("🔽"),
    ;

    companion object {
        private const val HIGHEST = "🔺"
        private const val LOWEST = "⏬"

        /** Ordered so the longest/most specific markers are tried first. */
        val ALL_EMOJI: List<String> = listOf(HIGHEST, HIGH.emoji, MEDIUM.emoji, LOW.emoji, LOWEST)

        fun fromEmoji(emoji: String): Priority? = when (emoji) {
            HIGHEST, HIGH.emoji -> HIGH
            MEDIUM.emoji -> MEDIUM
            LOW.emoji, LOWEST -> LOW
            else -> null
        }
    }
}
