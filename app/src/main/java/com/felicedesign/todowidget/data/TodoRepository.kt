package com.felicedesign.todowidget.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.felicedesign.todowidget.data.markdown.TodoDocument
import com.felicedesign.todowidget.data.markdown.TodoMarkdown
import com.felicedesign.todowidget.data.storage.InternalFileStore
import com.felicedesign.todowidget.data.storage.SafDocumentStore
import com.felicedesign.todowidget.data.storage.StorageUnavailableException
import com.felicedesign.todowidget.data.storage.TodoStore
import com.felicedesign.todowidget.model.Priority
import com.felicedesign.todowidget.model.Settings
import com.felicedesign.todowidget.model.StorageConfig
import com.felicedesign.todowidget.model.StorageMode
import com.felicedesign.todowidget.model.Todo
import com.felicedesign.todowidget.model.TodoBoard
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val Context.widgetStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_state")

/**
 * The single place tasks are read and changed.
 *
 * The Markdown file is the source of truth. A local mirror keeps the widget renderable even when
 * the configured file is briefly unreachable (vault on an SD card, permission revoked), and the
 * moment each task was ticked is kept here rather than in the file so the user's vault does not
 * collect second-precision timestamps.
 */
class TodoRepository(private val context: Context) {

    private val settingsRepository = SettingsRepository(context)
    private val mirror = File(context.filesDir, MIRROR_FILE)

    suspend fun board(now: LocalDateTime = LocalDateTime.now()): TodoBoard {
        val loaded = writeLock.withLock { load(settingsRepository.current()) }
        return buildBoard(loaded, now)
    }

    suspend fun add(
        text: String,
        priority: Priority?,
        dueAt: LocalDateTime?,
        durationMinutes: Long?,
        now: LocalDateTime = LocalDateTime.now(),
    ): TodoBoard {
        val applied = applyChange { document ->
            document.withAdded(
                Todo(
                    id = "",
                    text = text.trim(),
                    priority = priority,
                    createdOn = now.toLocalDate(),
                    dueAt = dueAt,
                    durationMinutes = durationMinutes,
                ),
            )
        }
        return buildBoard(applied, now)
    }

    suspend fun setDone(id: String, done: Boolean, now: LocalDateTime = LocalDateTime.now()): TodoBoard {
        val applied = applyChange { document ->
            val todo = document.find(id) ?: return@applyChange document
            document.withUpdated(
                todo.copy(done = done, completedOn = if (done) now.toLocalDate() else null),
            )
        }
        // Only start the auto-hide clock once the change actually reached the file.
        if (applied.error == null) markCompletion(id, done, now)
        return buildBoard(applied, now)
    }

    suspend fun delete(id: String, now: LocalDateTime = LocalDateTime.now()): TodoBoard {
        val applied = applyChange { document -> document.withRemoved(id) }
        if (applied.error == null) forgetCompletion(id)
        return buildBoard(applied, now)
    }

    /** Brings back the most recently ticked task, as long as it is still inside its window. */
    suspend fun undoLastCompletion(now: LocalDateTime = LocalDateTime.now()): TodoBoard {
        val id = board(now).undoableId ?: return board(now)
        return setDone(id, done = false, now = now)
    }

    suspend fun setShowArchive(show: Boolean) {
        context.widgetStateDataStore.edit { it[SHOW_ARCHIVE] = show }
    }

    suspend fun storageLabel(): String = storeFor(settingsRepository.current().storage).displayName

    /**
     * Rewrites the todo file at a newly chosen location, carrying the current content over so
     * switching to an Obsidian vault does not look like the list was wiped.
     */
    suspend fun migrateTo(config: StorageConfig): Result<Unit> = writeLock.withLock {
        runCatching {
            val existing = load(settingsRepository.current()).document.toText()
            val target = storeFor(config)
            val atTarget = runCatching { target.read() }.getOrDefault("")
            // Never clobber a note that already has content at the new location.
            if (atTarget.isBlank() && existing.isNotBlank()) target.write(existing)
            settingsRepository.setStorage(config)
            withContext(Dispatchers.IO) { mirror.writeText(if (atTarget.isBlank()) existing else atTarget) }
        }
    }

    private suspend fun applyChange(transform: (TodoDocument) -> TodoDocument): Loaded = writeLock.withLock {
        val settings = settingsRepository.current()
        val loaded = load(settings)
        val next = transform(loaded.document)
        if (next == loaded.document) return@withLock loaded

        val store = storeFor(settings.storage)
        try {
            store.write(next.toText())
            withContext(Dispatchers.IO) { mirror.writeText(next.toText()) }
            Loaded(next, null, loaded.label, settings)
        } catch (e: StorageUnavailableException) {
            // Leave the mirror untouched: a half-applied change is worse than a visible error.
            Loaded(loaded.document, e.message ?: "The todo file is unavailable", loaded.label, settings)
        }
    }

