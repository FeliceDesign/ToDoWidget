package com.felicedesign.todowidget.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.felicedesign.todowidget.data.toEpochMillis
import com.felicedesign.todowidget.data.toLocalDateTime
import com.felicedesign.todowidget.model.Settings
import com.felicedesign.todowidget.model.TodoBoard
import com.felicedesign.todowidget.util.Countdown
import java.time.LocalDateTime

/**
 * Wakes the widget exactly when something on screen would change, and not a moment sooner.
 *
 * Two things move on their own: a ticked task disappearing when its auto-hide window ends, and a
 * countdown rolling over to the next unit. Both are predictable, so instead of polling we work out
 * the earliest of those instants and set a single alarm for it.
 */
object WidgetTickScheduler {

    /** Inexact alarms need no special permission; a one-second window is close enough on screen. */
    private const val WINDOW_MILLIS = 1_000L
    private const val REQUEST_CODE = 4711

    fun schedule(context: Context, board: TodoBoard, settings: Settings) {
        val now = LocalDateTime.now()
        val next = nextWakeUp(board, settings, now)
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return

        if (next == null) {
            alarms.cancel(pendingIntent(context))
            return
        }
        alarms.setWindow(
            AlarmManager.RTC,
            next.toEpochMillis(),
            WINDOW_MILLIS,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context))
    }

    /** The first future instant at which the rendered widget would look different. */
    fun nextWakeUp(board: TodoBoard, settings: Settings, now: LocalDateTime): LocalDateTime? {
        val hideDeadlines = board.completedAt.values.map { completedAt ->
            (completedAt + settings.autoHideSeconds * 1_000L).toLocalDateTime()
        }
        val countdownFlips = board.active
            .mapNotNull { it.dueAt }
            .map { Countdown.nextChange(it, now) }

        return (hideDeadlines + countdownFlips).filter { it.isAfter(now) }.minOrNull()
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, TodoTickReceiver::class.java).setAction(TodoTickReceiver.ACTION_TICK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
