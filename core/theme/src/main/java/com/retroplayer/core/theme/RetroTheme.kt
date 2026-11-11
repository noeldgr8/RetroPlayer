package com.retroplayer.core.theme

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

/** The four themes. [id] matches the design tokens and is what gets persisted. */
enum class RetroTheme(
    val id: String,
    val style: PlayerStyle,
    val isDark: Boolean,
    @StringRes val nameRes: Int,
) {
    VinylLight("vl", PlayerStyle.Vinyl, false, R.string.theme_vinyl_light),
    VinylDark("vd", PlayerStyle.Vinyl, true, R.string.theme_vinyl_dark),
    CassetteLight("cl", PlayerStyle.Cassette, false, R.string.theme_cassette_light),
    CassetteDark("cd", PlayerStyle.Cassette, true, R.string.theme_cassette_dark);

    val colorScheme: ColorScheme
        get() = when (this) {
            VinylLight -> VinylLightScheme
            VinylDark -> VinylDarkScheme
            CassetteLight -> CassetteLightScheme
            CassetteDark -> CassetteDarkScheme
        }

    val playerTokens: PlayerTokens
        get() = when (this) {
            VinylLight -> VinylLightTokens
            VinylDark -> VinylDarkTokens
            CassetteLight -> CassetteLightTokens
            CassetteDark -> CassetteDarkTokens
        }

    /** Same style, light or dark variant. */
    fun withDark(dark: Boolean): RetroTheme = entries.first { it.style == style && it.isDark == dark }

    companion object {
        fun fromId(id: String?): RetroTheme = entries.firstOrNull { it.id == id } ?: VinylLight
    }
}

/** Everything the player objects need beyond the Material colour scheme. */
@Immutable
data class RetroStyle(
    val theme: RetroTheme,
    val tokens: PlayerTokens,
    val displayFont: FontFamily,
) {
    val style: PlayerStyle get() = theme.style
    val isDark: Boolean get() = theme.isDark
}

val LocalRetroStyle = staticCompositionLocalOf {
    RetroStyle(RetroTheme.VinylLight, VinylLightTokens, RetroFonts.displayFor(PlayerStyle.Vinyl))
}

/**
 * App theme. Player tokens always come from [theme]; [dynamicChrome] (wallpaper colours) only
 * affects Material chrome. [accent] is an album-art colour (already extracted) used when
 * "Tint from album art" is on; contrast is re-checked in [withAccent].
 */
@Composable
fun RetroPlayerTheme(
    theme: RetroTheme,
    dynamicChrome: Boolean = false,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base = when {
        dynamicChrome && theme.isDark -> dynamicDarkColorScheme(context)
        dynamicChrome -> dynamicLightColorScheme(context)
        else -> theme.colorScheme
    }
    val target = remember(base, accent) { if (accent != null) base.withAccent(accent) else base }
    val scheme = animateColorScheme(target)
    val retro = remember(theme) { RetroStyle(theme, theme.playerTokens, RetroFonts.displayFor(theme.style)) }
    CompositionLocalProvider(LocalRetroStyle provides retro) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

@Composable
private fun animC(target: Color): Color =
    animateColorAsState(target, tween(RetroMotion.THEME_CROSSFADE_MS), label = "scheme").value

/** Cross-fades every role used by the app over 350 ms. */
@Composable
private fun animateColorScheme(t: ColorScheme): ColorScheme = t.copy(
    primary = animC(t.primary),
    onPrimary = animC(t.onPrimary),
    primaryContainer = animC(t.primaryContainer),
    onPrimaryContainer = animC(t.onPrimaryContainer),
    secondary = animC(t.secondary),
    onSecondary = animC(t.onSecondary),
    secondaryContainer = animC(t.secondaryContainer),
    onSecondaryContainer = animC(t.onSecondaryContainer),
    tertiary = animC(t.tertiary),
    background = animC(t.background),
    onBackground = animC(t.onBackground),
    surface = animC(t.surface),
    onSurface = animC(t.onSurface),
    surfaceVariant = animC(t.surfaceVariant),
    onSurfaceVariant = animC(t.onSurfaceVariant),
    surfaceTint = animC(t.surfaceTint),
    outline = animC(t.outline),
    outlineVariant = animC(t.outlineVariant),
    surfaceContainerLowest = animC(t.surfaceContainerLowest),
    surfaceContainerLow = animC(t.surfaceContainerLow),
    surfaceContainer = animC(t.surfaceContainer),
    surfaceContainerHigh = animC(t.surfaceContainerHigh),
    surfaceContainerHighest = animC(t.surfaceContainerHighest),
)

/** Motion values from the design tokens. */
object RetroMotion {
    const val VINYL_REVOLUTION_MS = 1800
    const val TONEARM_MOVE_MS = 600
    const val TONEARM_REST_DEG = 0f
    const val TONEARM_PLAY_DEG = 22f
    const val PLATTER_SPIN_UP_MS = 800
    const val PLATTER_SPIN_DOWN_MS = 1000
    const val REEL_BASE_REVOLUTION_MS = 2400
    const val REEL_RATE_CONSTANT = 28f
    const val CASSETTE_START_MS = 500
    const val PLAY_BUTTON_MORPH_MS = 300
    const val THEME_CROSSFADE_MS = 350
    const val STYLE_SWAP_MS = 500
}
