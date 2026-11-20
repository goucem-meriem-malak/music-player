package com.gmm.music.ui.screens

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmm.music.R
import com.gmm.music.ui.LocalContainer
import com.gmm.music.ui.components.*
import com.gmm.music.ui.theme.*
import com.gmm.music.util.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class TechInfo(val mime: String?, val sampleRate: Int?, val channels: Int?, val bitrate: Int?, val bitDepth: Int?)

@Composable
fun TrackInfoScreen(key: String, onBack: () -> Unit) {
    val c = LocalContainer.current
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val index by c.library.index.collectAsStateWithLifecycle()
    val track = index.byUri[key]
    val tech by produceState<TechInfo?>(null, key) {
        value = withContext(Dispatchers.IO) {
            val ex = MediaExtractor()
            try {
                ex.setDataSource(ctx, Uri.parse(key), null)
                val f = ex.getTrackFormat(0)
                fun int(k: String) = if (f.containsKey(k)) f.getInteger(k) else null
                TechInfo(
                    f.getString(MediaFormat.KEY_MIME), int(MediaFormat.KEY_SAMPLE_RATE), int(MediaFormat.KEY_CHANNEL_COUNT), int(MediaFormat.KEY_BIT_RATE),
                    if (f.containsKey(MediaFormat.KEY_PCM_ENCODING)) when (f.getInteger(MediaFormat.KEY_PCM_ENCODING)) { 2 -> 16; 21 -> 24; 22 -> 32; 4 -> 32; else -> null } else null,
                )
            } catch (_: Exception) { null } finally { try { ex.release() } catch (_: Exception) { } }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.menu_track_info), onBack)
        if (track == null) { EmptyState(Icons.Rounded.ErrorOutline, stringResource(R.string.file_not_found), ""); return@Column }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Artwork(track.art, Modifier.size(200.dp).glass(RoundedCornerShape(28.dp)), sizePx = 640, shape = RoundedCornerShape(28.dp))
            Spacer(Modifier.height(16.dp))
            Text(track.title, style = MusicType.title, color = p.text, modifier = Modifier.padding(horizontal = 24.dp))
            Text(artistLabel(track.artist), style = MusicType.body, color = p.textSoft)
            SettingGroup(stringResource(R.string.group_tags)) {
                Field(R.string.info_album, albumLabel(track.album))
                Field(R.string.info_genre, track.genre)
                Field(R.string.info_year, if (track.year > 0) track.year.toString() else "")
                Field(R.string.info_track_no, if (track.trackNumber > 0) track.trackNumber.toString() else "")
                Field(R.string.info_duration, Format.duration(track.durationMs))
            }
            SettingGroup(stringResource(R.string.group_file)) {
                Field(R.string.info_format, Format.extension(track.mimeType, track.path))
                Field(R.string.info_mime, tech?.mime ?: track.mimeType)
                Field(R.string.info_size, Format.sizeBytes(track.sizeBytes))
                Field(R.string.info_sample_rate, tech?.sampleRate?.let { "$it Hz" }.orEmpty())
                Field(R.string.info_channels, tech?.channels?.toString().orEmpty())
                Field(R.string.info_bitrate, tech?.bitrate?.let { "${it / 1000} kbps" }.orEmpty())
                Field(R.string.info_bit_depth, tech?.bitDepth?.let { "$it-bit" }.orEmpty())
                Field(R.string.info_location, track.path ?: track.uri)
            }
        }
    }
}

@Composable
private fun Field(labelRes: Int, value: String) {
    if (value.isBlank()) return
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text(stringResource(labelRes), style = MusicType.caption, color = p.textSoft, modifier = Modifier.width(110.dp))
        Text(value, style = MusicType.bodySoft, color = p.text, modifier = Modifier.weight(1f))
    }
}
