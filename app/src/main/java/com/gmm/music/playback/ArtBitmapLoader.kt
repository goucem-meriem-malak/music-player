package com.gmm.music.playback

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.gmm.music.data.ArtKey
import com.gmm.music.util.ArtworkLoader
import java.util.concurrent.Executors

/**
 * Supplies artwork for the notification and lock screen. Track artwork uses a private
 * "music-art" URI that points back at our own ArtworkLoader, so no network is ever used.
 */
class ArtBitmapLoader(private val loader: ArtworkLoader) : BitmapLoader {
    private val executor: ListeningExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        executor.submit<Bitmap> { BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("decode failed") }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = executor.submit<Bitmap> {
        val track = uri.getQueryParameter("u") ?: error("no artwork")
        val albumId = uri.getQueryParameter("a")?.toLongOrNull() ?: -1L
        loader.loadBlocking(ArtKey(track, albumId), 512) ?: error("no artwork")
    }

    companion object {
        fun artUri(trackUri: String, albumId: Long): Uri = Uri.Builder()
            .scheme("music-art").authority("track")
            .appendQueryParameter("u", trackUri)
            .appendQueryParameter("a", albumId.toString())
            .build()
    }
}
