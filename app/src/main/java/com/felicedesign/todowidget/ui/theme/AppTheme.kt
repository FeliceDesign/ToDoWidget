package com.felicedesign.todowidget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A fixed, neutral palette for the two overlay screens.
 *
 * They deliberately do *not* follow the user's widget colours. Those colours are chosen to sit on a
 * wallpaper, and applying them here produced unreadable combinations — a black-on-black settings
 * screen the moment someone picked a dark background or a transparent one. Only the widget changes;
 * the screens that configure it stay legible no matter what is set.
 */
private val NeutralScheme = darkColorScheme(
    primary = Color(0xFF4C8DF6),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF7C8798),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF15171B),
    onBackground = Color(0xFFF1F3F6),
    surface = Color(0xFF1B1E23),
    onSurface = Color(0xFFF1F3F6),
    surfaceVariant = Color(0xFF262A31),
    onSurfaceVariant = Color(0xFFA6AEBB),
    outline = Color(0xFF3B424D),
    error = Color(0xFFF87171),
    onError = Color(0xFF1B1E23),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NeutralScheme, content = content)
}
