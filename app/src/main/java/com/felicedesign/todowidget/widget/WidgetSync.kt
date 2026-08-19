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
        val settings = SettingsRepository(app).current()
        val current = board ?: TodoRepository(app).board()

        WidgetTickScheduler.schedule(app, current, settings)
        TodoWidget.updateAll(app)
    }
}
