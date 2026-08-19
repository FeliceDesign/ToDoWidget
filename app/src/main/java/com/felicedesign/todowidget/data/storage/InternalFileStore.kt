package com.felicedesign.todowidget.data.storage

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The default location: a Markdown file in the app's own storage. Needs no permissions. */
class InternalFileStore(context: Context) : TodoStore {

    private val file = File(context.filesDir, FILE_NAME)

    override val displayName: String get() = "App storage · $FILE_NAME"

    override suspend fun read(): String = withContext(Dispatchers.IO) {
        if (file.exists()) file.readText() else ""
    }

    override suspend fun write(text: String) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "$FILE_NAME.tmp")
        temp.writeText(text)
        if (!temp.renameTo(file)) {
            file.writeText(text)
            temp.delete()
        }
    }

    override suspend fun lastModified(): Long = withContext(Dispatchers.IO) {
        if (file.exists()) file.lastModified() else 0L
    }

    companion object {
        const val FILE_NAME = "todos.md"
    }
}
