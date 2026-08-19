package com.felicedesign.todowidget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.felicedesign.todowidget.model.Appearance

/**
 * Both overlay screens borrow the widget's own colours, so the add sheet and the settings screen
 * look like they belong to the widget the user just tapped.
 */
@Composable
fun AppTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val surface = Color(appearance.background).copy(alpha = 1f)
    val onSurface = Color(appearance.text)
    val accent = Color(appearance.highlight)

    val scheme = if (appearance.background.isDark() || isSystemInDarkTheme()) {
        darkColorScheme(
            primary = accent,
            onPrimary = if (appearance.highlight.isDark()) Color.White else Color.Black,
            surface = surface,
            onSurface = onSurface,
            background = surface,
            onBackground = onSurface,
            surfaceVariant = surface,
            onSurfaceVariant = onSurface.copy(alpha = 0.7f),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = if (appearance.highlight.isDark()) Color.White else Color.Black,
            surface = surface,
            onSurface = onSurface,
            background = surface,
            onBackground = onSurface,
            surfaceVariant = surface,
            onSurfaceVariant = onSurface.copy(alpha = 0.7f),
        )
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
