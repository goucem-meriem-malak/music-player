package com.goucem.aurora.data.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.goucem.aurora.data.JsonStore
import com.goucem.aurora.data.Track
import org.json.JSONObject
import kotlin.math.abs

/**
 * Finds audio files in folders the user picked with the Storage Access Framework.
 * Tag data is read once and cached in a JSON file, so rescans stay fast.
 */
class SafScanner(private val context: Context, private val store: JsonStore) {
    private val cacheFile = "saf_cache.json"
    private val audioExt = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "oga", "wma")

    private data class Doc(val uri: Uri, val name: String, val modified: Long, val size: Long, val folder: String)

    fun scan(treeUriString: String, treeName: String, knownNameSize: Set<String>, minDurationMs: Long): List<Track> {
        val tree = Uri.parse(treeUriString)
        val docs = ArrayList<Doc>()
        try {
            walk(tree, DocumentsContract.getTreeDocumentId(tree), treeName.ifEmpty { "Folder" }, 0, docs)
        } catch (_: Exception) {
            return emptyList()
        }
        val cache = store.read(cacheFile) ?: JSONObject()
        val newCache = JSONObject()
        val out = ArrayList<Track>()
        for (d in docs) {
            if ("${d.name}|${d.size}" in knownNameSize) continue // already provided by MediaStore
            val key = d.uri.toString()
            val cached = cache.optJSONObject(key)
            val meta: JSONObject = if (cached != null && cached.optLong("modified") == d.modified) cached else extract(d) ?: continue
            newCache.put(key, meta)
            val dur = meta.optLong("dur")
            if (dur < minDurationMs) continue
            out += Track(
                id = -(abs(key.hashCode().toLong())) - 1,
                uri = key,
                title = meta.optString("title").ifBlank { d.name.substringBeforeLast('.') },
                artist = meta.optString("artist"),
                album = meta.optString("album"),
                albumId = -1,
                artistId = -1,
                durationMs = dur,
                path = null,
                folder = d.folder,
                dateAddedSec = d.modified / 1000,
                trackNumber = meta.optInt("track"),
                year = meta.optInt("year"),
                genre = meta.optString("genre"),
                mimeType = meta.optString("mime"),
                sizeBytes = d.size,
            )
        }
        try { store.write(cacheFile, mergeCache(newCache)) } catch (_: Exception) { }
        return out
    }

    private val seen = JSONObject()
    private fun mergeCache(add: JSONObject): JSONObject {
        add.keys().forEach { seen.put(it, add.get(it)) }
        return seen
    }

    private fun walk(tree: Uri, docId: String, folder: String, depth: Int, out: MutableList<Doc>) {
        if (depth > 12) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        context.contentResolver.query(children, cols, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0); val name = c.getString(1) ?: continue; val mime = c.getString(2) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walk(tree, id, "$folder/$name", depth + 1, out)
                } else if (mime.startsWith("audio/") || name.substringAfterLast('.', "").lowercase() in audioExt) {
                    out += Doc(DocumentsContract.buildDocumentUriUsingTree(tree, id), name, c.getLong(3), c.getLong(4), folder)
                }
            }
        }
    }

    private fun extract(d: Doc): JSONObject? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, d.uri)
            fun m(k: Int) = r.extractMetadata(k).orEmpty()
            JSONObject().apply {
                put("modified", d.modified)
                put("title", m(MediaMetadataRetriever.METADATA_KEY_TITLE))
                put("artist", m(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                put("album", m(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                put("genre", m(MediaMetadataRetriever.METADATA_KEY_GENRE))
                put("mime", m(MediaMetadataRetriever.METADATA_KEY_MIMETYPE))
                put("dur", m(MediaMetadataRetriever.METADATA_KEY_DURATION).toLongOrNull() ?: 0L)
                put("track", m(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER).substringBefore('/').toIntOrNull() ?: 0)
                put("year", m(MediaMetadataRetriever.METADATA_KEY_YEAR).take(4).toIntOrNull() ?: 0)
            }
        } catch (_: Exception) {
            null // corrupted or unreadable file: skip it
        } finally {
            try { r.release() } catch (_: Exception) { }
        }
    }
}
