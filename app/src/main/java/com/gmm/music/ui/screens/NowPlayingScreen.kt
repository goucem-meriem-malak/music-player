package com.gmm.music.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.gmm.music.R
import com.gmm.music.ui.*
import com.gmm.music.ui.components.*
import com.gmm.music.ui.theme.*
import com.gmm.music.util.Format

@Composable
fun NowPlayingScreen(onClose: () -> Unit, onQueue: () -> Unit, onEqualizer: () -> Unit, onSleep: () -> Unit) {
    val c = LocalContainer.current
    val ui = LocalUi.current
    val p = LocalPalette.current
    val reduced = LocalReducedMotion.current
    val player = c.player
    val state by player.state.collectAsStateWithLifecycle()
    val settings by c.settings.flow.collectAsStateWithLifecycle()
    val favs by c.userData.favorites.collectAsStateWithLifecycle()
    val np = state.nowPlaying
    val pos by rememberPlaybackPosition(250L)
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val sleep by c.sleepTimer.state.collectAsStateWithLifecycle()

    if (np == null) { LaunchedEffect(Unit) { onClose() }; return }
    val duration = player.durationMs().coerceAtLeast(1L)
    val shownPos = if (dragging) dragValue.toLong() else pos
    val isFav = np.key in favs
    val artScale by animateFloatAsState(if (state.isPlaying || reduced) 1f else 0.9f, tween(350), label = "art")

    // Ambient background from the artwork (real blur on Android 12+, plain gradient otherwise).
    val bg by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, np.art, settings.ambientBackground) {
        value = if (settings.ambientBackground) c.artwork.load(np.art, 128)?.asImageBitmap() else null
    }
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            var total = 0f
            detectVerticalDragGestures(onDragStart = { total = 0f }, onDragEnd = { if (total > 220f) onClose() }) { _, dy -> total += dy }
        },
    ) {
        val b = bg
        if (b != null && android.os.Build.VERSION.SDK_INT >= 31) {
            androidx.compose.foundation.Image(b, null, Modifier.fillMaxSize().blur(70.dp).alpha(0.55f), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, p.bgBottom.copy(alpha = 0.75f)))))
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.cd_close), onClick = onClose)
                Text(stringResource(R.string.now_playing), Modifier.weight(1f), style = MusicType.heading, color = p.text, textAlign = TextAlign.Center)
                GlassIconButton(Icons.Rounded.Info, stringResource(R.string.menu_track_info)) { ui.openTrackInfo(np.key) }
            }
            Spacer(Modifier.weight(0.5f))
            Box(Modifier.fillMaxWidth(0.86f).aspectRatio(1f)) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer { scaleX = artScale; scaleY = artScale }
                        .shadow(20.dp, androidx.compose.foundation.shape.CircleShape, clip = false, ambientColor = p.shadow, spotColor = p.shadow)
                        .background(Brush.linearGradient(listOf(p.knob, p.knob.copy(red = p.knob.red * 0.75f, green = p.knob.green * 0.75f))), androidx.compose.foundation.shape.CircleShape)
                        .padding(14.dp),
                ) { Artwork(np.art, Modifier.fillMaxSize(), sizePx = 768, shape = androidx.compose.foundation.shape.CircleShape) }
                Box(Modifier.align(Alignment.BottomEnd).padding(4.dp)) {
                    GlassIconButton(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        stringResource(if (isFav) R.string.menu_unfavorite else R.string.menu_favorite), tint = if (isFav) p.heart else p.text) { c.userData.toggleFavorite(np.key) }
                }
            }
            Spacer(Modifier.weight(0.5f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(np.title, style = MusicType.title, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                Text(artistLabel(np.artist), style = MusicType.body, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(14.dp))
            GlassSlider(
                value = shownPos.toFloat(), valueRange = 0f..duration.toFloat(), description = stringResource(R.string.cd_seek),
                onValueChange = { dragging = true; dragValue = it },
                onChangeFinished = { if (dragging) { player.seekTo(dragValue.toLong()); dragging = false } },
            )
            Row(Modifier.fillMaxWidth()) {
                Text(Format.duration(shownPos), style = MusicType.caption, color = p.textSoft)
                Spacer(Modifier.weight(1f))
                Text(Format.duration(duration), style = MusicType.caption, color = p.textSoft)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                BareIconButton(Icons.Rounded.Shuffle, stringResource(R.string.cd_shuffle), 48.dp, tint = if (state.shuffle) p.accent else p.textSoft) { player.toggleShuffle() }
                KnobButton(Icons.Rounded.SkipPrevious, stringResource(R.string.cd_previous), 56.dp) { player.previous() }
                KnobButton(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play), 76.dp, accent = true) { player.togglePlay() }
                KnobButton(Icons.Rounded.SkipNext, stringResource(R.string.cd_next), 56.dp) { player.next() }
                BareIconButton(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    stringResource(R.string.cd_repeat), 48.dp, tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) p.textSoft else p.accent) { player.cycleRepeat() }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                BareIconButton(Icons.Rounded.QueueMusic, stringResource(R.string.queue_title), 48.dp, tint = p.text, onClick = onQueue)
                BareIconButton(Icons.Rounded.Equalizer, stringResource(R.string.equalizer_title), 48.dp, tint = p.text, onClick = onEqualizer)
                val speeds = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
                Text(Format.speed(state.speed), style = MusicType.body, color = if (state.speed == 1f) p.text else p.accent,
                    modifier = Modifier.pressClickable { player.setSpeed(speeds[(speeds.indexOfFirst { it >= state.speed - 0.01f }.let { if (it < 0) 0 else it } + 1) % speeds.size]) }.padding(12.dp))
                BareIconButton(Icons.Rounded.Bedtime, stringResource(R.string.sleep_title), 48.dp, tint = if (sleep.active) p.accent else p.text, onClick = onSleep)
                BareIconButton(Icons.Rounded.PlaylistAdd, stringResource(R.string.menu_add_playlist), 48.dp, tint = p.text) {
                    c.library.index.value.byUri[np.key]?.let { ui.pickPlaylist(listOf(it)) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
