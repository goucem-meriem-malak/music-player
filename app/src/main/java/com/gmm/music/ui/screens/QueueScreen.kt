package com.gmm.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.gmm.music.R
import com.gmm.music.ui.*
import com.gmm.music.ui.components.*
import com.gmm.music.ui.theme.*
import com.gmm.music.util.Format
import kotlin.math.roundToInt

@Composable
fun QueueScreen(onBack: () -> Unit) {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val player = c.player
    val queue by player.queue.collectAsStateWithLifecycle()
    val state by player.state.collectAsStateWithLifecycle()
    val rowPx = with(LocalDensity.current) { 68.dp.toPx() }
    var dragUid by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val currentQueue by rememberUpdatedState(queue)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.queue_title), onBack) {
            BareIconButton(Icons.Rounded.Shuffle, stringResource(R.string.cd_shuffle), tint = if (state.shuffle) p.accent else p.text) { player.toggleShuffle() }
            BareIconButton(if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, stringResource(R.string.cd_repeat),
                tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) p.text else p.accent) { player.cycleRepeat() }
            BareIconButton(Icons.Rounded.DeleteSweep, stringResource(R.string.queue_clear), tint = p.text) { confirmClear = true }
        }
        if (queue.isEmpty()) {
            EmptyState(Icons.Rounded.QueueMusic, stringResource(R.string.queue_empty_title), stringResource(R.string.queue_empty_message))
        } else {
            Text("${stringResource(R.string.queue_hint)} · ${queue.size}", style = MusicType.caption, color = p.textFaint, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                itemsIndexed(queue, key = { _, q -> q.uid }) { index, q ->
                    val isCurrent = index == state.index
                    val dragging = dragUid == q.uid
                    Row(
                        Modifier.fillMaxWidth().height(68.dp).zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                            .then(if (isCurrent) Modifier.padding(horizontal = 8.dp).glass(RoundedCornerShape(18.dp)) else Modifier)
                            .clickable { player.skipTo(index) }.padding(horizontal = if (isCurrent) 8.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(q.art, Modifier.size(48.dp), sizePx = 128, shape = androidx.compose.foundation.shape.CircleShape)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(q.title, style = MusicType.body, color = if (isCurrent) p.accent else p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${artistLabel(q.artist)} · ${Format.duration(q.durationMs)}", style = MusicType.caption, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        BareIconButton(Icons.Rounded.Close, stringResource(R.string.remove_from_queue)) { player.removeItem(index) }
                        Box(
                            Modifier.size(44.dp).pointerInput(q.uid) {
                                detectDragGestures(
                                    onDragStart = { dragUid = q.uid; dragOffset = 0f },
                                    onDragEnd = { dragUid = null; dragOffset = 0f },
                                    onDragCancel = { dragUid = null; dragOffset = 0f },
                                ) { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val steps = (dragOffset / rowPx).roundToInt()
                                    if (steps != 0) {
                                        val from = currentQueue.indexOfFirst { it.uid == q.uid }
                                        val to = (from + steps).coerceIn(0, currentQueue.lastIndex)
                                        if (from >= 0 && to != from) { player.moveItem(from, to); dragOffset -= (to - from) * rowPx }
                                    }
                                }
                            },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.DragHandle, stringResource(R.string.cd_reorder), tint = p.textSoft) }
                    }
                }
            }
        }
    }
    if (confirmClear) GlassDialog(
        stringResource(R.string.queue_clear), { confirmClear = false }, stringResource(R.string.queue_clear_confirm),
        onConfirm = { confirmClear = false; player.stopAndClear(); onBack() },
        message = stringResource(R.string.queue_clear_message), destructive = true,
    )
}
