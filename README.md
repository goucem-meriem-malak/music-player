# Music

**Your music. Your control. No compromises.**

Music (project name "Aurora", package `com.goucem.aurora`) is a fully offline music player for Android with a soft-shadow violet design.
No accounts, no ads, no tracking, no internet permission.

> **Honest status (v1.0.0):** the full source code is written and statically reviewed, but it has
> **not been compiled, installed, or run yet** (the build environment had no Android SDK).
> Please follow SETUP.md, build it, and test on your phone. See "Testing status" below.

## Features
- Local library from MediaStore (internal storage and SD card) plus folders you pick yourself
- Songs, albums, artists, genres, folder browsing, favorites, recently added/played, most played
- Playback with AndroidX Media3 / ExoPlayer: gapless, shuffle, repeat, speed, seek, no unwanted autoplay
- Queue: play next, add to queue, drag to reorder, remove, clear. Last queue is restored (paused)
- Playlists saved locally, with M3U / M3U8 import and export
- Equalizer (device bands, 13 presets, custom presets), bass enhancement, loudness boost, balance
- Optional fade between songs, sleep timer (minutes or end of song)
- Background playback, media notification, lock-screen and Bluetooth controls, audio focus, headphone-unplug pause
- Soft-shadow violet interface (round dark buttons, pink accent, circular artwork), light and dark themes, reduced-motion and haptics settings, English with Arabic/RTL-ready structure

## Screenshots
Add your own after the first build: `docs/screenshots/home.png`, `now-playing.png`, `equalizer.png`.

## Technology
Kotlin, Jetpack Compose (Material 3 components only where needed), AndroidX Media3 (ExoPlayer + MediaSession),
MediaStore, Storage Access Framework, Kotlin Coroutines and Flow, JSON files (org.json) for data.
No Room, no SQLite, no backend, no Firebase, no analytics.

## Architecture (short)
```
ui/          Compose screens, components, theme (glass design tokens)
playback/    PlaybackService (ExoPlayer + MediaSession), PlayerConnection (MediaController), SleepTimer
audio/       AudioEffectsManager (Equalizer, BassBoost, LoudnessEnhancer), BalanceAudioProcessor, EQ presets
data/        Models, library (MediaStore + SAF scan), user data (favorites, stats, playlists, M3U), settings, JsonStore
util/        ArtworkLoader (memory + disk cache), formatting
```
See DOCUMENTATION.md for details.

## Testing status
| Item | Status |
|---|---|
| Source written | Done |
| Static review (braces, resource names, permissions, no network/secrets) | Done |
| Gradle build | **Not run** |
| Unit tests (written: M3U, folder tree, EQ curves, JSON store, formatting) | **Not run** |
| Playback, notification, Bluetooth, equalizer on a real device | **Not tested** |

## License
Proprietary. All rights reserved by goucm-meriem-malak. See LICENSE.
