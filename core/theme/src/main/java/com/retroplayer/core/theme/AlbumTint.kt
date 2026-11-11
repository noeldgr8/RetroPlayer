package com.retroplayer.core.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette

/** "Tint from album art" (off by default). Call [extract] off the main thread. */
object AlbumTint {
    fun extract(bitmap: Bitmap): Color? {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
        return swatch?.rgb?.let { Color(it) }
    }
}

private const val TEXT_CONTRAST = 4.5

/**
 * Replaces primary / primaryContainer with [accent], nudging tone until text contrast is
 * AA (4.5:1) against the background (and on-colours against their containers).
 */
fun ColorScheme.withAccent(accent: Color): ColorScheme {
    val bg = background
    val darkBg = bg.luminance() < 0.5f
    val primary = ensureContrast(accent, bg, towardLight = darkBg)
    val onPrimary = if (contrast(Color.White, primary) >= contrast(Color.Black, primary)) Color.White else Color.Black
    val container = lerp(bg, primary, if (darkBg) 0.35f else 0.22f)
    val onContainer = ensureContrast(primary, container, towardLight = darkBg)
    return copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        surfaceTint = primary,
    )
}

private fun Color.luminance(): Float = ColorUtils.calculateLuminance(toArgb()).toFloat()

private fun contrast(fg: Color, bg: Color): Double =
    ColorUtils.calculateContrast(fg.copy(alpha = 1f).toArgb(), bg.copy(alpha = 1f).toArgb())

private fun ensureContrast(color: Color, bg: Color, towardLight: Boolean): Color {
    if (contrast(color, bg) >= TEXT_CONTRAST) return color
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    repeat(40) {
        hsl[2] = (hsl[2] + if (towardLight) 0.025f else -0.025f).coerceIn(0f, 1f)
        val c = Color(ColorUtils.HSLToColor(hsl))
        if (contrast(c, bg) >= TEXT_CONTRAST) return c
    }
    return if (towardLight) Color.White else Color.Black
}
