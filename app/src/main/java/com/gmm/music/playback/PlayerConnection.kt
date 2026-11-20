package com.gmm.music.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.gmm.music.data.ArtKey
import com.gmm.music.data.JsonStore
import com.gmm.music.data.Track
import com.gmm.music.data.library.LibraryRepository
import com.gmm.music.data.library.LibraryStatus
import com.gmm.music.data.user.UserDataRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

data class NowPlaying(
    val uid: String,
    val key: String,
    val title: String,
    val artist: String,
    val album: String,
    val art: ArtKey,
    val durationMs: Long,
)

data class QueueItem(val uid: String, val key: String, val title: String, val artist: String, val art: ArtKey, val durationMs: Long)

data class PlayerState(
    val nowPlaying: NowPlaying? = null,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffle: Boolean = false,
    val speed: Float = 1f,
    val index: Int = -1,
    val queueSize: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

/** UI-facing wrapper around the MediaController. All calls are safe to make from the main thread. */
class PlayerConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val library: LibraryRepository,
    private val userData: UserDataRepository,
    private val store: JsonStore,
) {
    private val _state = MutableStateFlow(PlayerState())
    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val state: StateFlow<PlayerState> = _state
    val queue: StateFlow<List<QueueItem>> = _queue
    /** Short user-facing messages such as "File not found". */
    val messages: SharedFlow<String> = _messages

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private val pending = ArrayList<(MediaController) -> Unit>()
    private val uidCounter = AtomicLong(0)
    private var playCountJob: Job? = null
    private var saveJob: Job? = null
    private var consecutiveErrors = 0

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            this@PlayerConnection.controller = null
            future = null
            _state.value = PlayerState()
            _queue.value = emptyList()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            sync(player)
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) syncQueue(player)
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) scheduleSave(player)
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED)) scheduleCount(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            consecutiveErrors++
            val p = controller ?: return
            _messages.tryEmit("Could not play this file. It may be missing or damaged.")
            if (p.hasNextMediaItem() && consecutiveErrors < p.mediaItemCount.coerceAtLeast(1)) {
                p.seekToNextMediaItem(); p.prepare(); p.play()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) consecutiveErrors = 0
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).setListener(controllerListener).buildAsync()
        future = f
        f.addListener({
            try {
                val c = f.get()
                controller = c
                c.addListener(playerListener)
                sync(c); syncQueue(c)
                restoreQueueIfNeeded(c)
                val todo = pending.toList(); pending.clear()
                todo.forEach { it(c) }
            } catch (_: Exception) {
                future = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null && c.isConnected) block(c) else { pending += block; connect() }
    }

    // ---- reading state ----
    fun currentPositionMs(): Long = controller?.currentPosition ?: 0L
    fun durationMs(): Long = controller?.duration?.takeIf { it > 0 } ?: _state.value.nowPlaying?.durationMs ?: 0L

    private fun sync(p: Player) {
        val item = p.currentMediaItem
        val np = item?.let { toNowPlaying(it, p.duration) }
        _state.value = PlayerState(
            nowPlaying = np,
            isPlaying = p.isPlaying,
            buffering = p.playbackState == Player.STATE_BUFFERING,
            repeatMode = p.repeatMode,
            shuffle = p.shuffleModeEnabled,
            speed = p.playbackParameters.speed,
            index = p.currentMediaItemIndex,
            queueSize = p.mediaItemCount,
            hasNext = p.hasNextMediaItem(),
            hasPrevious = p.hasPreviousMediaItem(),
        )
    }

    private fun syncQueue(p: Player) {
        _queue.value = List(p.mediaItemCount) { i -> toQueueItem(p.getMediaItemAt(i)) }
    }

    private fun artOf(m: MediaItem): ArtKey {
        val x = m.mediaMetadata.extras
        return ArtKey(x?.getString("uri") ?: m.mediaId.substringBeforeLast("#q"), x?.getLong("albumId", -1L) ?: -1L)
    }

    private fun toNowPlaying(m: MediaItem, liveDuration: Long) = NowPlaying(
        uid = m.mediaId, key = m.mediaId.substringBeforeLast("#q"),
        title = m.mediaMetadata.title?.toString().orEmpty(), artist = m.mediaMetadata.artist?.toString().orEmpty(),
        album = m.mediaMetadata.albumTitle?.toString().orEmpty(), art = artOf(m),
        durationMs = if (liveDuration > 0) liveDuration else m.mediaMetadata.extras?.getLong("dur") ?: 0L,
    )

    private fun toQueueItem(m: MediaItem) = QueueItem(
        m.mediaId, m.mediaId.substringBeforeLast("#q"), m.mediaMetadata.title?.toString().orEmpty(),
        m.mediaMetadata.artist?.toString().orEmpty(), artOf(m), m.mediaMetadata.extras?.getLong("dur") ?: 0L,
    )

    private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId("$uri#q${uidCounter.incrementAndGet()}")
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title).setArtist(artist.ifBlank { null }).setAlbumTitle(album.ifBlank { null })
                .setArtworkUri(ArtBitmapLoader.artUri(uri, albumId))
                .setIsPlayable(true).setIsBrowsable(false)
                .setExtras(Bundle().apply { putString("uri", uri); putLong("albumId", albumId); putLong("dur", durationMs) })
                .build()
        ).build()

    // ---- commands ----
    /** Replaces the queue and starts playing. Very large lists are windowed around the chosen song. */
    fun play(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val window = 1500
        var list = tracks; var start = startIndex.coerceIn(0, tracks.lastIndex)
        if (tracks.size > window) {
            val from = (start - 200).coerceAtLeast(0)
            list = tracks.subList(from, (from + window).coerceAtMost(tracks.size))
            start -= from
        }
        val items = list.map { it.toMediaItem() }
        withController { c ->
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, if (shuffle) 0 else start, 0L)
            c.prepare(); c.play()
        }
    }

    fun shuffleAll(tracks: List<Track>) = play(tracks.shuffled(), 0, shuffle = true)

    fun playNext(track: Track) = withController { c ->
        if (c.mediaItemCount == 0) play(listOf(track))
        else { c.addMediaItem(c.currentMediaItemIndex + 1, track.toMediaItem()); _messages.tryEmit("Will play next") }
    }

    fun addToQueue(track: Track) = withController { c ->
        if (c.mediaItemCount == 0) play(listOf(track))
        else { c.addMediaItem(track.toMediaItem()); _messages.tryEmit("Added to queue") }
    }

    fun addAllToQueue(tracks: List<Track>) = withController { c ->
        if (c.mediaItemCount == 0) play(tracks) else { c.addMediaItems(tracks.map { it.toMediaItem() }); _messages.tryEmit("Added to queue") }
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            c.play()
        }
    }
    fun next() = withController { if (it.hasNextMediaItem()) it.seekToNextMediaItem() }
    fun previous() = withController { c -> if (c.currentPosition > 3000 || !c.hasPreviousMediaItem()) c.seekTo(0) else c.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun setSpeed(speed: Float) = withController { it.playbackParameters = PlaybackParameters(speed, 1f) }
    fun skipTo(index: Int) = withController { c -> if (index in 0 until c.mediaItemCount) { c.seekTo(index, 0L); c.play() } }
    fun moveItem(from: Int, to: Int) = withController { c -> if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to) }
    fun removeItem(index: Int) = withController { c -> if (index in 0 until c.mediaItemCount) c.removeMediaItem(index) }
    fun clearUpcoming() = withController { c ->
        val from = c.currentMediaItemIndex + 1
        if (from < c.mediaItemCount) c.removeMediaItems(from, c.mediaItemCount)
    }
    fun stopAndClear() = withController { c -> c.stop(); c.clearMediaItems() }

    // ---- statistics ----
    private fun scheduleCount(p: Player) {
        playCountJob?.cancel()
        val id = p.currentMediaItem?.mediaId ?: return
        if (!p.isPlaying) return
        playCountJob = scope.launch {
            delay(10_000)
            if (controller?.currentMediaItem?.mediaId == id && controller?.isPlaying == true) userData.recordPlay(id.substringBeforeLast("#q"))
        }
    }

    // ---- remember the queue between launches (restored paused, never auto-played) ----
    private val queueFile = "queue.json"

    private fun scheduleSave(p: Player) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1500)
            val c = controller ?: return@launch
            if (c.mediaItemCount == 0) { withContext(Dispatchers.IO) { try { store.write(queueFile, JSONObject()) } catch (_: Exception) { } }; return@launch }
            val keys = JSONArray(List(c.mediaItemCount) { c.getMediaItemAt(it).mediaId.substringBeforeLast("#q") })
            val j = JSONObject().put("keys", keys).put("index", c.currentMediaItemIndex).put("pos", c.currentPosition)
                .put("repeat", c.repeatMode).put("shuffle", c.shuffleModeEnabled)
            withContext(Dispatchers.IO) { try { store.write(queueFile, j) } catch (_: Exception) { } }
        }
    }

    private fun restoreQueueIfNeeded(c: MediaController) {
        if (c.mediaItemCount > 0) return
        scope.launch {
            val saved = withContext(Dispatchers.IO) { store.read(queueFile) } ?: return@launch
            val keys = saved.optJSONArray("keys") ?: return@launch
            if (keys.length() == 0) return@launch
            // wait (briefly) for the first library scan
            withTimeoutOrNull(15_000) { library.status.first { it == LibraryStatus.Ready } }
            val idx = library.index.value
            val items = ArrayList<MediaItem>()
            var mapped = -1
            val wanted = saved.optInt("index", 0)
            for (i in 0 until keys.length()) {
                val t = idx.byUri[keys.getString(i)] ?: continue
                if (i == wanted) mapped = items.size
                items += t.toMediaItem()
            }
            if (items.isEmpty() || c.mediaItemCount > 0) return@launch
            c.repeatMode = saved.optInt("repeat", Player.REPEAT_MODE_OFF)
            c.shuffleModeEnabled = saved.optBoolean("shuffle", false)
            c.setMediaItems(items, mapped.coerceAtLeast(0), saved.optLong("pos", 0L))
            c.prepare() // stays paused: no unwanted autoplay
        }
    }
}
