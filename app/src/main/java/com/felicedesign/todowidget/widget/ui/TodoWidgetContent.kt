package com.felicedesign.todowidget.widget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.felicedesign.todowidget.model.Appearance
import com.felicedesign.todowidget.model.BarSide
import com.felicedesign.todowidget.model.Settings
import com.felicedesign.todowidget.model.Todo
import com.felicedesign.todowidget.model.TodoBoard
import com.felicedesign.todowidget.ui.add.AddTodoActivity
import com.felicedesign.todowidget.ui.settings.SettingsActivity
import com.felicedesign.todowidget.ui.theme.isDark
import com.felicedesign.todowidget.ui.theme.scaleAlpha
import com.felicedesign.todowidget.util.Countdown
import com.felicedesign.todowidget.widget.WidgetIcons
import com.felicedesign.todowidget.widget.action.RefreshAction
import com.felicedesign.todowidget.widget.action.ToggleArchiveAction
import com.felicedesign.todowidget.widget.action.ToggleTodoAction
import com.felicedesign.todowidget.widget.action.UndoAction
import com.felicedesign.todowidget.widget.action.WidgetParams
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Below this width the remaining-time chip is dropped so task text keeps room to breathe. */
private val COMPACT_WIDTH = 200.dp
private val ARCHIVE_DATE = DateTimeFormatter.ofPattern("d MMM")

