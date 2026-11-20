Music - launch screen fix

What changed: the custom in-app splash (the slow, glitchy one) is gone. The app now uses Android's
official splash screen: your logo on the app color, the vinyl spins on Android 12+, and it closes
as soon as the app is ready (no fixed wait).

1) DELETE these two files from your project:
     app/src/main/java/com/gmm/music/ui/Splash.kt
     app/src/main/res/drawable/ic_splash_empty.xml
2) Copy the files in this zip into your project, same folders, replace when asked.
3) File > Sync Project with Gradle Files (one new small library: androidx.core:core-splashscreen), then Run.
   Uninstall the old app first if the old splash still shows.

If you edited MusicApp.kt yourself: just remove the two lines about "splashShown" in class AppContainer.
