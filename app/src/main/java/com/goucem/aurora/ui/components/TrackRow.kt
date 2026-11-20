package com.goucem.aurora.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goucem.aurora.R
import com.goucem.aurora.data.Track
import com.goucem.aurora.ui.LocalUi
import com.goucem.aurora.ui.theme.AuroraType
import com.goucem.aurora.ui.theme.LocalPalette
import com.goucem.aurora.util.Format

@Composable
fun artistLabel(artist: String): String = artist.ifBlank { stringResource(R.string.unknown_artist) }

@Composable
fun albumLabel(album: String): String = album.ifBlank { stringResource(R.string.unknown_album) }

/** A song row. No card, no divider: just artwork, text and a menu. */
@Composable
fun TrackRow(
    track: Track, isCurrent: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    subtitle: String? = null, dimmed: Boolean = false,
) {
    val p = LocalPalette.current
    val ui = LocalUi.current
    Row(
        modifier.fillMaxWidth().alpha(if (dimmed) 0.5f else 1f)
            .clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(track.art, Modifier.size(52.dp), sizePx = 128, shape = CircleShape)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = AuroraType.body, color = if (isCurrent) p.accent else p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle ?: artistLabel(track.artist), style = AuroraType.caption, color = p.textSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isCurrent) Icon(Icons.Rounded.GraphicEq, null, tint = p.accent, modifier = Modifier.padding(horizontal = 6.dp).size(20.dp))
        Text(Format.duration(track.durationMs), style = AuroraType.caption, color = p.textFaint)
        BareIconButton(Icons.Rounded.MoreVert, stringResource(R.string.cd_more_options)) { ui.openTrackMenu(track) }
    }
}
