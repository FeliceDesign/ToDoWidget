package com.felicedesign.todowidget

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Holds a scope that outlives the overlay screens. Saving a task has to finish even though the add
 * sheet closes the instant you tap Add, so that work must not hang off an activity's lifecycle.
 */
class TodoApp : Application() {

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        fun scopeOf(context: Context): CoroutineScope =
            (context.applicationContext as TodoApp).scope
    }
}
