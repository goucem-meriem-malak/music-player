package com.gmm.music.ui.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import com.gmm.music.R
import com.gmm.music.data.settings.GlassQuality
import com.gmm.music.data.settings.ThemeMode

/** All colors used by the app. Dark = the vivid violet soft-shadow look; Light = soft lavender. */
@Immutable
data class MusicPalette(
    val isDark: Boolean,
    val bgTop: Color, val bgMid: Color, val bgBottom: Color,
    val glowA: Color, val glowB: Color,
    val glassTop: Color, val glassBottom: Color,
    val glassStrongTop: Color, val glassStrongBottom: Color,
    val borderStart: Color, val borderEnd: Color,
    val barFill: Color,
    val text: Color, val textSoft: Color, val textFaint: Color,
    val accent: Color, val accentSoft: Color,
    val knob: Color, val knobIcon: Color,
    val heart: Color,
    /** Pressed-in surfaces such as slider tracks. */
    val inset: Color,
    /** Dark side of the soft drop shadows. */
    val shadow: Color,
)

val DarkPalette = MusicPalette(
    isDark = true,

    bgTop = Color(0xFF211B40),
    bgMid = Color(0xFF292250),
    bgBottom = Color(0xFF332961),

    glowA = Color(0xFF6255A0),
    glowB = Color(0xFF705B9D),

    glassTop = Color(0xFF352B65),
    glassBottom = Color(0xFF2A2354),

    glassStrongTop = Color(0xFF403477),
    glassStrongBottom = Color(0xFF342B65),

    borderStart = Color.White.copy(alpha = 0.13f),
    borderEnd = Color.Transparent,

    barFill = Color(0xFF51418A),

    text = Color(0xFFFFFFFF),
    textSoft = Color(0xFFD0CBE4).copy(alpha = 0.80f),
    textFaint = Color(0xFFB4ACD1).copy(alpha = 0.58f),

    accent = Color(0xFF9B8CFF),
    accentSoft = Color(0xFFB9B0F2),

    knob = Color(0xFF171332),
    knobIcon = Color(0xFFDCD8FF),

    heart = Color(0xFFE45A78),

    inset = Color(0xFF1E1945),
    shadow = Color(0xFF100D2C),
)

val LightPalette = MusicPalette(
    isDark = false,

    bgTop = Color(0xFFD5D0E7),
    bgMid = Color(0xFFC9C3DF),
    bgBottom = Color(0xFFBEB6D8),

    glowA = Color(0xFFA69ACB),
    glowB = Color(0xFF9889C2),

    glassTop = Color(0xFFE0DCEC),
    glassBottom = Color(0xFFD5D0E5),

    glassStrongTop = Color(0xFFE9E5F0),
    glassStrongBottom = Color(0xFFDCD6EA),

    borderStart = Color.White.copy(alpha = 0.48f),
    borderEnd = Color.Transparent,

    barFill = Color(0xFFD0C9E2),

    text = Color(0xFF211A3D),
    textSoft = Color(0xFF39304F).copy(alpha = 0.78f),
    textFaint = Color(0xFF514866).copy(alpha = 0.60f),

    accent = Color(0xFF6957C8),
    accentSoft = Color(0xFF8978D8),

    knob = Color(0xFF493B91),
    knobIcon = Color(0xFFFFFFFF),

    heart = Color(0xFFD94F70),

    inset = Color(0xFFB5ACD2),
    shadow = Color(0xFF81749F),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }
val LocalGlassQuality = staticCompositionLocalOf { GlassQuality.Full }
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

@OptIn(ExperimentalTextApi::class)
private val montserrat = FontFamily(
    Font(R.font.montserrat, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.montserrat, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.montserrat, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.montserrat, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private fun ts(size: Int, weight: FontWeight, line: Int, spacing: Float = 0f) = TextStyle(
    fontFamily = montserrat, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp,
)

object MusicType {
    val display = ts(30, FontWeight.Bold, 36, -0.4f)
    val title = ts(22, FontWeight.Bold, 28)
    val heading = ts(17, FontWeight.SemiBold, 22)
    val body = ts(15, FontWeight.Medium, 20)
    val bodySoft = ts(14, FontWeight.Normal, 19)
    val caption = ts(12, FontWeight.Medium, 16, 0.2f)
    val label = ts(11, FontWeight.SemiBold, 14, 0.6f)
    val wordmark = ts(20, FontWeight.Bold, 24, 6f)
}

@Composable
fun systemReducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        try { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f } catch (_: Exception) { false }
    }
}

@Composable
fun MusicTheme(
    mode: ThemeMode, quality: GlassQuality, reducedMotion: Boolean, haptics: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) { ThemeMode.System -> isSystemInDarkTheme(); ThemeMode.Dark -> true; ThemeMode.Light -> false }
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme(primary = p.accent, onPrimary = Color.White, background = p.bgMid, surface = p.bgMid, onSurface = p.text)
    else lightColorScheme(primary = p.accent, onPrimary = Color.White, background = p.bgMid, surface = p.bgMid, onSurface = p.text)
    val typo = Typography(
        bodyLarge = MusicType.body, bodyMedium = MusicType.bodySoft, titleLarge = MusicType.title,
        titleMedium = MusicType.heading, labelLarge = MusicType.body, labelMedium = MusicType.caption,
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val ic = WindowCompat.getInsetsController(window, view)
                ic.isAppearanceLightStatusBars = !dark
                ic.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(
        LocalPalette provides p, LocalGlassQuality provides quality,
        LocalReducedMotion provides (reducedMotion || systemReducedMotion()), LocalHapticsEnabled provides haptics,
    ) { MaterialTheme(colorScheme = scheme, typography = typo, content = content) }
}

/**
 * Raised soft surface (the app's "glass" call name is kept so every screen picks up the new style):
 * a slightly lighter violet face, a soft dark drop shadow and a faint light edge on the top-left.
 * "Lite" mode drops the shadow for slower phones.
 */
@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp), strong: Boolean = false): Modifier {
    val p = LocalPalette.current
    val lite = LocalGlassQuality.current == GlassQuality.Lite
    val top = if (strong) p.glassStrongTop else p.glassTop
    val bottom = if (strong) p.glassStrongBottom else p.glassBottom
    val lifted = if (lite) this else this.shadow(if (strong) 10.dp else 6.dp, shape, clip = false, ambientColor = p.shadow, spotColor = p.shadow)
    return lifted
        .clip(shape)
        .background(Brush.linearGradient(listOf(top, bottom), Offset.Zero, Offset(500f, 500f)))
        .border(1.dp, Brush.linearGradient(listOf(p.borderStart, p.borderEnd), Offset.Zero, Offset(260f, 260f)), shape)
}

/** The ambient background: violet gradient with two soft light blooms. */
@Composable
fun MusicBackground(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable BoxScope.() -> Unit = {}) {
    val p = LocalPalette.current
    Box(
        modifier
            .background(Brush.verticalGradient(listOf(p.bgTop, p.bgMid, p.bgBottom)))
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(p.glowA.copy(alpha = if (p.isDark) 0.22f else 0.45f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.08f), radius = size.width * 0.9f))
                drawRect(Brush.radialGradient(listOf(p.glowB.copy(alpha = if (p.isDark) 0.10f else 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.72f), radius = size.width * 0.85f))
            },
    ) { content() }
}
