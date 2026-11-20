package com.gmm.music.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmm.music.R
import com.gmm.music.ui.LocalContainer
import com.gmm.music.ui.components.*
import com.gmm.music.ui.theme.*
import com.gmm.music.util.Format
import kotlinx.coroutines.delay

@Composable
fun SleepTimerScreen(onBack: () -> Unit) {
    val c = LocalContainer.current
    val p = LocalPalette.current
    val st by c.sleepTimer.state.collectAsStateWithLifecycle()
    val remaining by produceState(0L, st) {
        while (st.endAtElapsedMs > 0) { value = (st.endAtElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0); delay(500) }
        value = 0
    }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopBar(stringResource(R.string.sleep_title), onBack)
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(120.dp).glass(androidx.compose.foundation.shape.CircleShape, strong = true), contentAlignment = Alignment.Center) {
                Text(
                    when { st.endOfTrack -> "⏹"; st.endAtElapsedMs > 0 -> Format.duration(remaining); else -> "--:--" },
                    style = MusicType.title, color = p.text,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when { st.endOfTrack -> stringResource(R.string.sleep_end_of_track_active); st.endAtElapsedMs > 0 -> stringResource(R.string.sleep_running); else -> stringResource(R.string.sleep_idle) },
                style = MusicType.bodySoft, color = p.textSoft,
            )
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(GridCells.Fixed(3), modifier = Modifier.heightIn(max = 220.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf(5, 10, 15, 30, 45, 60, 90, 120, 180)) { m ->
                    Box(Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp)).pressClickable { c.sleepTimer.startMinutes(m); onBack() }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.minutes_fmt, m), style = MusicType.body, color = p.text)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SecondaryButton(stringResource(R.string.sleep_end_of_track), Modifier.fillMaxWidth(), icon = Icons.Rounded.Bedtime) { c.sleepTimer.startEndOfTrack(); onBack() }
            if (st.active) {
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.sleep_cancel), Modifier.fillMaxWidth(), icon = Icons.Rounded.Close) { c.sleepTimer.cancel() }
            }
        }
    }
}
