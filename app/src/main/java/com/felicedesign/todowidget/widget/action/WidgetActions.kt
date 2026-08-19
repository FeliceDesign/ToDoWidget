package com.felicedesign.todowidget.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.felicedesign.todowidget.data.TodoRepository
import com.felicedesign.todowidget.widget.WidgetSync

internal object WidgetParams {
    val todoId = ActionParameters.Key<String>("todo_id")
    val done = ActionParameters.Key<Boolean>("done")
    val showArchive = ActionParameters.Key<Boolean>("show_archive")
}

/** Ticks a task off (or back on, from the archive) and redraws. */
class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[WidgetParams.todoId] ?: return
        val done = parameters[WidgetParams.done] ?: true
        val board = TodoRepository(context).setDone(id, done)
        WidgetSync.refresh(context, board)
    }
}

/** Restores the task that was ticked most recently, while it is still on screen. */
class UndoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val board = TodoRepository(context).undoLastCompletion()
        WidgetSync.refresh(context, board)
    }
}

/** Flips the widget between the open list and the archive, without opening the app. */
class ToggleArchiveAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val repository = TodoRepository(context)
        repository.setShowArchive(parameters[WidgetParams.showArchive] ?: false)
        WidgetSync.refresh(context, repository.board())
    }
}

/** Re-reads the file — used by the manual refresh tap on the error banner. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetSync.refresh(context)
    }
}
