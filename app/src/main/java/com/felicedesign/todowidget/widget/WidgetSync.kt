package com.felicedesign.todowidget.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.felicedesign.todowidget.data.SettingsRepository
import com.felicedesign.todowidget.data.TodoRepository
import com.felicedesign.todowidget.model.TodoBoard

/**
 * Redraws every placed widget and re-arms the tick alarm. Everything that changes a task goes
 * through here so the two never drift apart.
 */
object WidgetSync {

    suspend fun refresh(context: Context, board: TodoBoard? = null) {
        val app = context.applicationContext
        val repository = TodoRepository(app)
        val settings = SettingsRepository(app).current()
        val current = board ?: repository.board()

        WidgetTickScheduler.schedule(app, current, settings)

        // Nudges the flow a live widget is collecting…
        repository.signalChanged()
        // …and restarts the session for any widget that has none.
        TodoWidget.updateAll(app)
    }
}
