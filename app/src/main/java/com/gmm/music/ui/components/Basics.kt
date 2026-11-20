package com.gmm.music.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gmm.music.R
import com.gmm.music.data.ArtKey
import com.gmm.music.ui.LocalContainer
import com.gmm.music.ui.theme.*

@Composable
fun rememberHaptic(): () -> Unit {
    val view = LocalView.current
    val enabled = LocalHapticsEnabled.current
    return remember(view, enabled) { { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) } }
}

@Composable
fun Modifier.pressClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val haptic = rememberHaptic()
    val reduced = LocalReducedMotion.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed && !reduced) 0.94f else 1f, tween(90), label = "press")
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(enabled) {
            detectTapGestures(onPress = { pressed = true; tryAwaitRelease(); pressed = false }, onTap = { if (enabled) { haptic(); onClick() } })
        }
}

@Composable
fun Artwork(art: ArtKey?, modifier: Modifier = Modifier, sizePx: Int = 256, shape: Shape = RoundedCornerShape(14.dp)) {
    val loader = LocalContainer.current.artwork
    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = art?.let { loader.peek(it, sizePx)?.asImageBitmap() }, art, sizePx,
    ) {
        value = if (art == null) null else loader.load(art, sizePx)?.asImageBitmap()
    }
    val p = LocalPalette.current
    Box(
        modifier.clip(shape).background(Brush.linearGradient(listOf(p.glowA.copy(alpha = 0.55f), p.knob.copy(alpha = 0.85f)))),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.MusicNote, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.fillMaxSize(0.42f))
    }
}

/** Round dark indigo button with a soft shadow, like the reference. [accent] adds a pink ring (used for play/pause). */
@Composable
fun KnobButton(icon: ImageVector, description: String, size: Dp = 56.dp, accent: Boolean = false, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.size(size)
            .shadow(8.dp, CircleShape, clip = false, ambientColor = p.shadow, spotColor = p.shadow)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(p.knob.copy(alpha = 1f), p.knob.copy(red = p.knob.red * 0.8f, green = p.knob.green * 0.8f, blue = p.knob.blue * 0.9f))))
            .border(if (accent) 2.dp else 1.dp, Brush.linearGradient(if (accent) listOf(p.accent, Color(0xFF8155E8)) else listOf(Color.White.copy(alpha = 0.16f), Color.Transparent)), CircleShape)
            .pressClickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = p.knobIcon, modifier = Modifier.size(size * 0.46f)) }
}

/** Light glass icon button for secondary actions. */
@Composable
fun GlassIconButton(icon: ImageVector, description: String, size: Dp = 44.dp, tint: Color = LocalPalette.current.text, onClick: () -> Unit) {
    Box(
        Modifier.size(size).glass(CircleShape).pressClickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

/** Plain icon button without a background. */
@Composable
fun BareIconButton(icon: ImageVector, description: String, size: Dp = 40.dp, tint: Color = LocalPalette.current.textSoft, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).pressClickable(onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f)) }
}

@Composable
fun GlassChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .then(if (selected) Modifier.clip(CircleShape).background(Brush.linearGradient(listOf(p.knob, p.knob.copy(alpha = 0.9f)))) else Modifier.glass(CircleShape))
            .pressClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MusicType.caption, color = if (selected) p.knobIcon else p.text, maxLines = 1) }
}

@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MusicType.display, color = LocalPalette.current.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MusicType.heading, color = LocalPalette.current.text, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Text(actionLabel, style = MusicType.caption, color = LocalPalette.current.accentSoft,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp))
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(88.dp).glass(CircleShape, strong = true), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = p.accentSoft, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MusicType.title, color = p.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MusicType.bodySoft, color = p.textSoft, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier.clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFB45CFF), p.accent)))
            .pressClickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(label, style = MusicType.body, color = Color.White, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier.glass(CircleShape, strong = true).pressClickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, tint = p.text, modifier = Modifier.size(20.dp))
        Text(label, style = MusicType.body, color = p.text, maxLines = 1)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        androidx.compose.material3.CircularProgressIndicator(color = p.accentSoft, trackColor = p.inset, strokeWidth = 3.dp)
    }
}

