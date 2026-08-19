package com.felicedesign.todowidget.data.storage

/**
 * Where the Markdown lives. Two implementations: a private file inside the app, and a document
 * the user picked with the system file picker (typically a note in an Obsidian vault).
 */
interface TodoStore {

    /** Human-readable location, shown in settings. */
    val displayName: String

    suspend fun read(): String

    suspend fun write(text: String)

    /** Millis of the last external modification, used to notice edits made outside the app. */
    suspend fun lastModified(): Long
}

/** The configured location could not be read or written — usually a revoked permission. */
class StorageUnavailableException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
