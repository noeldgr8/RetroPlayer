package com.retroplayer.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The two skeuomorphic player objects. */
enum class PlayerStyle { Vinyl, Cassette }

/**
 * Colours for the player objects (design-tokens.json > themes.<id>.player).
 * CSS gradients from the token file are stored as their colour stops; the drawing code in
 * :feature:player turns them into Brushes at the right geometry.
 */
sealed interface PlayerTokens

@Immutable
data class VinylTokens(
    // Plinth: linear-gradient(155deg, start, end) + repeating 93deg grain stripes
    val plinthStart: Color,
    val plinthEnd: Color,
    val grain: Color,
    val grainPeriod: Float,
    val plinthFlat: Color,
    // plinthShadow: inset 0 2 0 highlight, inset 0 -4 0 shade, drop shadow 0 20 blur
    val plinthHighlight: Color,
    val plinthInnerShade: Color,
    val plinthDropShadow: Color,
    val plinthDropBlur: Float,
    // Platter radial with hard stops at 60/61% and 97/98%
    val platterCenter: Color,
    val platterMid: Color,
    val platterEdge: Color,
    val record: Color,
    val grooveA: Color,
    val grooveB: Color,
    /** Dark themes only: 0 0 40 6 glow around the record. */
    val grooveGlow: Color?,
    /** linear-gradient(90deg, c0, c1 45%, c2) */
    val tonearm: List<Color>,
    /** radial(at 35% 30%: c0, c1 70%, c2) */
    val tonearmBase: List<Color>,
    val headshell: Color,
    /** linear-gradient(90deg, c0, c1, c2) */
    val counterweight: List<Color>,
    val pivotCap: Color,
    /** radial(at 35% 30%: c0, c1) */
    val startKnob: List<Color>,
    val plinthText: Color,
    val idleMat: Color,
    val idleMatLine: Color,
    /** radial(at 35% 30%: c0, c1 70%, c2) */
    val widgetKnob: List<Color>,
    val widgetKnobInk: Color,
    val widgetInk: Color,
    val widgetInkVariant: Color,
    val widgetTrack: Color,
) : PlayerTokens

@Immutable
data class CassetteTokens(
    /** linear-gradient(180deg, top, bottom) */
    val bodyTop: Color,
    val bodyBottom: Color,
    val bodyFlat: Color,
    val bodyHighlight: Color,
    val bodyInnerShade: Color,
    val bodyDropShadow: Color,
    val bodyDropBlur: Float,
    /** radial(at 35% 30%: light, dark) */
    val screwLight: Color,
    val screwDark: Color,
    val bodyText: Color,
    val edge: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val shell: Color,
    val reelHub: Color,
    val cogDark: Color,
    val tapeA: Color,
    val tapeB: Color,
    val tapeLine: Color,
    val roller: Color,
    val rollerStroke: Color,
    val head: Color,
    val grille: Color,
    val wheel: Color,
    val indicatorOn: Color,
    val indicatorOff: Color,
    val keyWell: Color,
    val keyFaceTop: Color,
    val keyFaceBottom: Color,
    val keyEdge: Color,
    val keyInk: Color,
    val widgetInk: Color,
    val widgetInkVariant: Color,
    val widgetTrack: Color,
) : PlayerTokens

private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r / 255f, g / 255f, b / 255f, a)

val VinylLightTokens = VinylTokens(
    plinthStart = Color(0xFFEEE2CE),
    plinthEnd = Color(0xFFD8C3A2),
    grain = rgba(110, 70, 30, .07f),
    grainPeriod = 9f,
    plinthFlat = Color(0xFFE3D2B6),
    plinthHighlight = rgba(255, 255, 255, .5f),
    plinthInnerShade = rgba(90, 60, 30, .25f),
    plinthDropShadow = rgba(60, 40, 20, .25f),
    plinthDropBlur = 40f,
    platterCenter = Color(0xFFE4E6E9),
    platterMid = Color(0xFFC3C7CC),
    platterEdge = Color(0xFF9EA3A9),
    record = Color(0xFF161616),
    grooveA = Color(0xFF111111),
    grooveB = Color(0xFF232323),
    grooveGlow = null,
    tonearm = listOf(Color(0xFFA9AEB5), Color(0xFFF3F4F6), Color(0xFF9AA0A7)),
    tonearmBase = listOf(Color(0xFFFFFFFF), Color(0xFFB4B9C0), Color(0xFF8F959C)),
    headshell = Color(0xFF2A2A2A),
    counterweight = listOf(Color(0xFF8F949B), Color(0xFFD9DCE0), Color(0xFF8F949B)),
    pivotCap = Color(0xFF7D838A),
    startKnob = listOf(Color(0xFFFFFFFF), Color(0xFFCFC2AA)),
    plinthText = Color(0xFF5B4630),
    idleMat = Color(0xFF2B2B2B),
    idleMatLine = Color(0xFF333333),
    widgetKnob = listOf(Color(0xFFFFFFFF), Color(0xFFC9CDD2), Color(0xFF9EA3A9)),
    widgetKnobInk = Color(0xFF2A2A2A),
    widgetInk = Color(0xFF2A1E12),
    widgetInkVariant = Color(0xFF5B4630),
    widgetTrack = rgba(60, 40, 20, .2f),
)

