package com.retroplayer.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.retroplayer.core.media.NowPlaying
import com.retroplayer.core.media.SPOTIFY_PACKAGE
import com.retroplayer.core.settings.AppSettings
import com.retroplayer.core.settings.WidgetConfig
import com.retroplayer.core.theme.AlbumPalettes
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.ObjectLook
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.VinylTokens
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The RetroPlayer home screen widget (DESIGN.md 8). The widget is the machine: chassis,
 * object art and controls are one pre-rendered piece; title, artist and the empty-state
 * button stay live. One implementation serves the Small, Medium and Large receivers and
 * picks the nearest designed layout for its real size.
 */
class RetroWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val media = deps.media()
        val settings = deps.settings()
        // A widget tap can start the process cold: make sure the session tracker is running.
        withContext(Dispatchers.Main) { media.refreshAccess() }
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val renderer = WidgetRenderers.get(context)

        provideContent {
            val nowPlaying by media.nowPlaying.collectAsState()
            val app by settings.settings.collectAsState(initial = AppSettings())
            val config by settings.widgetConfig(appWidgetId).collectAsState(initial = WidgetConfig())
            WidgetContent(renderer, nowPlaying, app, config)
        }
    }

}

/** One renderer per process (it caches control bitmaps). */
internal object WidgetRenderers {
    @Volatile private var instance: WidgetRenderer? = null
    fun get(context: Context): WidgetRenderer =
        instance ?: synchronized(this) {
            instance ?: WidgetRenderer(context.applicationContext).also { instance = it }
        }
}

internal fun resolveTheme(context: Context, app: AppSettings, config: WidgetConfig): RetroTheme {
    val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    return config.theme ?: app.effectiveTheme(systemDark)
}

internal fun resolvePrimary(context: Context, theme: RetroTheme, config: WidgetConfig): Color =
    if (config.theme == null && config.followWallpaper) {
        if (theme.isDark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else {
        theme.colorScheme.primary
    }

/** PRD: album art in widgets is scaled to about 300 x 300 px. */
private fun Bitmap.forWidget(): ImageBitmap {
    val max = 300
    val scaled = if (width > max || height > max) {
        val f = max.toFloat() / maxOf(width, height)
        Bitmap.createScaledBitmap(this, (width * f).toInt().coerceAtLeast(1), (height * f).toInt().coerceAtLeast(1), true)
    } else {
        this
    }
    return scaled.asImageBitmap()
}

@Composable
private fun WidgetContent(
    renderer: WidgetRenderer,
    nowPlaying: NowPlaying?,
    app: AppSettings,
    config: WidgetConfig,
) {
    val context = LocalContext.current
    val sizeDp = LocalSize.current
    val theme = resolveTheme(context, app, config)
    val primary = resolvePrimary(context, theme, config)
    val onPrimary = if (theme.isDark) theme.colorScheme.onPrimary else Color.White
    val mode = when {
        nowPlaying == null -> WidgetMode.Empty
        nowPlaying.isPlaying -> WidgetMode.Playing
        else -> WidgetMode.Paused
    }
    val w = sizeDp.width.value
    val h = sizeDp.height.value
    val layout = WidgetLayout(theme.style, WidgetSize.forSize(w, h), w, h, mode)
    val progress = nowPlaying?.let {
        if (it.durationMs > 0) (it.estimatedPositionMs().toFloat() / it.durationMs).coerceIn(0f, 1f) else 0f
    } ?: 0f
    val art = remember(nowPlaying?.art) { nowPlaying?.art?.forWidget() }
    // EN-01: bake the album's finish / shell / label into the object bitmap.
    val palette = remember(nowPlaying?.albumKey, nowPlaying?.art) {
        nowPlaying?.art?.let { AlbumPalettes.derive(it, nowPlaying.albumKey) }
    }
    val look = if (mode == WidgetMode.Empty) ObjectLook.Default else app.lookFor(palette)
    val background = renderer.background(
        layout, theme, art, progress, theme.colorScheme.primaryContainer, look, nowPlaying?.title,
    )

    val (ink, inkVariant, track) = when (val t = theme.playerTokens) {
        is VinylTokens -> Triple(t.widgetInk, t.widgetInkVariant, t.widgetTrack)
        is CassetteTokens -> Triple(t.widgetInk, t.widgetInkVariant, t.widgetTrack)
    }

    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .clickable(actionStartActivity(openAppIntent(context))),
    ) {
        Image(
            provider = ImageProvider(background),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize(),
        )

        layout.text?.let { t ->
            At(t.x, t.y) {
                Column(GlanceModifier.width(t.w.dp)) {
                    val title = if (mode == WidgetMode.Empty) context.getString(R.string.widget_nothing_playing) else nowPlaying!!.title
                    val sub = if (mode == WidgetMode.Empty) {
                        context.getString(R.string.widget_start_a_song)
                    } else {
                        listOf(nowPlaying!!.artist, nowPlaying.album).filter { it.isNotBlank() }.joinToString(" · ")
                    }
                    Text(
                        title,
                        maxLines = 2,
                        style = TextStyle(color = ColorProvider(ink), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        sub,
                        maxLines = if (layout.size == WidgetSize.Medium) 1 else 2,
                        style = TextStyle(color = ColorProvider(inkVariant), fontSize = 13.sp),
                    )
                }
            }
        }

        layout.progress?.let { p ->
            At(p.x, p.y) {
                LinearProgressIndicator(
                    progress = progress,
                    color = ColorProvider(primary),
                    backgroundColor = ColorProvider(track),
                    modifier = GlanceModifier.width(p.w.dp).height(4.dp),
                )
            }
        }

        val playing = mode == WidgetMode.Playing
        for (spot in layout.controls) {
            val pressed = when (spot.control) {
                Control.PlayPause, Control.Play -> playing
                else -> false
            }
            val label = context.getString(spot.control.labelRes)
            val bitmap = renderer.control(spot, theme, pressed, playing, primary, label)
            At(spot.face.x - spot.margin, spot.face.y - spot.margin) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = context.getString(spot.control.description(theme, playing)),
                    contentScale = ContentScale.FillBounds,
                    modifier = GlanceModifier
                        .size((spot.face.w + 2 * spot.margin).dp, (spot.face.h + 2 * spot.margin).dp)
                        .clickable(commandAction(spot.control, playing)),
                )
            }
        }

        layout.openButton?.let { b ->
            At(b.x, b.y) {
                Button(
                    text = context.getString(R.string.widget_open_spotify),
                    onClick = actionStartActivity(openSpotifyIntent(context)),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = ColorProvider(primary),
                        contentColor = ColorProvider(onPrimary),
                    ),
                    modifier = GlanceModifier.width(b.w.dp).height(b.h.dp),
                )
            }
        }
    }
}

