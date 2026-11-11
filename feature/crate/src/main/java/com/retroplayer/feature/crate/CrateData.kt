package com.retroplayer.feature.crate

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.retroplayer.core.history.PlayEntry
import com.retroplayer.core.theme.AlbumPalettes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/** One album on the shelf, aggregated from its plays. */
@Immutable
data class AlbumGroup(
    val key: String,
    val album: String,
    val artist: String,
    val thumbPath: String?,
    val plays: Int,
    val lastPlayed: Long,
    val firstPlayed: Long,
    val listenedMs: Long,
    val tracks: List<TrackCount>,
    val entries: List<PlayEntry>,
)

@Immutable
data class TrackCount(val title: String, val plays: Int, val entries: List<PlayEntry>)

/** Albums, most recently played first. */
fun groupAlbums(entries: List<PlayEntry>): List<AlbumGroup> =
    entries.groupBy { it.albumKey }
        .map { (key, plays) ->
            AlbumGroup(
                key = key,
                album = plays.first().album.orEmpty(),
                artist = plays.first().artist,
                thumbPath = plays.firstNotNullOfOrNull { it.artThumbPath },
                plays = plays.size,
                lastPlayed = plays.maxOf { it.startedAt },
                firstPlayed = plays.minOf { it.startedAt },
                listenedMs = plays.sumOf { it.listenedMs },
                tracks = plays.groupBy { it.title }
                    .map { (t, e) -> TrackCount(t, e.size, e) }
                    .sortedByDescending { it.plays },
                entries = plays,
            )
        }
        .sortedByDescending { it.lastPlayed }

enum class StatsRange { Week, Month }

@Immutable
data class CrateStats(
    val totalMs: Long,
    val tracks: Int,
    val topAlbum: AlbumGroup?,
    val topArtist: Pair<String, Int>?,
    val topArtistThumb: String?,
    val topTrack: Triple<String, String, Int>?,
    val topTrackThumb: String?,
)

fun stats(entries: List<PlayEntry>, range: StatsRange, now: Long = System.currentTimeMillis()): CrateStats {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (range == StatsRange.Week) set(Calendar.DAY_OF_WEEK, firstDayOfWeek) else set(Calendar.DAY_OF_MONTH, 1)
    }
    val inRange = entries.filter { it.startedAt >= cal.timeInMillis }
    val albums = groupAlbums(inRange)
    val artist = inRange.groupBy { it.artist }.maxByOrNull { it.value.size }
    val track = inRange.groupBy { it.title to it.artist }.maxByOrNull { it.value.size }
    return CrateStats(
        totalMs = inRange.sumOf { it.listenedMs },
        tracks = inRange.size,
        topAlbum = albums.maxByOrNull { it.plays },
        topArtist = artist?.let { it.key to it.value.size },
        topArtistThumb = artist?.value?.firstNotNullOfOrNull { it.artThumbPath },
        topTrack = track?.let { Triple(it.key.first, it.key.second, it.value.size) },
        topTrackThumb = track?.value?.firstNotNullOfOrNull { it.artThumbPath },
    )
}

fun formatDuration(ms: Long): String {
    val minutes = ms / 60_000
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** Thumbnail from filesDir/crate (decoded off the main thread). */
@Composable
fun rememberThumb(path: String?): State<ImageBitmap?> = produceState<ImageBitmap?>(null, path) {
    value = path?.let { p -> withContext(Dispatchers.IO) { BitmapFactory.decodeFile(p)?.asImageBitmap() } }
}

/** Spine colour: the album's palette tint (falls back to a neutral brown). */
@Composable
fun rememberTint(group: AlbumGroup): State<Color> = produceState(Color(0xFF8A6A50), group.key, group.thumbPath) {
    val path = group.thumbPath ?: return@produceState
    value = withContext(Dispatchers.IO) {
        BitmapFactory.decodeFile(path)?.let { AlbumPalettes.derive(it, group.key).tint }
    } ?: value
}
