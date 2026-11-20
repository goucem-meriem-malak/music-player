package com.gmm.music.data.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.gmm.music.data.*
import com.gmm.music.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class LibraryStatus { Idle, NeedsPermission, Scanning, Ready, Failed }

class LibraryRepository(
    private val context: Context,
    private val settings: SettingsRepository,
    private val saf: SafScanner,
    private val scope: CoroutineScope,
) {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    private val _status = MutableStateFlow(LibraryStatus.Idle)
    val tracks: StateFlow<List<Track>> = _tracks
    val status: StateFlow<LibraryStatus> = _status

    val index: StateFlow<LibraryIndex> = _tracks.map { LibraryIndex(it) }
        .flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, LibraryIndex(emptyList()))
    val albums: StateFlow<List<Album>> = _tracks.map { buildAlbums(it) }
        .flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())
    val artists: StateFlow<List<Artist>> = _tracks.map { buildArtists(it) }
        .flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())
    val genres: StateFlow<List<Genre>> = _tracks.map { list ->
        list.filter { it.genre.isNotBlank() }.groupBy { it.genre }
            .map { (g, t) -> Genre(g, t.sortedBy { it.title.lowercase() }) }.sortedBy { it.name.lowercase() }
    }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val mutex = Mutex()
    private var observerJob: Job? = null
    private var observerRegistered = false

    init {
        // Re-scan automatically when the folder list or the minimum length changes.
        scope.launch {
            settings.flow.map { it.minTrackSeconds to it.extraFolders }.distinctUntilChanged().drop(1).collect { refresh() }
        }
    }

    fun hasPermission(): Boolean {
        val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) { mutex.withLock { scan() } }
    }

    private fun registerObserver() {
        if (observerRegistered) return
        observerRegistered = true
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    observerJob?.cancel()
                    observerJob = scope.launch { delay(2000); refresh() }
                }
            },
        )
    }

    private fun scan() {
        val s = settings.value
        val permitted = hasPermission()
        if (_tracks.value.isEmpty()) _status.value = if (permitted) LibraryStatus.Scanning else LibraryStatus.NeedsPermission
        try {
            val minMs = s.minTrackSeconds * 1000L
            val store = if (permitted) queryMediaStore(minMs) else emptyList()
            val known = HashSet<String>()
            store.forEach { known += "${it.path?.substringAfterLast('/').orEmpty()}|${it.sizeBytes}" }
            val extra = s.extraFolders.flatMap { saf.scan(it.uri, it.name, known, minMs) }
            _tracks.value = (store + extra).sortedBy { it.title.lowercase() }
            _status.value = if (!permitted && extra.isEmpty()) LibraryStatus.NeedsPermission else LibraryStatus.Ready
            if (permitted) registerObserver()
        } catch (e: Exception) {
            _status.value = LibraryStatus.Failed
        }
    }

    @Suppress("DEPRECATION")
    private fun queryMediaStore(minDurationMs: Long): List<Track> {
        val base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val proj = ArrayList<String>().apply {
            addAll(listOf(
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.YEAR, MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DISPLAY_NAME,
            ))
            if (Build.VERSION.SDK_INT >= 30) add(MediaStore.Audio.Media.GENRE)
        }.toTypedArray()
        val out = ArrayList<Track>()
        context.contentResolver.query(
            base, proj, "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?",
            arrayOf(minDurationMs.coerceAtLeast(1).toString()), null,
        )?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val iArtistId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iData = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val iMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val iSize = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val iGenre = if (Build.VERSION.SDK_INT >= 30) c.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
            while (c.moveToNext()) {
                try {
                    val id = c.getLong(iId)
                    val path = c.getString(iData)
                    val name = c.getString(iName).orEmpty()
                    out += Track(
                        id = id,
                        uri = ContentUris.withAppendedId(base, id).toString(),
                        title = clean(c.getString(iTitle)).ifBlank { name.substringBeforeLast('.') },
                        artist = clean(c.getString(iArtist)),
                        album = clean(c.getString(iAlbum)),
                        albumId = c.getLong(iAlbumId),
                        artistId = c.getLong(iArtistId),
                        durationMs = c.getLong(iDur),
                        path = path,
                        folder = path?.substringBeforeLast('/', "").orEmpty(),
                        dateAddedSec = c.getLong(iAdded),
                        trackNumber = c.getInt(iTrack) % 1000,
                        year = c.getInt(iYear),
                        genre = if (iGenre >= 0) clean(c.getString(iGenre)) else "",
                        mimeType = c.getString(iMime).orEmpty(),
                        sizeBytes = c.getLong(iSize),
                    )
                } catch (_: Exception) {
                    // one broken row must never break the whole library
                }
            }
        }
        return out
    }

    private fun clean(s: String?): String {
        val v = s?.trim().orEmpty()
        return if (v.equals("<unknown>", true) || v.equals("unknown", true)) "" else v
    }

    private fun buildAlbums(list: List<Track>): List<Album> =
        list.groupBy { if (it.albumId >= 0) "id:${it.albumId}" else "name:${it.album.lowercase()}" }
            .filter { (_, t) -> t.first().album.isNotBlank() || t.first().albumId >= 0 }
            .map { (key, t) ->
                val sorted = t.sortedWith(compareBy({ it.trackNumber }, { it.title.lowercase() }))
                val artists = sorted.map { it.artist }.filter { it.isNotBlank() }.distinct()
                Album(key, sorted.first().albumId, sorted.first().album, if (artists.size > 1) "Various artists" else artists.firstOrNull().orEmpty(),
                    sorted.maxOf { it.year }, sorted)
            }.sortedBy { it.name.lowercase() }

    private fun buildArtists(list: List<Track>): List<Artist> =
        list.filter { it.artist.isNotBlank() }.groupBy { it.artist.lowercase() }.map { (k, t) ->
            Artist(k, t.first().artist, t.map { it.albumId.toString() + it.album }.distinct().size, t.sortedBy { it.title.lowercase() })
        }.sortedBy { it.name.lowercase() }
}
