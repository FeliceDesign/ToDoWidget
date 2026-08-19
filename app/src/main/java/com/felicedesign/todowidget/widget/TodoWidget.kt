package com.felicedesign.todowidget.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
 *
 * The data is collected *inside* the composition rather than loaded up front. Glance keeps a
 * session alive and recomposes it, but it does not call [provideGlance] again, so anything read
 * before [provideContent] would stay frozen at the value it had when the widget was first placed —
 * which is exactly how ticking a task off ends up looking like nothing happened.
 */
object TodoWidget : GlanceAppWidget() {

    // Exact sizing lets the layout react to the real widget size rather than a handful of buckets.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = TodoRepository(context)
        val settingsRepository = SettingsRepository(context)

        val boards = repository.boardFlow()
        val settingsUpdates = settingsRepository.settings

        // Seeded with real values so the first frame is the list, never an empty flash.
        val initialSettings = settingsRepository.current()
        val initialBoard = repository.board()
        WidgetTickScheduler.schedule(context, initialBoard, initialSettings)

        provideContent {
            val settings by settingsUpdates.collectAsState(initial = initialSettings)
            val board by boards.collectAsState(initial = initialBoard)

            // Read per recomposition, so countdowns are right again after every scheduled tick.
            TodoWidgetContent(board = board, settings = settings, now = LocalDateTime.now())
        }
    }
}
