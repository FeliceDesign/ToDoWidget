package com.felicedesign.todowidget.data.storage

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A Markdown file the user picked through the system document picker.
 *
 * Two shapes are supported: a single `.md` document, or a folder (an Obsidian vault) plus a file
 * name inside it — the file is created on first write if it is not there yet. Access relies on a
 * persisted URI permission, so no storage permission is ever requested.
 */
class SafDocumentStore(
    private val context: Context,
    private val documentUri: Uri?,
    private val treeUri: Uri?,
    private val fileName: String,
) : TodoStore {

    override val displayName: String
        get() = when {
            documentUri != null -> DocumentFile.fromSingleUri(context, documentUri)?.name ?: documentUri.lastPathSegment.orEmpty()
            treeUri != null -> {
                val folder = DocumentFile.fromTreeUri(context, treeUri)?.name ?: treeUri.lastPathSegment.orEmpty()
                "$folder / $fileName"
            }
            else -> fileName
        }

    override suspend fun read(): String = withContext(Dispatchers.IO) {
        val file = resolve(createIfMissing = false) ?: return@withContext ""
        if (!file.exists()) return@withContext ""
        try {
            context.contentResolver.openInputStream(file.uri)?.use { it.readBytes().decodeToString() }
                ?: throw StorageUnavailableException("Could not open ${file.uri} for reading")
        } catch (e: SecurityException) {
            throw StorageUnavailableException("Access to the selected file was revoked", e)
        } catch (e: IOException) {
            throw StorageUnavailableException("Could not read the selected file", e)
        }
    }

    override suspend fun write(text: String) = withContext(Dispatchers.IO) {
        val file = resolve(createIfMissing = true)
            ?: throw StorageUnavailableException("The selected location is no longer available")
        try {
            // "wt" truncates, so a shorter list does not leave stale bytes behind.
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { it.write(text.toByteArray()) }
                ?: throw StorageUnavailableException("Could not open ${file.uri} for writing")
        } catch (e: SecurityException) {
            throw StorageUnavailableException("Access to the selected file was revoked", e)
        } catch (e: IOException) {
            throw StorageUnavailableException("Could not write to the selected file", e)
        }
    }

    override suspend fun lastModified(): Long = withContext(Dispatchers.IO) {
        resolve(createIfMissing = false)?.lastModified() ?: 0L
    }

    private fun resolve(createIfMissing: Boolean): DocumentFile? {
        documentUri?.let { return DocumentFile.fromSingleUri(context, it) }
        val tree = treeUri?.let { DocumentFile.fromTreeUri(context, it) } ?: return null
        if (!tree.canRead()) throw StorageUnavailableException("Access to the selected folder was revoked")
        tree.findFile(fileName)?.let { return it }
        if (!createIfMissing) return null
        return tree.createFile(MIME_MARKDOWN, fileName.removeSuffix(EXTENSION))
            ?: throw StorageUnavailableException("Could not create $fileName in the selected folder")
    }

    private companion object {
        const val MIME_MARKDOWN = "text/markdown"
        const val EXTENSION = ".md"
    }
}
