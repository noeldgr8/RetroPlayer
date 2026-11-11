package com.retroplayer.feature.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.RetroMotion
import com.retroplayer.core.theme.ObjectLook
import com.retroplayer.core.theme.VinylTokens

/*
 * Still frames of the player objects, drawn with exactly the same layer code as the animated
 * composables. Used for widgets (pre-render the object into bitmaps per state).
 * Both draw in base units at the current transform: scale before calling.
 */

/** Turntable in a 400 x 400 box at the origin. Tonearm on the record when [playing]. */
fun DrawScope.drawTurntableStill(
    tokens: VinylTokens,
    art: ImageBitmap?,
    playing: Boolean,
    labelColor: Color,
    textMeasurer: TextMeasurer,
    idle: Boolean = false,
    bare: Boolean = true,
    progress: Float = 0f,
    look: ObjectLook = ObjectLook.Default,
) {
    val label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val t33 = textMeasurer.measure("33", label, density = Density(1f))
    val t45 = textMeasurer.measure("45", label, density = Density(1f))

    drawTurntableBase(tokens, bare, idle)
    if (!idle) {
        translate(26f, 46f) {
            drawRecordLook(tokens, art, labelColor, look)
            drawCircle(Brush.sweepGradient(*SheenStops, center = Offset(144f, 144f)), radius = 144f, center = Offset(144f, 144f))
        }
    }
    val armAngle = if (playing && !idle) RetroMotion.TONEARM_PLAY_DEG + 8f * progress.coerceIn(0f, 1f) else 0f
    rotate(armAngle, pivot = Pivot) { drawTonearm(tokens) }
    drawTurntableTop(tokens, bare, t33, t45)
}

/** Cassette player in a 400 x 260 box at the origin; packs reflect [progress]. */
fun DrawScope.drawCassetteStill(
    tokens: CassetteTokens,
    progress: Float,
    playing: Boolean,
    textMeasurer: TextMeasurer,
    bare: Boolean = true,
    look: ObjectLook = ObjectLook.Default,
    songTitle: String? = null,
) {
    val shell = shellColors(tokens, look)
    val brand = textMeasurer.measure(
        "RETROPLAYER",
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.24.em),
        density = Density(1f),
    )
    val stereo = textMeasurer.measure(
        "STEREO",
        TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.em),
        density = Density(1f),
    )
    val play = textMeasurer.measure(
        "PLAY",
        TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.14.em),
        density = Density(1f),
    )
    val p = progress.coerceIn(0f, 1f)
    drawCassetteBase(tokens, bare, brand, stereo, shell.fill)
    drawTape(tokens, p)
    val songLabel = look.label
    if (songLabel != null && !songTitle.isNullOrBlank()) {
        val text = measureSongLabel(textMeasurer, songLabel, songTitle)
        translate(ShellOrigin.x, ShellOrigin.y) { drawSongLabel(songLabel, look.colors, text) }
    }
    for (c in listOf(PackLeft, PackRight)) {
        val topLeft = ShellOrigin + c - Offset(20f, 20f)
        translate(topLeft.x, topLeft.y) { drawReelHub(tokens, shell.hub, shell.cogGap) }
    }
    drawCassetteTop(tokens, bare, indicator = if (playing) 1f else 0f, playLabel = play)
}
