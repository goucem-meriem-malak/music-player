# Security checklist

Status words: **Verified** = checked by reading/searching the project files. **Not verified** = needs a build or device.
**Not applicable** = does not exist in this app.

| Item | Status | Notes |
|---|---|---|
| No INTERNET permission in the manifest | Verified | Manifest lists only media, foreground-service, notification and wake-lock permissions |
| No hidden network code or URLs in source | Verified | Search for http/INTERNET found only the privacy text |
| No analytics, ads, crash-reporting or login SDKs | Verified | Dependency list is in app/build.gradle.kts |
| No Firebase, Room, SQLite or backend | Verified | |
| No secrets, keys or passwords in the repo | Verified | `.gitignore` also excludes keystores and env files |
| Cloud backup disabled | Verified | `allowBackup=false` and data-extraction rules exclude cloud backup |
| Minimal storage permissions | Verified | READ_MEDIA_AUDIO (Android 13+), READ_EXTERNAL_STORAGE up to Android 12; folder access through SAF |
| Original music files are never deleted | Verified | No delete code touches media files (only playlists/cache entries are removed) |
| Persistent data separate from cache | Verified | `filesDir/data` vs `cacheDir/artwork` |
| Atomic writes with backup for JSON data | Verified (code) / Not verified (device) | Temp file, fsync, atomic move, `.bak` fallback; unit test written, not run |
| Media service exported correctly | Not verified | Exported as required by Media3 controllers; check on device |
| Release build shrinks and obfuscates | Not verified | R8 enabled, never built |
| Dependency vulnerability scan | Not verified | Versions pinned in Gradle; not scanned |
| Accounts, payments, remote config | Not applicable | |
