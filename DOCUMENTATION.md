# Music documentation

## Minimum Android version
**Android 8.0 (API 26).** Reason: adaptive icons, notification channels used by Media3, `java.nio` atomic file moves,
variable font weights. Target/compile SDK 35.

## Structure
```
app/src/main/java/com/goucem/music/
  MusicApp.kt           Application + AppContainer (one place that creates all singletons)
  MainActivity.kt        Edge-to-edge Compose host, notification permission
  data/                  Models, JsonStore (atomic JSON), settings/, library/ (MediaStore, SAF, FolderTree), user/ (favorites, playlists, M3U)
  playback/              PlaybackService, PlayerConnection, SleepTimer, ArtBitmapLoader
  audio/                 AudioEffectsManager, BalanceAudioProcessor, EqCurves + presets
  ui/                    MusicRoot (navigation, sheets), theme/, components/, screens/
  util/                  ArtworkLoader, Format
```

## Screens and navigation
Bottom bar: Home, Library, Search, Playlists. Library tabs: Songs, Albums, Artists, Genres, Folders, Favorites.
Detail: Collection (album/artist/genre/folder/favorites/recent lists), Playlist detail.
Full screen: Now Playing (swipe down or arrow to close), Queue, Equalizer, Audio settings, Settings, Sleep timer, Track information.
Empty, loading, permission and error states are in `LibraryGate` and `EmptyState`.

## Design
Deep indigo violet background with slightly lighter raised surfaces and soft drop shadows, near-black indigo round buttons with lilac icons,
a soft lavender accent for the current song and progress, and circular artwork. Colors live in `ui/theme/Theme.kt` (dark and light palettes).
The surface modifier is still called `glass`. Light theme is a soft grey-lavender. Font: Montserrat (bundled, SIL OFL).
"Soft shadows: Lite" in Settings removes shadows for slower phones. Now Playing adds a blurred artwork glow on Android 12+.
The footer is one slim bar in the layout (mini-player above the tabs), so content never goes under it.
Tab changes fade through with a small slide; detail screens slide in; Now Playing and Queue slide up.
Every screen paints its own background so pages never blend. The app opts out of the phone's forced dark mode so light theme keeps dark text.
Logo: a flat vinyl disc in the app's violet shades (`ic_launcher_*.xml`, README copy in `docs/images/logo.png`).

## Data and long-term independence
All files are in `filesDir/data`: `settings.json`, `playlists.json`, `listening.json` (favorites + play counts),
`queue.json`, `saf_cache.json`. Writes: temp file → fsync → atomic move, previous version kept as `.bak`. Artwork cache is in `cacheDir`.
Playlists can be exported as M3U8 at any time. Nothing depends on a server or account. The project needs only Android Studio,
Gradle and Maven-hosted libraries to rebuild; keep a copy of the Gradle cache for fully offline rebuilds.

## Playback notes
- One ExoPlayer lives in `PlaybackService`; the UI talks to it through a `MediaController`.
- Queue ends → playback stops. The saved queue is restored paused. Large lists are windowed to 1500 songs around the chosen one.
- A missing or damaged file shows a message and skips to the next song.
- Equalizer, BassBoost and LoudnessEnhancer attach to the player's audio session; unsupported effects are shown as unavailable.
- Balance is a small audio processor that works on 16-bit stereo output.

## Known limitations (honest list)
1. **Never compiled or run.** Expect to fix a few compile errors on first build.
2. Shadows below Android 9 are plain black (custom shadow colors need Android 9+). Artwork glow blur needs Android 12+.
3. "Fade between songs" is a volume fade-out/fade-in, **not** an overlapping crossfade (ExoPlayer plays one track at a time).
4. No ReplayGain/normalization by tags. "Loudness boost" (LoudnessEnhancer) is offered instead.
5. Genre tags from MediaStore need Android 11+. Folders you add with SAF read genre from the file tags.
6. Songs from SAF folders have no file path, so M3U export writes their content URI for those.
7. Playlists cannot be reordered by dragging yet (the queue can). Add/remove/rename/delete/import/export work.
8. Notification/lock-screen artwork depends on the artwork loader finding a cover; otherwise Android shows no art.
9. Only English strings ship. For Arabic: add `values-ar/strings.xml` and add `"ar"` to `resourceConfigurations` in `app/build.gradle.kts`.
10. Bit-perfect or Hi-Res output is **not** claimed. Android may resample for the output device.
11. The signed release APK uses the debug key (fine for personal use).

## Manual items for you
- Build the project, fix any compile errors, test on your phone.
- Optionally create your own signing key.
- Add screenshots to `docs/screenshots/`.
- Decide when (and if) to create the private GitHub repository and push.

## Third-party software
AndroidX (Compose, Navigation, Lifecycle, Activity, Core, Media3), Kotlin, kotlinx.coroutines, Guava: Apache License 2.0.
Montserrat font: SIL Open Font License 1.1 (see `docs/OFL-Montserrat.txt`). Test only: JUnit (EPL 1.0), org.json.
