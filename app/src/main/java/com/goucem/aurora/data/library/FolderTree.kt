package com.goucem.aurora.data.library

import com.goucem.aurora.data.Track

/** Builds a browsable folder hierarchy from the track list (no file system access needed). */
object FolderTree {
    data class FolderEntry(val name: String, val path: String, val trackCount: Int)
    data class Listing(val path: String, val folders: List<FolderEntry>, val tracks: List<Track>)

    fun normalize(folder: String): String = folder.trim('/')

    fun list(tracks: List<Track>, path: String): Listing {
        val base = normalize(path)
        val direct = ArrayList<Track>()
        val counts = LinkedHashMap<String, Int>()
        for (t in tracks) {
            val f = normalize(t.folder)
            if (f == base) {
                direct += t
            } else if (base.isEmpty() || f.startsWith("$base/")) {
                val rest = if (base.isEmpty()) f else f.substring(base.length + 1)
                val seg = rest.substringBefore('/')
                if (seg.isNotEmpty()) counts[seg] = (counts[seg] ?: 0) + 1
            }
        }
        val folders = counts.entries.map { (name, n) ->
            FolderEntry(name, if (base.isEmpty()) name else "$base/$name", n)
        }.sortedBy { it.name.lowercase() }
        return Listing(base, folders, direct.sortedBy { it.title.lowercase() })
    }

    /** First folder that actually contains something to choose from (skips "storage/emulated/0"-style chains). */
    fun startPath(tracks: List<Track>): String {
        var path = ""
        while (true) {
            val l = list(tracks, path)
            if (l.tracks.isEmpty() && l.folders.size == 1) path = l.folders[0].path else return path
        }
    }

    fun parent(path: String): String = normalize(path).substringBeforeLast('/', "")

    fun allTracksUnder(tracks: List<Track>, path: String): List<Track> {
        val base = normalize(path)
        return tracks.filter { val f = normalize(it.folder); f == base || base.isEmpty() || f.startsWith("$base/") }
            .sortedBy { it.title.lowercase() }
    }
}
