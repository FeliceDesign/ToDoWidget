package com.felicedesign.todowidget.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.felicedesign.todowidget.data.SettingsRepository
import com.felicedesign.todowidget.data.TodoRepository
import com.felicedesign.todowidget.model.TodoBoard

/**
 * Redraws every placed widget and re-arms the tick alarm. Everything that changes a task goes
 * through here so the two never drift apart.
 *
 * Order matters more than it looks. Signalling the change is what actually redraws a widget whose
 * session is alive, and it is cheap, so it happens first and is never queued behind anything that
 * can block. `updateAll` suspends until Glance has produced new remote views, which can take a
 * while — everything that must happen regardless is done before it, not after.
 */
object WidgetSync {

    suspend fun refresh(context: Context, board: TodoBoard? = null) {
        val app = context.applicationContext
        val repository = TodoRepository(app)

        repository.signalChanged()

        val settings = SettingsRepository(app).current()
        val current = board ?: repository.board()
        WidgetTickScheduler.schedule(app, current, settings)

        // Restarts the session for any widget that no longer has one.
        TodoWidget.updateAll(app)
    }

    /**
     * Everything [refresh] does, plus the framework's own update broadcast, which rebuilds each
     * widget from scratch. Appearance changes need that: they alter the surface the composition is
     * drawn on, which a plain recomposition does not always reapply.
     */
    suspend fun forceRefresh(context: Context) {
        val app = context.applicationContext
        TodoRepository(app).signalChanged()
        broadcastUpdate(app)
        refresh(app)
    }

    private fun broadcastUpdate(app: Context) {
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
