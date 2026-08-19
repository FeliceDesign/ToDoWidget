package com.felicedesign.todowidget.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.felicedesign.todowidget.model.Appearance
import com.felicedesign.todowidget.model.BarSide
import com.felicedesign.todowidget.model.Settings
import com.felicedesign.todowidget.model.StorageMode

private data class ColorSlot(
    val title: String,
    val description: String,
    val value: (Appearance) -> Int,
    val apply: (Appearance, Int) -> Appearance,
    val allowTransparency: Boolean = false,
)

private val COLOR_SLOTS = listOf(
    ColorSlot(
        "Highlight",
        "Add button, undo and completed ticks",
        { it.highlight },
        { a, c -> a.copy(highlight = c) },
    ),
    ColorSlot(
        "Background",
        "Slide opacity to zero for a fully transparent widget",
        { it.background },
        { a, c -> a.copy(background = c) },
        allowTransparency = true,
    ),
    ColorSlot("Text", "Task titles and labels", { it.text }, { a, c -> a.copy(text = c) }),
    ColorSlot("High priority", "⏫", { it.highPriority }, { a, c -> a.copy(highPriority = c) }),
    ColorSlot("Medium priority", "🔼", { it.mediumPriority }, { a, c -> a.copy(mediumPriority = c) }),
    ColorSlot("Low priority", "🔽", { it.lowPriority }, { a, c -> a.copy(lowPriority = c) }),
)

private val PRESETS = listOf(
    "Midnight" to Appearance(),
    "Paper" to Appearance(
        highlight = 0xFF2563EB.toInt(),
        background = 0xF2FFFFFF.toInt(),
        text = 0xFF1B1F24.toInt(),
        highPriority = 0xFFDC2626.toInt(),
        mediumPriority = 0xFFB45309.toInt(),
        lowPriority = 0xFF15803D.toInt(),
    ),
    "Glass" to Appearance(background = 0x00000000),
)

@Composable
fun SettingsScreen(
    settings: Settings,
    onAppearanceChange: ((Appearance) -> Appearance) -> Unit,
    onAutoHideChange: (Int) -> Unit,
    onAddButtonSideChange: (BarSide) -> Unit,
    onFileNameChange: (String) -> Unit,
    onUseInternalStorage: () -> Unit,
    onVaultPicked: (Uri) -> Unit,
    onFilePicked: (Uri) -> Unit,
    onClose: () -> Unit,
) {
    var editing by remember { mutableStateOf<ColorSlot?>(null) }

    val vaultPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(onVaultPicked) }

    // Many providers report Markdown as octet-stream, so we cannot filter on mime type here.
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onFilePicked) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "Widget settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        SectionHeader("Colours")
        COLOR_SLOTS.forEach { slot ->
            ColorRow(
                title = slot.title,
                description = slot.description,
                color = slot.value(settings.appearance),
                onClick = { editing = slot },
            )
        }

        SectionHeader("Presets")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PRESETS.forEach { (name, appearance) ->
                OutlinedButton(onClick = { onAppearanceChange { appearance } }) { Text(name) }
            }
        }

        SectionHeader("Add button")
        Text(
            text = "Which edge the add button sits on. The other buttons line up beside it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BarSide.entries.forEach { side ->
                val label = if (side == BarSide.LEFT) "Left" else "Right"
                if (settings.addButtonSide == side) {
                    Button(onClick = { onAddButtonSideChange(side) }) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onAddButtonSideChange(side) }) { Text(label) }
                }
            }
        }

        SectionHeader("Behaviour")
        Text(
            text = "Hide a ticked task after ${settings.autoHideSeconds}s",
            style = MaterialTheme.typography.bodyMedium,
        )
        var pendingSeconds by remember(settings.autoHideSeconds) {
            mutableStateOf(settings.autoHideSeconds.toFloat())
        }
        Slider(
            value = pendingSeconds,
            onValueChange = { pendingSeconds = it },
            onValueChangeFinished = { onAutoHideChange(pendingSeconds.toInt()) },
            valueRange = Settings.AUTO_HIDE_RANGE.first.toFloat()..Settings.AUTO_HIDE_RANGE.last.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader("Todo file")
        Text(
            text = when (settings.storage.mode) {
                StorageMode.INTERNAL -> "Stored inside the app. Nothing else can see it."
                StorageMode.DOCUMENT -> "Stored in a file you picked, so Obsidian and any other " +
                    "Markdown app can read and edit the same list."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vaultPicker.launch(null) }) { Text("Vault folder") }
            OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }) { Text("Single file") }
        }
        Spacer(Modifier.height(8.dp))

        if (settings.storage.treeUri != null && settings.storage.mode == StorageMode.DOCUMENT) {
            var name by remember(settings.storage.fileName) { mutableStateOf(settings.storage.fileName) }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("File name inside the vault") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { onFileNameChange(name) }) { Text("Use this file name") }
        }

        if (settings.storage.mode != StorageMode.INTERNAL) {
            TextButton(onClick = onUseInternalStorage) { Text("Back to app storage") }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tasks are written in the Obsidian Tasks format, so priorities and due dates " +
                "keep working in Obsidian.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }

    editing?.let { slot ->
        ColorPickerDialog(
            title = slot.title,
            initial = slot.value(settings.appearance),
            allowTransparency = slot.allowTransparency,
            onDismiss = { editing = null },
            onConfirm = { chosen ->
                onAppearanceChange { current -> slot.apply(current, chosen) }
                editing = null
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(24.dp))
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ColorRow(title: String, description: String, color: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(color), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(8.dp)),
        )
        Spacer(Modifier.size(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
