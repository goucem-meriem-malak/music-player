package com.gmm.music.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gmm.music.R
import com.gmm.music.ui.theme.LocalReducedMotion
import com.gmm.music.ui.theme.MusicType
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated start screen: the app's gradient, the logo scaling in while the vinyl spins, then the
 * name fading up. It fades out into the app. Always dark, like the app's main look.
 */
@Composable
fun SplashOverlay(onFinished: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val appear = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val out = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        if (reduced) {
            appear.snapTo(1f); title.snapTo(1f)
            delay(500)
        } else {
            coroutineScope {
                launch { appear.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
                launch { spin.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
                launch { delay(450); title.animateTo(1f, tween(500)) }
            }
            delay(250)
        }
        out.animateTo(0f, tween(350))
        onFinished()
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = out.value }
            .background(Brush.verticalGradient(listOf(Color(0xFF211B40), Color(0xFF292250), Color(0xFF332961))))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                Modifier.size(260.dp).graphicsLayer {
                    val s = 0.72f + 0.28f * appear.value
                    scaleX = s; scaleY = s
                    alpha = appear.value.coerceIn(0f, 1f)
                },
            ) { drawLogo(spin.value * 540f) }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.wordmark), style = MusicType.wordmark, color = Color.White,
                modifier = Modifier.graphicsLayer { alpha = title.value; translationY = (1f - title.value) * 18f },
            )
        }
    }
}

/** The vinyl-disc logo, drawn with the same shapes as the app icon. [rotation] spins only the disc. */
private fun DrawScope.drawLogo(rotation: Float) {
    val d = size.minDimension * (150f / 260f)
    val left = (size.width - d) / 2f
    val top = (size.height - d) / 2f
    val u = d / 108f
    val c = Offset(size.width / 2f, size.height / 2f)

    // soft glow behind the logo
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF9B8CFF).copy(alpha = 0.28f), Color.Transparent), center = c, radius = size.minDimension / 2f),
        radius = size.minDimension / 2f, center = c,
    )

    // round badge with the long flat shadow
    val badge = Path().apply { addOval(Rect(left, top, left + d, top + d)) }
    clipPath(badge) {
        drawRect(Color(0xFF3A2F78), topLeft = Offset(left, top), size = Size(d, d))
        val k = 0.7071f; val r = 28f; val len = 90f
        val shadow = Path().apply {
            moveTo(left + (54f + r * k) * u, top + (54f - r * k) * u)
            lineTo(left + (54f + r * k + len) * u, top + (54f - r * k + len) * u)
            lineTo(left + (54f - r * k + len) * u, top + (54f + r * k + len) * u)
            lineTo(left + (54f - r * k) * u, top + (54f + r * k) * u)
            close()
        }
        drawPath(shadow, Color(0xFF241C55))
    }

    // spinning disc
    rotate(rotation, pivot = c) {
        drawCircle(Color(0xFF171332), 28f * u, c)
        val arcTopLeft = Offset(c.x - 28f * u, c.y - 28f * u)
        val arcSize = Size(56f * u, 56f * u)
        drawArc(Color(0xFF4B3F96), 195f, 55f, true, arcTopLeft, arcSize)
        drawArc(Color(0xFF4B3F96), 15f, 55f, true, arcTopLeft, arcSize)
        drawCircle(Color(0xFF211A4A).copy(alpha = 0.6f), 23f * u, c)
    }

    // centre label stays still
    drawCircle(Color(0xFF6F5FD0), 9.5f * u, c)
    val lc = Offset(c.x - 0.2f * u, c.y - 1f * u)
    drawCircle(Color(0xFF9B8CFF), 8.2f * u, lc)
    drawCircle(Color(0xFFEEEAFF), 3.8f * u, lc)
}
