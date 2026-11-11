package com.retroplayer.core.theme

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.abs
import kotlin.math.min

/** EN-01 vinyl finishes (DESIGN.md 14.1). */
enum class VinylFinish { Classic, Translucent, Marble, Splatter, Picture }

/** EN-01 cassette shell and reel styles. Clear is the v1 look. */
enum class CassetteShell { Clear, Black, White, Tinted }

/** Song-name label styles on the cassette. */
enum class LabelStyle { Ruled, Printed, Handwritten }

/** Colours derived from one album's art, cached per album key. */
@Immutable
data class AlbumPalette(
    val tint: Color,
    val light: Color,
    val dark: Color,
    /** Distinct hue groups among swatches covering >= 5% of the art. */
    val variety: Int,
    /** Stable hash of the album key, for deterministic tie-breaks. */
    val hash: Int,
    val dominantLuminance: Float,
)

/** What the player objects actually draw for the current album. */
@Immutable
data class ObjectLook(
    val finish: VinylFinish = VinylFinish.Classic,
    val shell: CassetteShell = CassetteShell.Clear,
    /** Null = no song-name label on the cassette (default, v1 look). */
    val label: LabelStyle? = null,
    val palette: AlbumPalette? = null,
) {
    /** Palette for drawing; a neutral one when the album has no art. */
    val colors: AlbumPalette get() = palette ?: NeutralPalette

    companion object {
        val Default = ObjectLook()
        val NeutralPalette = AlbumPalette(
            tint = Color(0xFF8A6A50), light = Color(0xFFD9C6B0), dark = Color(0xFF3A2A1E),
            variety = 1, hash = 0, dominantLuminance = 0.2f,
        )
    }
}

/** Automatic choices (DESIGN.md 14.1 table): same album always gives the same result. */
fun AlbumPalette.autoFinish(): VinylFinish = when {
    dominantLuminance < 0.08f -> VinylFinish.Classic
    variety <= 2 -> VinylFinish.Translucent
    variety <= 4 -> VinylFinish.Marble
    else -> VinylFinish.Splatter
}

fun AlbumPalette.autoShell(): CassetteShell = when {
    dominantLuminance < 0.08f -> CassetteShell.Black
    variety <= 2 -> CassetteShell.Tinted
    variety <= 4 -> if (hash % 2 == 0) CassetteShell.White else CassetteShell.Clear
    else -> CassetteShell.Clear
}

fun AlbumPalette.autoLabel(): LabelStyle = when {
    dominantLuminance < 0.08f -> LabelStyle.Printed
    variety <= 2 -> LabelStyle.Handwritten
    variety <= 4 -> LabelStyle.Ruled
    else -> if (hash % 2 == 0) LabelStyle.Ruled else LabelStyle.Handwritten
}

/** Derives and caches [AlbumPalette]s. Call [derive] off the main thread. */
object AlbumPalettes {
    private val cache = LruCache<String, AlbumPalette>(64)

    fun cached(albumKey: String): AlbumPalette? = cache.get(albumKey)

    fun derive(bitmap: Bitmap, albumKey: String): AlbumPalette {
        cache.get(albumKey)?.let { return it }
        val p = Palette.from(bitmap).maximumColorCount(16).generate()
        val swatches = p.swatches
        val total = swatches.sumOf { it.population }.coerceAtLeast(1)

        val dominant = p.dominantSwatch
        val tintSwatch = p.vibrantSwatch ?: dominant
        val tint = tintSwatch?.let { Color(it.rgb) } ?: ObjectLook.NeutralPalette.tint

        val light = swatches
            .filter { it.hsl[1] > 0.15f }
            .maxByOrNull { it.hsl[2] }
            ?.let { Color(it.rgb) }
            ?: lerp(tint, Color.White, 0.6f)
        val dark = swatches.minByOrNull { it.hsl[2] }?.let { Color(it.rgb) } ?: lerp(tint, Color.Black, 0.6f)

        // Hue groups: swatches covering >= 5% of the art whose hues differ by more than 30deg.
        val hues = mutableListOf<Float>()
        swatches
            .filter { it.population.toFloat() / total >= 0.05f }
            .sortedByDescending { it.population }
            .forEach { s ->
                val h = s.hsl[0]
                if (hues.none { hueDistance(it, h) <= 30f }) hues += h
            }

        val palette = AlbumPalette(
            tint = tint,
            light = light,
            dark = dark,
            variety = hues.size.coerceAtLeast(1),
            hash = albumKey.hashCode().let { if (it == Int.MIN_VALUE) 0 else abs(it) },
            dominantLuminance = dominant?.let { ColorUtils.calculateLuminance(it.rgb).toFloat() } ?: 0.2f,
        )
        cache.put(albumKey, palette)
        return palette
    }

    private fun hueDistance(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return min(d, 360f - d)
    }
}

/** White or #2A1606, whichever has higher contrast on [bg] (printed label ink). */
fun printedInk(bg: Color): Color {
    val white = ColorUtils.calculateContrast(Color.White.toArgb(), bg.copy(alpha = 1f).toArgb())
    val dark = ColorUtils.calculateContrast(Color(0xFF2A1606).toArgb(), bg.copy(alpha = 1f).toArgb())
    return if (white >= dark) Color.White else Color(0xFF2A1606)
}
