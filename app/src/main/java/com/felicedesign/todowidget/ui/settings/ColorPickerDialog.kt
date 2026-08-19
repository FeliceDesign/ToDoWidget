package com.felicedesign.todowidget.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

/**
 * A small HSV picker. Hue, saturation and brightness cover the everyday cases, and the alpha slider
 * — offered only where it makes sense — is what lets the widget background go fully transparent.
 */
@Composable
fun ColorPickerDialog(
    title: String,
    initial: Int,
    allowTransparency: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val startHsv = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) }
    }
    var hue by remember { mutableFloatStateOf(startHsv[0]) }
    var saturation by remember { mutableFloatStateOf(startHsv[1]) }
    var value by remember { mutableFloatStateOf(startHsv[2]) }
    var alpha by remember { mutableFloatStateOf(((initial ushr 24) and 0xFF) / 255f) }

    val current = android.graphics.Color.HSVToColor(
        (alpha * 255).toInt().coerceIn(0, 255),
        floatArrayOf(hue, saturation, value),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(current), RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(10.dp)),
                    )
                    Text(
                        text = "#%08X".format(current),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                LabelledSlider("Hue", hue, 0f, 360f) { hue = it }
                LabelledSlider("Saturation", saturation, 0f, 1f) { saturation = it }
                LabelledSlider("Brightness", value, 0f, 1f) { value = it }
                if (allowTransparency) {
                    LabelledSlider("Opacity", alpha, 0f, 1f) { alpha = it }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SWATCHES.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color(swatch), RoundedCornerShape(6.dp))
                                .clickable {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(swatch, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun LabelledSlider(
    label: String,
    value: Float,
    from: Float,
    to: Float,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = from..to,
            modifier = Modifier.fillMaxWidth().height(36.dp),
        )
    }
}

private val SWATCHES = listOf(
    0xFF3B82F6.toInt(),
    0xFF8B5CF6.toInt(),
    0xFFF75A5A.toInt(),
    0xFFE3B341.toInt(),
    0xFF4ECB71.toInt(),
    0xFFE8EDF2.toInt(),
    0xFF0F1419.toInt(),
)
