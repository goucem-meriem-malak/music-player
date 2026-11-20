package com.goucem.aurora

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goucem.aurora.ui.AuroraRoot
import com.goucem.aurora.ui.LocalContainer
import com.goucem.aurora.ui.theme.AuroraTheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AuroraApp).container
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            val settings by container.settings.flow.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalContainer provides container) {
                AuroraTheme(settings.themeMode, settings.glassQuality, settings.reducedMotion, settings.haptics) {
                    val ctx = LocalContext.current
                    LaunchedEffect(Unit) { container.player.messages.collect { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }
                    AuroraRoot()
                }
            }
        }
    }
}
