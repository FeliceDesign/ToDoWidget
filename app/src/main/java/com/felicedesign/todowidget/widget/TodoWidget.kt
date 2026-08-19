package com.felicedesign.todowidget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.felicedesign.todowidget.data.SettingsRepository
import com.felicedesign.todowidget.data.TodoRepository
import com.felicedesign.todowidget.widget.ui.TodoWidgetContent
import java.time.LocalDateTime

/**
 * The home-screen widget. Everything it draws comes from the Markdown file and the user's colour
 * settings; every tap either changes that file or opens one of the two small overlay screens.
 */
object TodoWidget : GlanceAppWidget() {

    // Exact sizing lets the layout react to the real widget size rather than a handful of buckets.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsRepository(context).current()
        val board = TodoRepository(context).board()

        // Every refresh — including the periodic one and the one after a reboot — re-arms the tick.
        WidgetTickScheduler.schedule(context, board, settings)

        provideContent {
            TodoWidgetContent(board = board, settings = settings, now = LocalDateTime.now())
        }
    }
}
