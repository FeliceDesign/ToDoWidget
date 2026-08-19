package com.felicedesign.todowidget.ui.theme

/** Returns the same colour at a fraction of its current opacity. */
fun Int.scaleAlpha(fraction: Float): Int {
    val alpha = ((this ushr 24) and 0xFF) * fraction.coerceIn(0f, 1f)
    return (alpha.toInt().coerceIn(0, 255) shl 24) or (this and 0x00FFFFFF)
}

/** True when a colour is dark enough that white text sits comfortably on top. */
fun Int.isDark(): Boolean {
    val r = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val b = this and 0xFF
    return (0.299 * r + 0.587 * g + 0.114 * b) < 140
}
