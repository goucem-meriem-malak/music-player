package com.goucem.aurora.data.user

data class M3uEntry(val path: String, val title: String?, val artist: String?, val durationSec: Int)

/** Minimal, forgiving M3U / M3U8 reader and writer (UTF-8). */
object M3u {
    fun parse(text: String): List<M3uEntry> {
        val out = ArrayList<M3uEntry>()
        var title: String? = null
        var artist: String? = null
        var dur = -1
        for (raw in text.removePrefix("\uFEFF").lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val body = line.substring(8)
                val comma = body.indexOf(',')
                dur = (if (comma >= 0) body.substring(0, comma) else body).trim().substringBefore(' ').toIntOrNull() ?: -1
                val label = if (comma >= 0) body.substring(comma + 1).trim() else ""
                val dash = label.indexOf(" - ")
                if (dash > 0) { artist = label.substring(0, dash).trim(); title = label.substring(dash + 3).trim() }
                else { artist = null; title = label.ifEmpty { null } }
            } else if (line.startsWith("#")) {
                continue
            } else {
                var p = line
                if (p.startsWith("file://", ignoreCase = true)) p = java.net.URLDecoder.decode(p.substring(7).replace("+", "%2B"), "UTF-8")
                out += M3uEntry(p.replace('\\', '/'), title, artist, dur)
                title = null; artist = null; dur = -1
            }
        }
        return out
    }

    fun write(entries: List<M3uEntry>): String = buildString {
        append("#EXTM3U\n")
        for (e in entries) {
            val label = if (!e.artist.isNullOrBlank()) "${e.artist} - ${e.title.orEmpty()}" else e.title.orEmpty()
            append("#EXTINF:").append(e.durationSec).append(',').append(label.replace('\n', ' ')).append('\n')
            append(e.path).append('\n')
        }
    }
}