    private suspend fun buildBoard(loaded: Loaded, now: LocalDateTime): TodoBoard {
        val completed = pruneCompletions(loaded.document.todos, loaded.settings.autoHideSeconds, now)
        val (done, open) = loaded.document.todos.partition { it.done }
        val stillVisible = done.filter { it.id in completed }

        return TodoBoard(
            active = TodoSorting.active(open + stillVisible, now),
            archived = TodoSorting.archived(done - stillVisible.toSet()),
            completedAt = completed,
            showArchive = context.widgetStateDataStore.data.first()[SHOW_ARCHIVE] ?: false,
            error = loaded.error,
            storageLabel = loaded.label,
        )
    }

    private class Loaded(
        val document: TodoDocument,
        val error: String?,
        val label: String,
        val settings: Settings,
    )

    private suspend fun load(settings: Settings): Loaded {
        val store = storeFor(settings.storage)
        return try {
            val text = store.read()
            withContext(Dispatchers.IO) { mirror.writeText(text) }
            Loaded(TodoMarkdown.parse(text), null, store.displayName, settings)
        } catch (e: StorageUnavailableException) {
            val fallback = withContext(Dispatchers.IO) { if (mirror.exists()) mirror.readText() else "" }
            Loaded(TodoMarkdown.parse(fallback), e.message, store.displayName, settings)
        }
    }

    private fun storeFor(config: StorageConfig): TodoStore = when (config.mode) {
        StorageMode.INTERNAL -> InternalFileStore(context)
        StorageMode.DOCUMENT -> SafDocumentStore(
            context = context,
            documentUri = config.documentUri?.let(Uri::parse),
            treeUri = config.treeUri?.let(Uri::parse),
            fileName = config.fileName,
        )
    }

    private suspend fun markCompletion(id: String, done: Boolean, now: LocalDateTime) {
        context.widgetStateDataStore.edit { prefs ->
            val entries = prefs[COMPLETED_AT].orEmpty().filterNot { it.substringBefore(SEPARATOR) == id }
            prefs[COMPLETED_AT] = if (done) {
                entries.toSet() + "$id$SEPARATOR${now.toEpochMillis()}"
            } else {
                entries.toSet()
            }
        }
    }

    private suspend fun forgetCompletion(id: String) {
        context.widgetStateDataStore.edit { prefs ->
            prefs[COMPLETED_AT] = prefs[COMPLETED_AT].orEmpty()
                .filterNot { it.substringBefore(SEPARATOR) == id }
                .toSet()
        }
    }

    /** Drops window entries that have expired or whose task no longer exists. */
    private suspend fun pruneCompletions(
        todos: List<Todo>,
        autoHideSeconds: Int,
        now: LocalDateTime,
    ): Map<String, Long> {
        val cutoff = now.toEpochMillis() - autoHideSeconds * 1_000L
        val knownIds = todos.mapTo(mutableSetOf()) { it.id }
        var live = emptyMap<String, Long>()

        context.widgetStateDataStore.edit { prefs ->
            live = prefs[COMPLETED_AT].orEmpty()
                .mapNotNull { entry ->
                    val id = entry.substringBefore(SEPARATOR)
                    val at = entry.substringAfter(SEPARATOR, "").toLongOrNull() ?: return@mapNotNull null
                    if (at > cutoff && id in knownIds) id to at else null
                }
                .toMap()
            prefs[COMPLETED_AT] = live.map { (id, at) -> "$id$SEPARATOR$at" }.toSet()
        }
        return live
    }

    private companion object {
        /**
         * Shared across instances on purpose: a widget tap, the add sheet and the settings screen
         * each build their own repository, and two of them writing at once would lose an edit.
         */
        val writeLock = Mutex()

        const val MIRROR_FILE = "mirror.md"
        const val SEPARATOR = "|"
        val COMPLETED_AT = stringSetPreferencesKey("completed_at")
        val SHOW_ARCHIVE = booleanPreferencesKey("show_archive")
    }
}

internal fun LocalDateTime.toEpochMillis(): Long =
    atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

internal fun Long.toLocalDateTime(): LocalDateTime =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()

internal fun LocalDate.atEndOfDay(): LocalDateTime = atTime(TodoMarkdown.DEFAULT_DUE_TIME)
