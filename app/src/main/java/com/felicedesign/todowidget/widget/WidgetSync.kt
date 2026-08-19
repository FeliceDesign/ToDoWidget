package com.felicedesign.todowidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.felicedesign.todowidget.data.SettingsRepository
import com.felicedesign.todowidget.data.TodoRepository
import com.felicedesign.todowidget.model.TodoBoard
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Redraws every placed widget and re-arms the tick alarm. Everything that changes a task goes
 * through here so the two never drift apart.
 */
object WidgetSync {

    // Rapid changes — dragging through colours, ticking several tasks — would otherwise race each
    // other and let an older redraw land last.
    private val lock = Mutex()

    suspend fun refresh(context: Context, board: TodoBoard? = null) = lock.withLock {
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

    /**
     * Everything [refresh] does, plus the framework's own update broadcast.
     *
     * Appearance changes only alter attributes of views the composition already emitted, which is
     * where a redraw is most easily skipped. Going through the AppWidgetManager makes the host
     * rebuild each widget from scratch, so a new colour always lands.
     */
    suspend fun forceRefresh(context: Context) {
        refresh(context)

        val app = context.applicationContext
        val ids = AppWidgetManager.getInstance(app)
            .getAppWidgetIds(ComponentName(app, TodoWidgetReceiver::class.java))
        if (ids.isEmpty()) return

        app.sendBroadcast(
            Intent(app, TodoWidgetReceiver::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
        )
    }
}
