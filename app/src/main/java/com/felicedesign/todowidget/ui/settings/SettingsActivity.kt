package com.felicedesign.todowidget.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.felicedesign.todowidget.TodoApp
import com.felicedesign.todowidget.data.SettingsRepository
import com.felicedesign.todowidget.data.TodoRepository
import com.felicedesign.todowidget.model.Appearance
import com.felicedesign.todowidget.model.BarSide
import com.felicedesign.todowidget.model.Settings
import com.felicedesign.todowidget.model.StorageConfig
import com.felicedesign.todowidget.model.StorageMode
import com.felicedesign.todowidget.ui.theme.AppTheme
import com.felicedesign.todowidget.widget.WidgetSync
import kotlinx.coroutines.launch

/**
 * The widget's settings button lands here: colours, how long a ticked task lingers, and which
 * Markdown file — optionally one inside an Obsidian vault — the list is stored in.
 */
class SettingsActivity : ComponentActivity() {

    private val settingsRepository by lazy { SettingsRepository(this) }
    private val todoRepository by lazy { TodoRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = Settings())
            AppTheme {
                SettingsScreen(
                    settings = settings,
                    onAppearanceChange = ::updateAppearance,
                    onAutoHideChange = ::updateAutoHide,
                    onAddButtonSideChange = ::updateAddButtonSide,
                    onFileNameChange = ::updateFileName,
                    onUseInternalStorage = { migrate(StorageConfig()) },
                    onVaultPicked = ::useVaultFolder,
                    onFilePicked = ::useSingleFile,
                    onClose = ::finish,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // A widget host that is off screen may defer updates, so push a final one exactly as the
        // user returns to the home screen.
        inBackground { WidgetSync.forceRefresh(applicationContext) }
    }

    private fun updateAppearance(transform: (Appearance) -> Appearance) = inBackground {
        settingsRepository.updateAppearance(transform)
        // Forced rather than a plain refresh: a colour change only alters attributes of views that
        // are already on screen, which is exactly the case a host is happy to skip.
        WidgetSync.forceRefresh(applicationContext)
    }

    private fun updateAddButtonSide(side: BarSide) = inBackground {
        settingsRepository.setAddButtonSide(side)
        WidgetSync.forceRefresh(applicationContext)
    }

    private fun updateAutoHide(seconds: Int) = inBackground {
        settingsRepository.setAutoHideSeconds(seconds)
        WidgetSync.forceRefresh(applicationContext)
    }

    private fun updateFileName(name: String) = inBackground {
        val current = settingsRepository.current().storage
        if (current.treeUri == null) return@inBackground
        migrateNow(current.copy(fileName = name.ensureMarkdownExtension()))
    }

    private fun useVaultFolder(uri: Uri) {
        persist(uri)
        inBackground {
            val fileName = settingsRepository.current().storage.fileName
            migrateNow(
                StorageConfig(
                    mode = StorageMode.DOCUMENT,
                    documentUri = null,
                    treeUri = uri.toString(),
                    fileName = fileName,
                ),
            )
        }
    }

    private fun useSingleFile(uri: Uri) {
        persist(uri)
        inBackground {
            migrateNow(
                StorageConfig(
                    mode = StorageMode.DOCUMENT,
                    documentUri = uri.toString(),
                    treeUri = null,
                    fileName = settingsRepository.current().storage.fileName,
                ),
            )
        }
    }

    private fun migrate(config: StorageConfig) = inBackground { migrateNow(config) }

    private suspend fun migrateNow(config: StorageConfig) {
        todoRepository.migrateTo(config)
        WidgetSync.forceRefresh(applicationContext)
    }

    /** Keeps access to the picked file or folder across reboots. */
    private fun persist(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
    }

    private fun inBackground(block: suspend () -> Unit) {
        TodoApp.scopeOf(applicationContext).launch { block() }
    }
}

private fun String.ensureMarkdownExtension(): String {
    val trimmed = trim().ifBlank { StorageConfig.DEFAULT_FILE_NAME }
    return if (trimmed.endsWith(".md", ignoreCase = true)) trimmed else "$trimmed.md"
}
