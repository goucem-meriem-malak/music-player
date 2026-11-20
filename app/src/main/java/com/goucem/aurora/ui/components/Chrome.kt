package com.goucem.aurora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goucem.aurora.R
import com.goucem.aurora.ui.LocalContainer
import com.goucem.aurora.ui.rememberPlaybackPosition
import com.goucem.aurora.ui.theme.*

/** Floating mini-player. Tap to open Now Playing. */
@Composable
fun MiniPlayer(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val player = LocalContainer.current.player
    val state by player.state.collectAsStateWithLifecycle()
    val np = state.nowPlaying ?: return
    val pos by rememberPlaybackPosition(500L)
    val dur = player.durationMs().coerceAtLeast(1L)
    Box(
        modifier.fillMaxWidth().padding(horizontal = 12.dp).height(66.dp)
            .glass(RoundedCornerShape(24.dp), strong = true)
            .clickable(onClick = onOpen),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(np.art, Modifier.size(46.dp), sizePx = 128, shape = CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(np.title, style = AuroraType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artistLabel(np.artist), style = AuroraType.caption, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            KnobButton(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play), size = 44.dp) { player.togglePlay() }
            Spacer(Modifier.width(6.dp))
            BareIconButton(Icons.Rounded.SkipNext, stringResource(R.string.cd_next), tint = p.text) { player.next() }
        }
        Box(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp).fillMaxWidth().height(2.dp)
                .drawBehind {
                    drawRect(p.inset)
                    drawRect(p.accent, size = androidx.compose.ui.geometry.Size(size.width * (pos.toFloat() / dur).coerceIn(0f, 1f), size.height))
                },
        )
    }
}

data class NavItem(val route: String, val icon: ImageVector, val labelRes: Int)

/** Floating pill navigation. */
@Composable
fun AuroraBottomBar(items: List<NavItem>, currentRoute: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Row(
        modifier.padding(horizontal = 24.dp).fillMaxWidth().height(66.dp)
            .glass(CircleShape, strong = true).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            val label = stringResource(item.labelRes)
            Box(
                Modifier.weight(1f).height(50.dp).clip(CircleShape)
                    .then(if (selected) Modifier.background(Brush.linearGradient(listOf(p.knob, p.knob.copy(alpha = 0.9f)))) else Modifier)
                    .pressClickable { onSelect(item.route) },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(item.icon, label, tint = if (selected) p.accent else p.textSoft, modifier = Modifier.size(24.dp))
                    if (selected) Text(label, style = AuroraType.caption, color = p.knobIcon, maxLines = 1)
                }
            }
        }
    }
}
