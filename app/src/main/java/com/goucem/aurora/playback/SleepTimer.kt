package com.goucem.aurora.playback

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SleepState(val endAtElapsedMs: Long = 0L, val endOfTrack: Boolean = false) {
    val active: Boolean get() = endAtElapsedMs > 0L || endOfTrack
}

/** Pauses playback after a delay, or at the end of the current track. Runs in the app process. */
class SleepTimer(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state

    private var player: ExoPlayer? = null
    private var job: Job? = null

    private val listener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && _state.value.endOfTrack) {
                player?.pauseAtEndOfMediaItems = false
                _state.value = SleepState()
            }
        }
    }

    fun attach(p: ExoPlayer?) {
        player?.removeListener(listener)
        player = p
        p?.addListener(listener)
    }

    fun startMinutes(minutes: Int) {
        cancel()
        val end = SystemClock.elapsedRealtime() + minutes * 60_000L
        _state.value = SleepState(endAtElapsedMs = end)
        job = scope.launch {
            while (SystemClock.elapsedRealtime() < end) delay(1000)
            player?.pause()
            _state.value = SleepState()
        }
    }

    fun startEndOfTrack() {
        cancel()
        player?.pauseAtEndOfMediaItems = true
        _state.value = SleepState(endOfTrack = true)
    }

    fun cancel() {
        job?.cancel()
        job = null
        player?.pauseAtEndOfMediaItems = false
        _state.value = SleepState()
    }
}
