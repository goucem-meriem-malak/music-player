package com.gmm.music

import android.app.Application
import com.gmm.music.audio.AudioEffectsManager
import com.gmm.music.data.JsonStore
import com.gmm.music.data.library.LibraryRepository
import com.gmm.music.data.library.SafScanner
import com.gmm.music.data.settings.SettingsRepository
import com.gmm.music.data.user.UserDataRepository
import com.gmm.music.playback.PlayerConnection
import com.gmm.music.playback.SleepTimer
import com.gmm.music.util.ArtworkLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Simple service locator. Everything is created once and lives as long as the process. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    /** Persistent user data (settings, playlists, favorites). Never mixed with cache. */
    val store = JsonStore(File(app.filesDir, "data"))
    val settings = SettingsRepository(store, scope)
    val userData = UserDataRepository(store, scope)
    val artwork = ArtworkLoader(app)
    val library = LibraryRepository(app, settings, SafScanner(app, store), scope)
    val effects = AudioEffectsManager(settings, scope)
    val sleepTimer = SleepTimer(scope)
    val player = PlayerConnection(app, scope, library, userData, store)
}

class MusicApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.library.refresh()
        container.player.connect()
        if (container.settings.value.playbackSpeed != 1f) container.player.setSpeed(container.settings.value.playbackSpeed)
    }
}
