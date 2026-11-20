package com.gmm.music.data.user

import com.gmm.music.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Favorites, play statistics and playlists. All of it lives in local JSON files. */
class UserDataRepository(private val store: JsonStore, private val scope: CoroutineScope) {
    private val statsFile = "listening.json"
    private val playlistsFile = "playlists.json"

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    private val _stats = MutableStateFlow<Map<String, PlayStat>>(emptyMap())
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val favorites: StateFlow<Set<String>> = _favorites
    val stats: StateFlow<Map<String, PlayStat>> = _stats
    val playlists: StateFlow<List<Playlist>> = _playlists

    private var statsJob: Job? = null
    private var playlistsJob: Job? = null

    init {
        try {
            store.read(statsFile)?.let { j ->
                val fav = j.optJSONArray("favorites")
                _favorites.value = if (fav == null) emptySet() else List(fav.length()) { fav.getString(it) }.toSet()
                val plays = j.optJSONObject("plays")
                val map = HashMap<String, PlayStat>()
                plays?.keys()?.forEach { k ->
                    val o = plays.getJSONObject(k)
                    map[k] = PlayStat(o.optInt("count"), o.optLong("last"))
                }
                _stats.value = map
            }
        } catch (_: Exception) { }
        try {
            store.read(playlistsFile)?.optJSONArray("playlists")?.let { arr ->
                _playlists.value = List(arr.length()) { i -> playlistFromJson(arr.getJSONObject(i)) }
            }
        } catch (_: Exception) { }
    }

    // ---- favorites & stats ----
    fun isFavorite(key: String) = key in _favorites.value

    fun toggleFavorite(key: String) {
        _favorites.value = if (key in _favorites.value) _favorites.value - key else _favorites.value + key
        saveStats()
    }

    fun recordPlay(key: String) {
        val old = _stats.value[key]
        _stats.value = _stats.value + (key to PlayStat((old?.count ?: 0) + 1, System.currentTimeMillis()))
        saveStats()
    }

    private fun saveStats() {
        statsJob?.cancel()
        statsJob = scope.launch(Dispatchers.IO) {
            delay(800)
            val j = JSONObject()
            j.put("favorites", JSONArray(_favorites.value.toList()))
            val plays = JSONObject()
            _stats.value.forEach { (k, v) -> plays.put(k, JSONObject().put("count", v.count).put("last", v.lastPlayedMs)) }
            j.put("plays", plays)
            try { store.write(statsFile, j) } catch (_: Exception) { }
        }
    }

    // ---- playlists ----
    fun createPlaylist(name: String, items: List<PlaylistItem> = emptyList()): Playlist {
        val now = System.currentTimeMillis()
        val p = Playlist(UUID.randomUUID().toString(), name.trim().ifEmpty { "Playlist" }, now, now, items)
        mutatePlaylists { it + p }
        return p
    }

    fun renamePlaylist(id: String, name: String) =
        mutatePlaylists { l -> l.map { if (it.id == id) it.copy(name = name.trim().ifEmpty { it.name }, updatedMs = System.currentTimeMillis()) else it } }

    fun deletePlaylist(id: String) = mutatePlaylists { l -> l.filterNot { it.id == id } }

    fun addToPlaylist(id: String, tracks: List<Track>) = mutatePlaylists { l ->
        l.map { p ->
            if (p.id != id) p else p.copy(items = p.items + tracks.map { it.toItem() }, updatedMs = System.currentTimeMillis())
        }
    }

    fun removeFromPlaylist(id: String, index: Int) = mutatePlaylists { l ->
        l.map { p ->
            if (p.id != id || index !in p.items.indices) p
            else p.copy(items = p.items.toMutableList().also { it.removeAt(index) }, updatedMs = System.currentTimeMillis())
        }
    }

    private fun mutatePlaylists(f: (List<Playlist>) -> List<Playlist>) {
        _playlists.value = f(_playlists.value)
        playlistsJob?.cancel()
        playlistsJob = scope.launch(Dispatchers.IO) {
            delay(500)
            val arr = JSONArray()
            _playlists.value.forEach { arr.put(playlistToJson(it)) }
            try { store.write(playlistsFile, JSONObject().put("version", 1).put("playlists", arr)) } catch (_: Exception) { }
        }
    }

    // ---- M3U ----
    fun importM3u(name: String, text: String, index: LibraryIndex): Playlist {
        val items = M3u.parse(text).map { e ->
            val stub = PlaylistItem("", e.path, e.title ?: e.path.substringAfterLast('/').substringBeforeLast('.'), e.artist.orEmpty(), maxOf(0, e.durationSec) * 1000L)
            val t = index.resolve(stub)
            if (t != null) t.toItem() else stub
        }
        return createPlaylist(name, items)
    }

    fun exportM3u(playlist: Playlist, index: LibraryIndex): String = M3u.write(playlist.items.map { item ->
        val t = index.resolve(item)
        M3uEntry(t?.path ?: item.path ?: item.uri, t?.title ?: item.title, t?.artist ?: item.artist, ((t?.durationMs ?: item.durationMs) / 1000).toInt())
    })

    private fun Track.toItem() = PlaylistItem(uri, path, title, artist, durationMs)

    private fun playlistToJson(p: Playlist) = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("created", p.createdMs); put("updated", p.updatedMs)
        put("items", JSONArray().also { arr ->
            p.items.forEach { i ->
                arr.put(JSONObject().put("uri", i.uri).put("path", i.path ?: JSONObject.NULL)
                    .put("title", i.title).put("artist", i.artist).put("dur", i.durationMs))
            }
        })
    }

    private fun playlistFromJson(j: JSONObject): Playlist {
        val arr = j.optJSONArray("items")
        return Playlist(
            id = j.getString("id"), name = j.optString("name", "Playlist"),
            createdMs = j.optLong("created"), updatedMs = j.optLong("updated"),
            items = if (arr == null) emptyList() else List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                PlaylistItem(o.optString("uri"), if (o.isNull("path")) null else o.optString("path"),
                    o.optString("title"), o.optString("artist"), o.optLong("dur"))
            },
        )
    }
}
