package com.retroplayer.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.retroplayer.core.media.MediaRepository
import com.retroplayer.core.settings.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun media(): MediaRepository
    fun settings(): SettingsRepository
}

/** Same widget, three picker entries so each size gets its own default size and preview. */
class SmallWidgetReceiver : RetroWidgetReceiver()
class MediumWidgetReceiver : RetroWidgetReceiver()
class LargeWidgetReceiver : RetroWidgetReceiver()

abstract class RetroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RetroWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val settings = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).settings()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { settings.clearWidgetConfig(it) }
            } finally {
                pending.finish()
            }
        }
    }
}

/** Knob / key taps: send the command to the active media session. */
class MediaCommandAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val media = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).media()
        withContext(Dispatchers.Main) {
            media.refreshAccess()
            when (parameters[Command]) {
                PLAY -> media.play()
                PAUSE -> media.pause()
                NEXT -> media.next()
                PREVIOUS -> media.previous()
            }
        }
    }

    companion object {
        val Command = ActionParameters.Key<String>("command")
        const val PLAY = "play"
        const val PAUSE = "pause"
        const val NEXT = "next"
        const val PREVIOUS = "previous"
    }
}

/**
 * Pushes widget updates from the listener,
 * plus a slow progress refresh every 30 s while playing. Start once from Application.onCreate.
 */
@Singleton
class WidgetSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val media: MediaRepository,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ticker: Job? = null
    private var started = false

    @OptIn(FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(media.nowPlaying, settings.settings) { np, s ->
                // Only what changes the widget's look; position is handled by the ticker.
                listOf(np?.trackId, np?.isPlaying, np?.art?.generationId, s.theme, s.lightDark, np == null)
            }
                .distinctUntilChanged()
                .debounce(300)
                .collect {
                    update()
                    val playing = media.nowPlaying.value?.isPlaying == true
                    ticker?.cancel()
                    if (playing) {
                        ticker = scope.launch {
                            while (isActive) {
                                delay(30_000)
                                update()
                            }
                        }
                    }
                }
        }
    }

    suspend fun update() {
        runCatching { RetroWidget().updateAll(context) }
    }
}
