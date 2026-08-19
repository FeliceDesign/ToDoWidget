package com.felicedesign.todowidget.ui.add

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.felicedesign.todowidget.TodoApp
import com.felicedesign.todowidget.data.SettingsRepository
import com.felicedesign.todowidget.data.TodoRepository
import com.felicedesign.todowidget.model.Priority
import com.felicedesign.todowidget.model.Settings
import com.felicedesign.todowidget.ui.theme.AppTheme
import com.felicedesign.todowidget.widget.WidgetSync
import java.time.LocalDateTime
import kotlinx.coroutines.launch

/**
 * The add button's destination: a sheet that floats over the home screen rather than a full app
 * screen. Android has no editable text inside a widget, so this is as close to "in the widget" as
 * the platform allows — it opens straight onto the launcher and closes the moment you save.
 */
class AddTodoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The sheet draws edge to edge and pads itself, which is the only way it can clear both
        // the navigation bar and the keyboard on a see-through window.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val settingsRepository = SettingsRepository(this)

        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = Settings())
            AppTheme(settings.appearance) {
                AddTodoSheet(
                    settings = settings,
                    onDismiss = ::finish,
                    onSave = ::save,
                )
            }
        }
    }

    private fun save(text: String, priority: Priority?, dueAt: LocalDateTime?, durationMinutes: Long?) {
        val app = applicationContext
        // Finish immediately so the sheet does not linger; the write runs on the app scope so it
        // survives this activity being torn down.
        finish()
        TodoApp.scopeOf(app).launch {
            val board = TodoRepository(app).add(
                text = text,
                priority = priority,
                dueAt = dueAt,
                durationMinutes = durationMinutes,
            )
            WidgetSync.refresh(app, board)
        }
    }
}
