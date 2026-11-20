package com.gmm.music.data

/** Identifies the artwork source of a track. */
data class ArtKey(val uri: String, val albumId: Long)

data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val durationMs: Long,
    val path: String?,
    /** Folder as a "/"-separated path (MediaStore path, or "<picked folder>/<subfolders>" for SAF). */
    val folder: String,
    val dateAddedSec: Long,
    val trackNumber: Int,
    val year: Int,
    val genre: String,
    val mimeType: String,
    val sizeBytes: Long,
) {
    val key: String get() = uri
    val art: ArtKey get() = ArtKey(uri, albumId)
    val searchText: String by lazy(LazyThreadSafetyMode.NONE) { "$title $artist $album".lowercase() }
}

data class Album(
    val key: String,
    val id: Long,
    val name: String,
    val artist: String,
    val year: Int,
    val tracks: List<Track>,
) {
    val durationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class Artist(
    val key: String,
    val name: String,
    val albumCount: Int,
    val tracks: List<Track>,
)

data class Genre(val name: String, val tracks: List<Track>)

data class PlayStat(val count: Int, val lastPlayedMs: Long)

data class PlaylistItem(
    val uri: String,
    val path: String?,
    val title: String,
    val artist: String,
    val durationMs: Long,
)

data class Playlist(
    val id: String,
    val name: String,
    val createdMs: Long,
    val updatedMs: Long,
    val items: List<PlaylistItem>,
)

/** Fast lookups used to resolve playlist items and saved queues back to library tracks. */
class LibraryIndex(tracks: List<Track>) {
    val byUri: Map<String, Track> = tracks.associateBy { it.uri }
    val byPath: Map<String, Track> = tracks.mapNotNull { t -> t.path?.let { it to t } }.toMap()
    private val byName: Map<String, List<Track>> = tracks.filter { it.path != null }
        .groupBy { it.path!!.substringAfterLast('/').lowercase() }

    fun resolve(item: PlaylistItem): Track? {
        byUri[item.uri]?.let { return it }
        item.path?.let { p ->
            byPath[p]?.let { return it }
            val name = p.substringAfterLast('/').substringAfterLast('\\').lowercase()
            byName[name]?.let { list ->
                if (list.size == 1) return list[0]
                list.firstOrNull { it.title.equals(item.title, true) }?.let { return it }
            }
        }
        return null
    }
}