/** Glance has no absolute positioning; a full-size Box with start / top padding places [content]. */
@Composable
private fun At(x: Float, y: Float, content: @Composable () -> Unit) {
    Box(
        GlanceModifier.fillMaxSize().padding(start = x.coerceAtLeast(0f).dp, top = y.coerceAtLeast(0f).dp),
        contentAlignment = Alignment.TopStart,
    ) { content() }
}

private fun Control.description(theme: RetroTheme, playing: Boolean): Int {
    val cassette = theme.style == com.retroplayer.core.theme.PlayerStyle.Cassette
    return when (this) {
        Control.Previous -> if (cassette) R.string.cd_key_rew else R.string.cd_previous
        Control.Next -> if (cassette) R.string.cd_key_ff else R.string.cd_next
        Control.PlayPause -> if (playing) R.string.cd_pause else R.string.cd_play
        Control.Play -> R.string.cd_play
        Control.Stop -> R.string.cd_pause
    }
}

private fun commandAction(control: Control, playing: Boolean) = actionRunCallback<MediaCommandAction>(
    actionParametersOf(
        MediaCommandAction.Command to when (control) {
            Control.Previous -> MediaCommandAction.PREVIOUS
            Control.Next -> MediaCommandAction.NEXT
            Control.Play -> MediaCommandAction.PLAY
            Control.Stop -> MediaCommandAction.PAUSE
            Control.PlayPause -> if (playing) MediaCommandAction.PAUSE else MediaCommandAction.PLAY
        },
    ),
)

/** The app's player activity (explicit: debug builds have a second launcher entry). */
internal fun openAppIntent(context: Context): Intent =
    Intent().setComponent(ComponentName(context.packageName, "com.retroplayer.app.MainActivity"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

internal fun openSpotifyIntent(context: Context): Intent =
    context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
        ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$SPOTIFY_PACKAGE"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
