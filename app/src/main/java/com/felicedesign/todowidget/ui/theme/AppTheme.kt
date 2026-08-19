package com.felicedesign.todowidget.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * A fixed, neutral palette for the two overlay screens.
 *
 * Surfaces and text deliberately do *not* follow the user's widget colours: those are chosen to sit
 * on a wallpaper, and applying them here produced unreadable combinations — a black-on-black
 * settings screen the moment someone picked a dark or transparent background.
 *
 * The accent is the exception. It only ever tints buttons and the text cursor, which stay legible
 * whatever it is, so passing [accentArgb] lets the add sheet show the highlight colour that is
 * actually configured instead of a stock blue.
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
fun AppTheme(accentArgb: Int? = null, content: @Composable () -> Unit) {
    val scheme = if (accentArgb == null) {
        NeutralScheme
    } else {
        NeutralScheme.copy(
            primary = Color(accentArgb),
            onPrimary = if (accentArgb.isDark()) Color.White else Color.Black,
        )
    }

    MaterialTheme(colorScheme = scheme) {
        // A colour scheme alone does not set the default content colour — only Surface and Scaffold
        // do, and neither screen uses one. Without this, every Text that does not name a colour
        // falls back to black, which is invisible on this background.
        CompositionLocalProvider(
            LocalContentColor provides scheme.onSurface,
            content = content,
        )
    }
}