@Composable
fun TodoWidgetContent(board: TodoBoard, settings: Settings, now: LocalDateTime) {
    val appearance = settings.appearance
    val size = LocalSize.current
    val showTime = size.width >= COMPACT_WIDTH

    // Alpha 0 means the user asked for a fully transparent widget. Rounding the corners has to go
    // with the background: a corner radius needs something to clip, so on its own it is enough to
    // put an opaque surface back underneath the list.
    val transparent = (appearance.background ushr 24) == 0
    val surface = if (transparent) {
        // Explicitly transparent rather than simply unset: the widget is inflated with the app's
        // own theme, so leaving the background alone lets that theme's colour show through.
        GlanceModifier.fillMaxSize().background(Color.Transparent)
    } else {
        GlanceModifier.fillMaxSize().background(Color(appearance.background)).cornerRadius(16.dp)
    }

    // The coloured surface is deliberately a child rather than the root. A recomposition reliably
    // reapplies attributes on nested views — the add button's colour always followed the setting —
    // while the root container's background did not, so a new background only appeared on the next
    // full rebuild.
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Column(modifier = surface.padding(vertical = 6.dp)) {
            board.error?.let { ErrorBanner(it, appearance.highPriority) }

            val items = board.visible
            if (items.isEmpty()) {
                EmptyState(board.showArchive, appearance.text, GlanceModifier.defaultWeight())
            } else {
                LazyColumn(modifier = GlanceModifier.defaultWeight()) {
                    items(items, itemId = { it.id.hashCode().toLong() }) { todo ->
                        TodoRow(todo, board, settings, now, showTime)
                    }
                }
            }

            BottomBar(board, settings)
        }
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    board: TodoBoard,
    settings: Settings,
    now: LocalDateTime,
    showTime: Boolean,
) {
    val appearance = settings.appearance
    val justCompleted = todo.id in board.completedAt
    val accent = if (todo.done) appearance.highlight else appearance.colorFor(todo.priority)
    val textColor = if (todo.done) appearance.text.scaleAlpha(0.45f) else appearance.text

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable(
                actionRunCallback<ToggleTodoAction>(
                    actionParametersOf(
                        WidgetParams.todoId to todo.id,
                        WidgetParams.done to !todo.done,
                    ),
                ),
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(
                if (todo.done) WidgetIcons.CHECKBOX_CHECKED else WidgetIcons.CHECKBOX_UNCHECKED,
            ),
            contentDescription = if (todo.done) "Completed" else "Not done",
            colorFilter = ColorFilter.tint(ColorProvider(Color(accent))),
            modifier = GlanceModifier.size(20.dp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = todo.text,
            maxLines = 2,
            style = TextStyle(
                color = ColorProvider(Color(textColor)),
                fontSize = 14.sp,
                textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )

        val trailing = trailingLabel(todo, board.showArchive, justCompleted, now)
        if (showTime && trailing != null) {
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = trailing,
                style = TextStyle(
                    color = ColorProvider(Color(trailingColor(todo, settings, now))),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

private fun trailingLabel(
    todo: Todo,
    showingArchive: Boolean,
    justCompleted: Boolean,
    now: LocalDateTime,
): String? = when {
    showingArchive -> todo.completedOn?.format(ARCHIVE_DATE)
    justCompleted -> "done"
    todo.dueAt != null -> Countdown.format(todo.dueAt, now)
    else -> null
}

private fun trailingColor(todo: Todo, settings: Settings, now: LocalDateTime): Int = when {
    todo.done -> settings.appearance.text.scaleAlpha(0.45f)
    todo.dueAt != null && Countdown.isOverdue(todo.dueAt, now) -> settings.appearance.highPriority
    else -> settings.appearance.text.scaleAlpha(0.6f)
}

@Composable
private fun EmptyState(showingArchive: Boolean, textColor: Int, modifier: GlanceModifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = if (showingArchive) "Nothing completed yet" else "All clear — tap + to add a task",
            style = TextStyle(
                color = ColorProvider(Color(textColor.scaleAlpha(0.55f))),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            ),
            modifier = GlanceModifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun ErrorBanner(message: String, color: Int) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .clickable(actionRunCallback<RefreshAction>()),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(WidgetIcons.WARNING),
            contentDescription = "Storage problem",
            colorFilter = ColorFilter.tint(ColorProvider(Color(color))),
            modifier = GlanceModifier.size(14.dp),
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = message,
            maxLines = 2,
            style = TextStyle(color = ColorProvider(Color(color)), fontSize = 11.sp),
        )
    }
}

@Composable
private fun BottomBar(board: TodoBoard, settings: Settings) {
    val appearance = settings.appearance
    val onLeft = settings.addButtonSide == BarSide.LEFT

    // Kept as one list so the two layouts stay mirror images: whichever button sits closest to the
    // add button on one side sits closest to it on the other.
    val secondary = buildList<@Composable () -> Unit> {
        add {
            BarButton(
                icon = if (board.showArchive) WidgetIcons.BACK else WidgetIcons.ARCHIVE,
                description = if (board.showArchive) "Back to open tasks" else "Show completed tasks",
                tint = appearance.text.scaleAlpha(0.75f),
                action = actionRunCallback<ToggleArchiveAction>(
                    actionParametersOf(WidgetParams.showArchive to !board.showArchive),
                ),
            )
        }
        add {
            BarButton(
                icon = WidgetIcons.SETTINGS,
                description = "Settings",
                tint = appearance.text.scaleAlpha(0.75f),
                action = actionStartActivity<SettingsActivity>(),
            )
        }
        if (board.undoableId != null && !board.showArchive) {
            add {
                BarButton(
                    icon = WidgetIcons.UNDO,
                    description = "Undo",
                    tint = appearance.highlight,
                    action = actionRunCallback<UndoAction>(),
                )
            }
        }
    }

    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        if (onLeft) {
            AddButton(appearance)
            secondary.asReversed().forEach { it() }
            Spacer(GlanceModifier.defaultWeight())
        } else {
            Spacer(GlanceModifier.defaultWeight())
            secondary.forEach { it() }
            AddButton(appearance)
        }
    }
}

@Composable
private fun AddButton(appearance: Appearance) {
    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .cornerRadius(20.dp)
            .background(Color(appearance.highlight))
            .clickable(actionStartActivity<AddTodoActivity>()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(WidgetIcons.ADD),
            contentDescription = "Add a task",
            colorFilter = ColorFilter.tint(
                ColorProvider(if (appearance.highlight.isDark()) Color.White else Color.Black),
            ),
            modifier = GlanceModifier.size(22.dp),
        )
    }
}

@Composable
private fun BarButton(
    icon: Int,
    description: String,
    tint: Int,
    action: androidx.glance.action.Action,
) {
    Box(
        modifier = GlanceModifier.size(36.dp).cornerRadius(18.dp).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(ColorProvider(Color(tint))),
            modifier = GlanceModifier.size(18.dp),
        )
    }
}
