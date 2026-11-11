package com.retroplayer.core.history

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.retroplayer.core.media.MediaRepository
import com.retroplayer.core.media.NowPlaying
import com.retroplayer.core.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** One logged play. Stored only on the phone. */
@Entity(tableName = "plays", indices = [Index("artist", "album"), Index("startedAt")])
data class PlayEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String?,
    /** 300 px JPEG in filesDir/crate/, one per album. */
    val artThumbPath: String?,
    val startedAt: Long,
    val listenedMs: Long,
    val sourcePackage: String,
) {
    val albumKey: String get() = "$artist|${album.orEmpty()}"
}

@Dao
interface PlayDao {
    @Query("SELECT * FROM plays ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<PlayEntry>>

    @Insert
    suspend fun insert(entry: PlayEntry): Long

    @Query("UPDATE plays SET listenedMs = :listenedMs WHERE id = :id")
    suspend fun updateListened(id: Long, listenedMs: Long)

    @Query("DELETE FROM plays WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM plays WHERE artist = :artist AND IFNULL(album, '') = :album")
    suspend fun deleteAlbum(artist: String, album: String)

    @Query("DELETE FROM plays")
    suspend fun deleteAll()
}

@Database(entities = [PlayEntry::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun plays(): PlayDao
}

@Module
@InstallIn(SingletonComponent::class)
object HistoryModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): HistoryDatabase =
        Room.databaseBuilder(context, HistoryDatabase::class.java, "crate.db").build()

    @Provides
    fun dao(db: HistoryDatabase): PlayDao = db.plays()
}

/** Read and edit the crate. */
@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PlayDao,
) {
    val entries: Flow<List<PlayEntry>> = dao.observeAll()

    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun restore(entry: PlayEntry) { dao.insert(entry.copy(id = 0)) }
    suspend fun deleteAlbum(artist: String, album: String?) = dao.deleteAlbum(artist, album.orEmpty())

    suspend fun clear() {
        dao.deleteAll()
        File(context.filesDir, "crate").deleteRecursively()
    }
}

/**
 * Logs a track once it has been *playing* for more than 30 s (paused time doesn't count),
 * then keeps its listened time up to date. Respects "Keep listening history".
 * Start once from Application.onCreate.
 */
@Singleton
class HistoryLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val media: MediaRepository,
    private val settings: SettingsRepository,
    private val dao: PlayDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private var started = false

    private class Session(var track: NowPlaying, val startedAt: Long) {
        var accumulatedMs: Long = 0
        var playingSince: Long? = null
        var entryId: Long? = null
        var lastSavedMs: Long = 0

        fun listened(now: Long) = accumulatedMs + (playingSince?.let { now - it } ?: 0)
    }

    private var session: Session? = null
    private var enabled = true

    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(media.nowPlaying, settings.settings.map { it.keepHistory }.distinctUntilChanged()) { np, keep -> np to keep }
                .collect { (np, keep) -> lock.withLock { onState(np, keep) } }
        }
        scope.launch {
            while (true) {
                delay(5_000)
                lock.withLock { tick() }
            }
        }
    }

    private suspend fun onState(np: NowPlaying?, keep: Boolean) {
        val now = System.currentTimeMillis()
        enabled = keep
        val current = session
        if (current == null || np == null || current.track.trackId != np.trackId) {
            current?.let { flush(it, now) }
            session = np?.let { Session(it, now) }
        } else {
            current.track = np
        }
        val s = session ?: return
        if (s.track.isPlaying && s.playingSince == null) s.playingSince = now
        if (!s.track.isPlaying && s.playingSince != null) {
            s.accumulatedMs += now - s.playingSince!!
            s.playingSince = null
        }
        tick()
    }

    private suspend fun tick() {
        val s = session ?: return
        val now = System.currentTimeMillis()
        val listened = s.listened(now)
        if (!enabled) return
        if (s.entryId == null && listened > THRESHOLD_MS) {
            s.entryId = dao.insert(
                PlayEntry(
                    title = s.track.title,
                    artist = s.track.artist,
                    album = s.track.album.ifBlank { null },
                    artThumbPath = saveThumb(s.track),
                    startedAt = s.startedAt,
                    listenedMs = listened,
                    sourcePackage = s.track.sourcePackage,
                ),
            )
            s.lastSavedMs = listened
        } else if (s.entryId != null && listened - s.lastSavedMs >= 15_000) {
            dao.updateListened(s.entryId!!, listened)
            s.lastSavedMs = listened
        }
    }

    private suspend fun flush(s: Session, now: Long) {
        val id = s.entryId ?: run { tick(); s.entryId } ?: return
        dao.updateListened(id, s.listened(now))
    }

    /** One 300 px JPEG per album in filesDir/crate/. */
    private fun saveThumb(np: NowPlaying): String? {
        val art = np.art ?: return null
        val dir = File(context.filesDir, "crate").apply { mkdirs() }
        val file = File(dir, "${np.albumKey.hashCode().toUInt()}.jpg")
        if (!file.exists()) {
            val max = 300
            val f = max.toFloat() / maxOf(art.width, art.height)
            val scaled = if (f < 1f) {
                Bitmap.createScaledBitmap(art, (art.width * f).toInt().coerceAtLeast(1), (art.height * f).toInt().coerceAtLeast(1), true)
            } else {
                art
            }
            runCatching { file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) } }
                .onFailure { return null }
        }
        return file.absolutePath
    }

    companion object {
        const val THRESHOLD_MS = 30_000L
    }
}
