package com.felicedesign.todowidget.ui.add

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.felicedesign.todowidget.data.markdown.TodoMarkdown
import com.felicedesign.todowidget.model.Priority
import com.felicedesign.todowidget.model.Settings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class DeadlineMode { NONE, DURATION, DEADLINE }

private val DURATION_PRESETS = listOf(30L, 60L, 120L, 240L, 1440L, 4320L, 10080L)
private val DATE_LABEL = DateTimeFormatter.ofPattern("d MMM")
private val TIME_LABEL = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun AddTodoSheet(
    settings: Settings,
    onDismiss: () -> Unit,
    onSave: (text: String, priority: Priority?, dueAt: LocalDateTime?, durationMinutes: Long?) -> Unit,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf<Priority?>(null) }
    var mode by remember {
        mutableStateOf(if (settings.defaultDurationMinutes != null) DeadlineMode.DURATION else DeadlineMode.NONE)
    }
    var durationMinutes by remember { mutableStateOf(settings.defaultDurationMinutes ?: 60L) }
    var deadlineDate by remember { mutableStateOf(LocalDate.now()) }
    var deadlineTime by remember { mutableStateOf<LocalTime?>(null) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Tapping the dimmed area behind the sheet closes it, like a real bottom sheet.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Order matters: the keyboard lifts the whole sheet, the surface still runs to the
                // bottom edge, and only the content is inset above the navigation bar.
                .imePadding()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New task") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )

            SectionLabel("Priority")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                ChoiceChip("None", priority == null) { priority = null }
                Priority.entries.forEach { level ->
                    ChoiceChip(
                        label = level.label(),
                        selected = priority == level,
                        // A dot in the configured colour, so the chip still says which colour this
                        // level will get in the widget without tinting the chip itself.
                        dot = Color(settings.appearance.colorFor(level)),
                    ) { priority = level }
                }
            }

            SectionLabel("When")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("Anytime", mode == DeadlineMode.NONE) { mode = DeadlineMode.NONE }
                ChoiceChip("Within", mode == DeadlineMode.DURATION) { mode = DeadlineMode.DURATION }
                ChoiceChip("By", mode == DeadlineMode.DEADLINE) { mode = DeadlineMode.DEADLINE }
            }

            when (mode) {
                DeadlineMode.NONE -> Unit

                DeadlineMode.DURATION -> {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    ) {
                        DURATION_PRESETS.forEach { minutes ->
                            ChoiceChip(
                                label = TodoMarkdown.formatDuration(minutes),
                                selected = durationMinutes == minutes,
                            ) { durationMinutes = minutes }
                        }
                    }
                }

                DeadlineMode.DEADLINE -> {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    deadlineDate = LocalDate.of(year, month + 1, day)
                                },
                                deadlineDate.year,
                                deadlineDate.monthValue - 1,
                                deadlineDate.dayOfMonth,
                            ).show()
                        }) { Text(deadlineDate.format(DATE_LABEL)) }

                        TextButton(onClick = {
                            val start = deadlineTime ?: LocalTime.of(18, 0)
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> deadlineTime = LocalTime.of(hour, minute) },
                                start.hour,
                                start.minute,
                                true,
                            ).show()
                        }) { Text(deadlineTime?.format(TIME_LABEL) ?: "End of day") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val now = LocalDateTime.now()
                        when (mode) {
                            DeadlineMode.NONE -> onSave(text, priority, null, null)
                            DeadlineMode.DURATION ->
                                onSave(text, priority, now.plusMinutes(durationMinutes), durationMinutes)
                            DeadlineMode.DEADLINE -> onSave(
                                text,
                                priority,
                                deadlineDate.atTime(deadlineTime ?: TodoMarkdown.DEFAULT_DUE_TIME),
                                null,
                            )
                        }
                    },
                    enabled = text.isNotBlank(),
                ) { Text("Add") }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    dot: Color? = null,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dot != null) {
                    Box(modifier = Modifier.size(10.dp).background(dot, CircleShape))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label)
            }
        },
    )
}

private fun Priority.label(): String = when (this) {
    Priority.HIGH -> "High"
    Priority.MEDIUM -> "Medium"
    Priority.LOW -> "Low"
}
