package com.retroplayer.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.retroplayer.core.media.MediaRepository
import com.retroplayer.core.settings.AppSettings
import com.retroplayer.core.settings.SettingsRepository
import com.retroplayer.core.settings.WidgetConfig
import com.retroplayer.core.theme.CassetteTokens
import com.retroplayer.core.theme.RetroPlayerTheme
import com.retroplayer.core.theme.RetroTheme
import com.retroplayer.core.theme.VinylTokens
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Widget configuration: opens when a widget is added and from Reconfigure.
 * Saves the theme per appWidgetId.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var media: MediaRepository

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out cancels the add.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        lifecycleScope.launch {
            // Reconfigure (a widget that already has a saved theme) says "Save" instead of "Add widget".
            val reconfigure = settings.hasWidgetConfig(appWidgetId)
            val initial = settings.widgetConfig(appWidgetId).first()
            setContent {
                val app by settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
                val appTheme = app.effectiveTheme(isSystemInDarkTheme())
                val view = androidx.compose.ui.platform.LocalView.current
                androidx.compose.runtime.SideEffect {
                    androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !appTheme.isDark
                        isAppearanceLightNavigationBars = !appTheme.isDark
                    }
                }
                RetroPlayerTheme(appTheme) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ConfigScreen(
                            initial = initial,
                            appTheme = appTheme,
                            app = app,
                            reconfigure = reconfigure,
                            onClose = { finish() },
                            onSave = ::save,
                        )
                    }
                }
            }
        }
    }

    private fun save(config: WidgetConfig) {
        lifecycleScope.launch {
            settings.setWidgetConfig(appWidgetId, config)
            runCatching {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity).getGlanceIdBy(appWidgetId)
                RetroWidget().update(this@WidgetConfigActivity, glanceId)
            }
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@Composable
private fun ConfigScreen(
    initial: WidgetConfig,
    appTheme: RetroTheme,
    app: AppSettings,
    reconfigure: Boolean,
    onClose: () -> Unit,
    onSave: (WidgetConfig) -> Unit,
) {
    // null = Same as app
    var themeId by rememberSaveable { mutableStateOf(initial.theme?.id) }
    var wallpaper by rememberSaveable { mutableStateOf(initial.followWallpaper) }
    val chosen = themeId?.let(RetroTheme::fromId)
    val previewTheme = chosen ?: appTheme
    val sameAsApp = chosen == null

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close))
                }
                Text(
                    stringResource(R.string.widget_theme),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // Preview card: wallpaper tone with the Medium widget in the chosen theme.
                Box(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFA69A8B))
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    MediumWidgetPreview(previewTheme, if (sameAsApp && wallpaper) WidgetConfig(null, true) else WidgetConfig(chosen, false), app)
                }

                Column(Modifier.selectableGroup().padding(top = 8.dp)) {
                    RadioRow(
                        title = stringResource(R.string.same_as_app),
                        supporting = stringResource(R.string.currently, stringResource(appTheme.nameRes)),
                        selected = sameAsApp,
                        onClick = { themeId = null },
                    )
                    for (t in RetroTheme.entries) {
                        RadioRow(stringResource(t.nameRes), null, selected = themeId == t.id, onClick = { themeId = t.id })
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .alpha(if (sameAsApp) 1f else 0.45f)
                        .toggleable(wallpaper, enabled = sameAsApp, role = Role.Switch, onValueChange = { wallpaper = it })
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 16.dp)) {
                        Text(stringResource(R.string.follow_wallpaper), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.follow_wallpaper_supporting),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = wallpaper && sameAsApp, onCheckedChange = null, enabled = sameAsApp)
                }
            }
            Button(
                onClick = { onSave(WidgetConfig(chosen, wallpaper && sameAsApp)) },
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 28.dp, top = 12.dp).height(52.dp),
            ) {
                Text(stringResource(if (reconfigure) R.string.save_widget else R.string.add_widget))
            }
        }
    }
}

@Composable
private fun RadioRow(title: String, supporting: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.size(20.dp))
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Medium widget preview built from the same renderer and layout as the real widget,
 * showing the current track (or the empty state).
 */
@Composable
private fun MediumWidgetPreview(theme: RetroTheme, config: WidgetConfig, app: AppSettings) {
    val context = LocalContext.current
    val renderer = remember { WidgetRenderers.get(context) }
    val media = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).media()
    }
    val np by media.nowPlaying.collectAsStateWithLifecycle()
    BoxWithConstraints(Modifier.padding(16.dp)) {
        val w = minOf(maxWidth.value, 360f)
        val h = 170f * w / 360f
        val mode = when {
            np == null -> WidgetMode.Empty
            np!!.isPlaying -> WidgetMode.Playing
            else -> WidgetMode.Paused
        }
        val layout = WidgetLayout(theme.style, WidgetSize.Medium, w, h, mode)
        val primary = resolvePrimary(context, theme, config)
        val bg = remember(theme, mode, w, np?.trackId) {
            renderer.background(
                layout, theme, np?.art?.asImageBitmap(),
                np?.let { if (it.durationMs > 0) it.estimatedPositionMs().toFloat() / it.durationMs else 0f } ?: 0f,
                theme.colorScheme.primaryContainer,
            ).asImageBitmap()
        }
        val (ink, inkVariant) = when (val t = theme.playerTokens) {
            is VinylTokens -> t.widgetInk to t.widgetInkVariant
            is CassetteTokens -> t.widgetInk to t.widgetInkVariant
        }
        Box(Modifier.size(w.dp, h.dp).clip(RoundedCornerShape(24.dp))) {
            Image(bg, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            layout.text?.let { t ->
                Column(Modifier.offset(t.x.dp, t.y.dp).width(t.w.dp)) {
                    Text(
                        np?.title ?: stringResource(R.string.widget_nothing_playing),
                        color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        np?.let { listOf(it.artist, it.album).filter(String::isNotBlank).joinToString(" · ") }
                            ?: stringResource(R.string.widget_start_a_song),
                        color = inkVariant, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val playing = mode == WidgetMode.Playing
            for (spot in layout.controls) {
                val pressed = (spot.control == Control.PlayPause || spot.control == Control.Play) && playing
                val bmp = renderer.control(spot, theme, pressed, playing, primary, stringResource(spot.control.labelRes))
                Image(
                    bmp.asImageBitmap(), null,
                    Modifier
                        .offset((spot.face.x - spot.margin).dp, (spot.face.y - spot.margin).dp)
                        .size((spot.face.w + 2 * spot.margin).dp, (spot.face.h + 2 * spot.margin).dp),
                )
            }
            layout.openButton?.let { b ->
                Box(
                    Modifier
                        .offset(b.x.dp, b.y.dp)
                        .size(b.w.dp, b.h.dp)
                        .background(primary, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.widget_open_spotify),
                        color = if (theme.isDark) theme.colorScheme.onPrimary else Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}