/** Seek / value slider drawn with glass styling. [onChangeFinished] fires when the finger lifts. */
@Composable
fun GlassSlider(
    value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f, onChangeFinished: (() -> Unit)? = null,
    description: String = "",
) {
    val p = LocalPalette.current
    var width by remember { mutableIntStateOf(1) }
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val frac = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    fun at(x: Float) = valueRange.start + (x / width).coerceIn(0f, 1f) * span
    Box(
        modifier.fillMaxWidth().height(32.dp).onSizeChanged { width = it.width.coerceAtLeast(1) }
            .semantics { contentDescription = description }
            .pointerInput(valueRange) { detectTapGestures { onValueChange(at(it.x)); onChangeFinished?.invoke() } }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { onValueChange(at(it.x)) },
                    onDragEnd = { onChangeFinished?.invoke() },
                    onDragCancel = { onChangeFinished?.invoke() },
                    onHorizontalDrag = { change, _ -> change.consume(); onValueChange(at(change.position.x)) },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(p.inset))
        Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFF9B6BFF), p.accent))))
        Box(
            Modifier.fillMaxWidth().height(32.dp).drawBehind {
                val x = size.width * frac
                drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(x.coerceIn(9.dp.toPx(), size.width - 9.dp.toPx()), size.height / 2))
                drawCircle(p.accent, radius = 4.dp.toPx(), center = Offset(x.coerceIn(9.dp.toPx(), size.width - 9.dp.toPx()), size.height / 2))
            }
        )
    }
}

/** Vertical slider for equalizer bands. Center = 0 dB. */
@Composable
fun VerticalGlassSlider(value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val p = LocalPalette.current
    var height by remember { mutableIntStateOf(1) }
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    fun at(y: Float) = range.endInclusive - (y / height).coerceIn(0f, 1f) * span
    val frac = ((value - range.start) / span).coerceIn(0f, 1f)
    Box(
        modifier.width(36.dp).onSizeChanged { height = it.height.coerceAtLeast(1) }
            .pointerInput(range, enabled) { detectTapGestures { if (enabled) onValueChange(at(it.y)) } }
            .pointerInput(range, enabled) {
                detectVerticalDragGestures(
                    onDragStart = { if (enabled) onValueChange(at(it.y)) },
                    onVerticalDrag = { change, _ -> change.consume(); if (enabled) onValueChange(at(change.position.y)) },
                )
            }
            .drawBehind {
                val cx = size.width / 2
                val r = 9.dp.toPx()
                drawRoundRect(p.inset, Offset(cx - 3.dp.toPx(), 0f), Size(6.dp.toPx(), size.height), CornerRadius(3.dp.toPx()))
                val y = (size.height * (1f - frac)).coerceIn(r, size.height - r)
                val zero = size.height * (range.endInclusive / span)
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFF9B6BFF), p.accent)),
                    Offset(cx - 3.dp.toPx(), minOf(y, zero)), Size(6.dp.toPx(), kotlin.math.abs(y - zero).coerceAtLeast(1f)), CornerRadius(3.dp.toPx()),
                    alpha = if (enabled) 1f else 0.4f,
                )
                drawCircle(Color.White.copy(alpha = if (enabled) 1f else 0.6f), r, Offset(cx, y))
                drawCircle(p.accent.copy(alpha = if (enabled) 1f else 0.5f), r * 0.45f, Offset(cx, y))
            },
    )
}

@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val x by animateFloatAsState(if (checked) 1f else 0f, tween(140), label = "switch")
    Box(
        Modifier.width(50.dp).height(30.dp).clip(CircleShape)
            .background(if (checked) Brush.horizontalGradient(listOf(Color(0xFF9B6BFF), p.accent)) else Brush.horizontalGradient(listOf(p.inset, p.inset)))
            .pressClickable { onChange(!checked) },
    ) {
        Box(Modifier.padding(3.dp).offset(x = (20.dp * x)).size(24.dp).clip(CircleShape).background(Color.White))
    }
}
