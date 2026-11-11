package com.retroplayer.feature.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Filled Material icon paths (24 x 24 viewport), shared by Compose icons and widget bitmaps. */
object IconPaths {
    const val PLAY = "M8 5v14l11-7z"
    const val PAUSE = "M6 19h4V5H6v14zm8-14v14h4V5h-4z"
    const val STOP = "M6 6h12v12H6z"
    const val SKIP_NEXT = "M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"
    const val SKIP_PREVIOUS = "M6 6h2v12H6zm3.5 6l8.5 6V6z"
    const val FAST_FORWARD = "M4 18l8.5-6L4 6v12zm9-12v12l8.5-6L13 6z"
    const val FAST_REWIND = "M11 18V6l-8.5 6 8.5 6zm.5-6l8.5 6V6l-8.5 6z"
    const val MUSIC_NOTE = "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"
    const val NOTIFICATIONS_OFF =
        "M20 18.69L7.84 6.14 5.27 3.49 4 4.76l2.8 2.8v.01c-.52.99-.8 2.16-.8 3.42v5l-2 2v1h13.73l2 2" +
            "L21 19.72l-1-1.03zM12 22c1.11 0 2-.89 2-2h-4c0 1.11.89 2 2 2zm6-7.32V11c0-3.08-1.64-5.64" +
            "-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68c-.15.03-.29.08-.42.12-.1.03-.2.07" +
            "-.3.11h-.01c-.01 0-.01 0-.02.01-.23.09-.46.2-.68.31 0 0-.01 0-.01.01L18 14.68z"
    const val ARROW_DOWN = "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"
    const val CHEVRON_RIGHT = "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z"
    const val CHECK = "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"
}

/** The handful of filled Material icons the app needs (avoids material-icons-extended). */
object RetroIcons {
    val Play: ImageVector by lazy { icon("Play", IconPaths.PLAY) }
    val Pause: ImageVector by lazy { icon("Pause", IconPaths.PAUSE) }
    val Stop: ImageVector by lazy { icon("Stop", IconPaths.STOP) }
    val SkipNext: ImageVector by lazy { icon("SkipNext", IconPaths.SKIP_NEXT) }
    val SkipPrevious: ImageVector by lazy { icon("SkipPrevious", IconPaths.SKIP_PREVIOUS) }
    val FastForward: ImageVector by lazy { icon("FastForward", IconPaths.FAST_FORWARD) }
    val FastRewind: ImageVector by lazy { icon("FastRewind", IconPaths.FAST_REWIND) }
    val MusicNote: ImageVector by lazy { icon("MusicNote", IconPaths.MUSIC_NOTE) }
    val NotificationsOff: ImageVector by lazy { icon("NotificationsOff", IconPaths.NOTIFICATIONS_OFF) }
    val ArrowDown: ImageVector by lazy { icon("ArrowDown", IconPaths.ARROW_DOWN) }
    val ChevronRight: ImageVector by lazy { icon("ChevronRight", IconPaths.CHEVRON_RIGHT) }
    val Check: ImageVector by lazy { icon("Check", IconPaths.CHECK) }

    private fun icon(name: String, path: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
            .build()
}
