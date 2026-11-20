package com.goucem.aurora.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import com.goucem.aurora.data.ArtKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads album artwork: memory cache -> disk cache (cacheDir, safe to clear) -> MediaStore thumbnail ->
 * embedded picture. Artwork is never written to the persistent data folder.
 */
class ArtworkLoader(private val context: Context) {
    private val memory = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 12).toInt().coerceAtMost(48 * 1024 * 1024)) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val noArt: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val gate = Semaphore(3)
    private val dir = File(context.cacheDir, "artwork").also { it.mkdirs() }

    private fun cacheKey(art: ArtKey, size: Int) = "${art.uri}@$size"

    fun peek(art: ArtKey, size: Int): Bitmap? = memory.get(cacheKey(art, size))

    suspend fun load(art: ArtKey, size: Int): Bitmap? {
        val key = cacheKey(art, size)
        memory.get(key)?.let { return it }
        if (key in noArt) return null
        return withContext(Dispatchers.IO) { gate.withPermit { loadSync(art, size, key) } }
    }

    fun loadBlocking(art: ArtKey, size: Int): Bitmap? = runBlocking { load(art, size) }

    private fun loadSync(art: ArtKey, size: Int, key: String): Bitmap? {
        memory.get(key)?.let { return it }
        val file = File(dir, hash(key) + ".jpg")
        var bmp: Bitmap? = null
        if (file.exists()) bmp = try { BitmapFactory.decodeFile(file.path) } catch (_: Exception) { null }
        if (bmp == null) {
            bmp = fromThumbnail(art, size) ?: fromAlbumArtUri(art, size) ?: fromEmbedded(art, size)
            if (bmp != null) {
                try {
                    val out = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    file.writeBytes(out.toByteArray())
                } catch (_: Exception) { }
            }
        }
        if (bmp == null) noArt += key else memory.put(key, bmp)
        return bmp
    }

    private fun fromThumbnail(art: ArtKey, size: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < 29 || !art.uri.startsWith("content://media/")) return null
        return try { context.contentResolver.loadThumbnail(Uri.parse(art.uri), Size(size, size), null) } catch (_: Exception) { null }
    }

    private fun fromAlbumArtUri(art: ArtKey, size: Int): Bitmap? {
        if (art.albumId < 0) return null
        return try {
            val uri = Uri.parse("content://media/external/audio/albumart/${art.albumId}")
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.let { decode(it, size) }
        } catch (_: Exception) { null }
    }

    private fun fromEmbedded(art: ArtKey, size: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, Uri.parse(art.uri))
            r.embeddedPicture?.let { decode(it, size) }
        } catch (_: Exception) { null } finally {
            try { r.release() } catch (_: Exception) { }
        }
    }

    private fun decode(bytes: ByteArray, size: Int): Bitmap? {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        var sample = 1
        while (o.outWidth / (sample * 2) >= size && o.outHeight / (sample * 2) >= size) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun hash(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).take(10).joinToString("") { "%02x".format(it) }

    fun clearDiskCache() { dir.listFiles()?.forEach { it.delete() }; noArt.clear() }
}
