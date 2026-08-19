package com.felicedesign.todowidget.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.felicedesign.todowidget.data.TodoRepository
import com.felicedesign.todowidget.widget.WidgetSync
import kotlinx.coroutines.CancellationException

internal object WidgetParams {
    val todoId = ActionParameters.Key<String>("todo_id")
    val done = ActionParameters.Key<Boolean>("done")
    val showArchive = ActionParameters.Key<Boolean>("show_archive")
}

/**
 * Runs a widget tap and makes sure the outcome is visible either way.
 *
 * A callback that throws leaves the widget looking untouched, which is indistinguishable from a
 * tap that never registered — so failures are written where the error banner will show them.
 */
private suspend fun handleTap(context: Context, block: suspend (TodoRepository) -> Unit) {
    val repository = TodoRepository(context)
    try {
        block(repository)
        repository.setActionError(null)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        repository.setActionError(e.message ?: e.javaClass.simpleName)
        WidgetSync.refresh(context)
    }
}

/** Ticks a task off (or back on, from the archive) and redraws. */
class ToggleTodoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        handleTap(context) { repository ->
            val id = parameters[WidgetParams.todoId] ?: return@handleTap
            val done = parameters[WidgetParams.done] ?: true
            WidgetSync.refresh(context, repository.setDone(id, done))
        }
    }
}

/** Restores the task that was ticked most recently, while it is still on screen. */
class UndoAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        handleTap(context) { repository ->
            WidgetSync.refresh(context, repository.undoLastCompletion())
        }
    }
}

/** Flips the widget between the open list and the archive, without opening the app. */
class ToggleArchiveAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        handleTap(context) { repository ->
            repository.setShowArchive(parameters[WidgetParams.showArchive] ?: false)
            WidgetSync.refresh(context, repository.board())
        }
    }
}

/** Re-reads the file — used by the manual refresh tap on the error banner. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        handleTap(context) { WidgetSync.refresh(context) }
    }
}
