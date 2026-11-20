package com.goucem.aurora.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.goucem.aurora.AuroraApp
import com.goucem.aurora.MainActivity
import com.goucem.aurora.R
import kotlinx.coroutines.*
import kotlin.math.min

/**
 * Foreground media service. Owns the single ExoPlayer and MediaSession, which gives us
 * background playback, the media notification, lock-screen and Bluetooth controls.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var fadeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as AuroraApp).container

        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessorChain(DefaultAudioSink.DefaultAudioProcessorChain(container.effects.balanceProcessor))
                    .build()
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

        val exo = ExoPlayer.Builder(this, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        player = exo

        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) { container.effects.attach(audioSessionId) }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                fadeJob?.cancel()
                if (isPlaying) fadeJob = scope.launch { while (isActive) { applyFade(exo, container); delay(80) } }
            }
        })
        container.effects.attach(exo.audioSessionId)
        container.sleepTimer.attach(exo)

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, exo)
            .setBitmapLoader(ArtBitmapLoader(container.artwork))
            .setSessionActivity(openApp)
            .build()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this).apply { setSmallIcon(R.drawable.ic_stat_aurora) })
    }

    /** "Fade between songs": ramps the volume down at the end of a track and up at the start of the next. */
    private fun applyFade(exo: ExoPlayer, container: com.goucem.aurora.AppContainer) {
        val s = container.settings.value
        if (!s.crossfadeEnabled) {
            if (exo.volume != 1f) exo.volume = 1f
            return
        }
        val fadeMs = s.crossfadeSeconds * 1000L
        val dur = exo.duration
        if (dur == C.TIME_UNSET || dur < fadeMs * 3) {
            if (exo.volume != 1f) exo.volume = 1f
            return
        }
        val pos = exo.currentPosition
        val v = min(pos / fadeMs.toFloat(), (dur - pos) / fadeMs.toFloat()).coerceIn(0f, 1f)
        exo.volume = v * v * (3f - 2f * v)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        val container = (application as AuroraApp).container
        scope.cancel()
        container.sleepTimer.attach(null)
        container.effects.release()
        session?.release()
        session = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