val VinylDarkTokens = VinylTokens(
    plinthStart = Color(0xFF4B2E1C),
    plinthEnd = Color(0xFF24150C),
    grain = rgba(0, 0, 0, .22f),
    grainPeriod = 8f,
    plinthFlat = Color(0xFF3A2416),
    plinthHighlight = rgba(255, 255, 255, .08f),
    plinthInnerShade = rgba(0, 0, 0, .4f),
    plinthDropShadow = rgba(0, 0, 0, .6f),
    plinthDropBlur = 44f,
    platterCenter = Color(0xFF3B3B3F),
    platterMid = Color(0xFF2A2A2E),
    platterEdge = Color(0xFF18181B),
    record = Color(0xFF0E0E0E),
    grooveA = Color(0xFF111111),
    grooveB = Color(0xFF232323),
    grooveGlow = rgba(255, 182, 138, .18f),
    tonearm = listOf(Color(0xFF4E5258), Color(0xFFB3B7BD), Color(0xFF45494F)),
    tonearmBase = listOf(Color(0xFFA3A7AD), Color(0xFF3A3D42), Color(0xFF222428)),
    headshell = Color(0xFF0F0F0F),
    counterweight = listOf(Color(0xFF3E4146), Color(0xFF8A8E94), Color(0xFF3E4146)),
    pivotCap = Color(0xFF1C1D20),
    startKnob = listOf(Color(0xFF8B6A50), Color(0xFF3A2416)),
    plinthText = Color(0xFFCDB49B),
    idleMat = Color(0xFF1E1E21),
    idleMatLine = Color(0xFF26262A),
    widgetKnob = listOf(Color(0xFF6A6E74), Color(0xFF2E3034), Color(0xFF1C1D20)),
    widgetKnobInk = Color(0xFFEEE0D3),
    widgetInk = Color(0xFFF1E4D6),
    widgetInkVariant = Color(0xFFCDB49B),
    widgetTrack = rgba(255, 255, 255, .16f),
)

val CassetteLightTokens = CassetteTokens(
    bodyTop = Color(0xFFE3E6EA),
    bodyBottom = Color(0xFFC7CCD3),
    bodyFlat = Color(0xFFD2D6DC),
    bodyHighlight = rgba(255, 255, 255, .6f),
    bodyInnerShade = rgba(0, 0, 0, .16f),
    bodyDropShadow = rgba(30, 40, 60, .28f),
    bodyDropBlur = 36f,
    screwLight = Color(0xFFFFFFFF),
    screwDark = Color(0xFF8D939B),
    bodyText = Color(0xFF4A5059),
    edge = Color(0xFFA9AFB8),
    glassTop = Color(0xFF2A2F37),
    glassBottom = Color(0xFF161A1F),
    shell = rgba(236, 237, 240, .16f),
    reelHub = Color(0xFFF4F4F4),
    cogDark = Color(0xFF1A1A1A),
    tapeA = Color(0xFF4A3326),
    tapeB = Color(0xFF3B281D),
    tapeLine = Color(0xFF3B281D),
    roller = Color(0xFFC9CCD1),
    rollerStroke = Color(0xFF7D838A),
    head = Color(0xFF7D838A),
    grille = Color(0xFFA3AAB3),
    wheel = Color(0xFFD4D8DE),
    indicatorOn = Color(0xFFFF9F1A),
    indicatorOff = Color(0xFF8C929B),
    keyWell = Color(0xFFC3C8CF),
    keyFaceTop = Color(0xFFFAFBFC),
    keyFaceBottom = Color(0xFFE2E5E9),
    keyEdge = Color(0xFF9CA3AD),
    keyInk = Color(0xFF2B3038),
    widgetInk = Color(0xFF191C20),
    widgetInkVariant = Color(0xFF42474F),
    widgetTrack = rgba(0, 0, 0, .14f),
)

val CassetteDarkTokens = CassetteTokens(
    bodyTop = Color(0xFF3A3D43),
    bodyBottom = Color(0xFF202226),
    bodyFlat = Color(0xFF2C2F34),
    bodyHighlight = rgba(255, 255, 255, .1f),
    bodyInnerShade = rgba(0, 0, 0, .4f),
    bodyDropShadow = rgba(0, 0, 0, .6f),
    bodyDropBlur = 40f,
    screwLight = Color(0xFF9A9EA5),
    screwDark = Color(0xFF2A2C30),
    bodyText = Color(0xFFA9AEB6),
    edge = Color(0xFF121316),
    glassTop = Color(0xFF15171B),
    glassBottom = Color(0xFF0B0C0E),
    shell = rgba(200, 205, 215, .10f),
    reelHub = Color(0xFFE8E8E8),
    cogDark = Color(0xFF1A1A1A),
    tapeA = Color(0xFF4A3326),
    tapeB = Color(0xFF3B281D),
    tapeLine = Color(0xFF3B281D),
    roller = Color(0xFFC9CCD1),
    rollerStroke = Color(0xFF7D838A),
    head = Color(0xFF7D838A),
    grille = Color(0xFF121316),
    wheel = Color(0xFF3A3D42),
    indicatorOn = Color(0xFFFFB547),
    indicatorOff = Color(0xFF4A4E55),
    keyWell = Color(0xFF0B0C0E),
    keyFaceTop = Color(0xFF474A50),
    keyFaceBottom = Color(0xFF34373C),
    keyEdge = Color(0xFF050506),
    keyInk = Color(0xFFD9DCE1),
    widgetInk = Color(0xFFE3E2E6),
    widgetInkVariant = Color(0xFFA9AEB6),
    widgetTrack = rgba(255, 255, 255, .14f),
)
