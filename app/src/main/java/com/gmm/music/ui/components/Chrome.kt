package com.gmm.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmm.music.R
import com.gmm.music.ui.LocalContainer
import com.gmm.music.ui.rememberPlaybackPosition
import com.gmm.music.ui.theme.*

data class NavItem(val route: String, val icon: ImageVector, val labelRes: Int)

/**
 * One slim bar docked to the bottom of the screen: the mini-player (when something is loaded)
 * above the navigation row. It sits in the layout, so content ends above it and is never hidden.
 */
@Composable
fun DockedBar(
    showMini: Boolean, showNav: Boolean, items: List<NavItem>, currentRoute: String?,
    onOpenNow: () -> Unit, onSelect: (String) -> Unit, modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().glass(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), strong = true).navigationBarsPadding(),
    ) {
        if (showMini) MiniRow(onOpenNow)
        if (showNav) NavRow(items, currentRoute, onSelect)
    }
}

@Composable
private fun MiniRow(onOpen: () -> Unit) {
    val p = LocalPalette.current
    val player = LocalContainer.current.player
    val state by player.state.collectAsStateWithLifecycle()
    val np = state.nowPlaying ?: return
    val pos by rememberPlaybackPosition(500L)
    val dur = player.durationMs().coerceAtLeast(1L)
    Box(Modifier.fillMaxWidth().height(54.dp).clickable(onClick = onOpen)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(np.art, Modifier.size(38.dp), sizePx = 128, shape = CircleShape)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(np.title, style = MusicType.body, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artistLabel(np.artist), style = MusicType.caption, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            KnobButton(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(if (state.isPlaying) R.string.cd_pause else R.string.cd_play), size = 38.dp) { player.togglePlay() }
            Spacer(Modifier.width(4.dp))
            BareIconButton(Icons.Rounded.SkipNext, stringResource(R.string.cd_next), size = 38.dp, tint = p.text) { player.next() }
        }
        Box(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp).fillMaxWidth().height(2.dp).drawBehind {
                drawRect(p.inset)
                drawRect(p.accent, size = Size(size.width * (pos.toFloat() / dur).coerceIn(0f, 1f), size.height))
            },
        )
    }
}

@Composable
private fun NavRow(items: List<NavItem>, currentRoute: String?, onSelect: (String) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            val label = stringResource(item.labelRes)
            Box(Modifier.weight(1f).fillMaxHeight().pressClickable { onSelect(item.route) }, contentAlignment = Alignment.Center) {
                if (selected) {
                    Row(
                        Modifier.height(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(p.knob, p.knob.copy(alpha = 0.92f)))).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(item.icon, label, tint = p.accent, modifier = Modifier.size(22.dp))
                        Text(label, style = MusicType.caption, color = p.knobIcon, maxLines = 1)
                    }
                } else Icon(item.icon, label, tint = p.textSoft, modifier = Modifier.size(24.dp))
            }
        }
    }
}
