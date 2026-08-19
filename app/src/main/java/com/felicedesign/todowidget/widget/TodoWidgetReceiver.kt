package com.felicedesign.todowidget.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = TodoWidget

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Nothing left on the home screen, so stop waking up for countdowns.
        WidgetTickScheduler.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val remaining = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(android.content.ComponentName(context, TodoWidgetReceiver::class.java))
        if (remaining.isEmpty()) WidgetTickScheduler.cancel(context)
    }
}
