package com.retroplayer.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Fonts. TODO(fonts): bundle Roboto Flex, Fraunces (600) and Caveat as res/font files in this
 * module (or wire Google downloadable fonts) and swap these placeholders. Until then Fraunces
 * falls back to the system serif and Roboto Flex to the system sans.
 */
object RetroFonts {
    val Ui: FontFamily = FontFamily.Default
    val Fraunces: FontFamily = FontFamily.Serif

    fun displayFor(style: PlayerStyle): FontFamily =
        if (style == PlayerStyle.Vinyl) Fraunces else Ui
}

/** Type scale from DESIGN.md section 3. */
object RetroType {
    fun appName(display: FontFamily, inner: Boolean) = TextStyle(
        fontFamily = display,
        fontWeight = FontWeight.SemiBold,
        fontSize = if (inner) 24.sp else 22.sp,
        lineHeight = if (inner) 30.sp else 28.sp,
    )

    fun titleCover(display: FontFamily) = TextStyle(
        fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp,
    )

    fun titleInner(display: FontFamily) = TextStyle(
        fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp,
    )

    fun titleLandscape(display: FontFamily) = TextStyle(
        fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp,
    )

    fun headline(display: FontFamily, size: Int = 30, line: Int = 36) = TextStyle(
        fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = size.sp, lineHeight = line.sp,
    )

    val artistCover = TextStyle(fontFamily = RetroFonts.Ui, fontSize = 16.sp, lineHeight = 22.sp)
    val artistInner = TextStyle(fontFamily = RetroFonts.Ui, fontSize = 18.sp, lineHeight = 26.sp)

    val source = TextStyle(
        fontFamily = RetroFonts.Ui,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.08.em,
    )

    val times = TextStyle(
        fontFamily = RetroFonts.Ui,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
    )

    val caption = TextStyle(fontFamily = RetroFonts.Ui, fontSize = 13.sp, lineHeight = 18.sp)

    fun keyLabel(big: Boolean) = TextStyle(
        fontFamily = RetroFonts.Ui,
        fontWeight = FontWeight.Bold,
        fontSize = if (big) 13.sp else 11.sp,
        letterSpacing = 0.12.em,
    )
}
