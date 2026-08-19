package com.felicedesign.todowidget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the scheduled tick (and the boot broadcast) and redraws the widget. Redrawing also
 * re-arms the next tick, so the chain keeps itself going for as long as anything is counting down.
 */
class TodoTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                WidgetSync.refresh(app)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TICK = "com.felicedesign.todowidget.action.TICK"
    }
}
