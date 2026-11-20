package com.goucem.aurora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.goucem.aurora.R
import com.goucem.aurora.ui.theme.*

@Composable
fun GlassSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val reduced = LocalReducedMotion.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(shown || reduced, enter = if (reduced) fadeIn() else slideInVertically { it / 2 } + fadeIn()) {
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 640.dp)
                        .background(Brush.verticalGradient(listOf(p.bgMid, p.bgBottom)), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .glass(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), strong = true)
                        .pointerInput(Unit) { detectTapGestures { } }
                        .navigationBarsPadding().padding(top = 10.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(40.dp).height(4.dp).background(p.textFaint, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun SheetAction(icon: ImageVector, label: String, tint: Color = LocalPalette.current.text, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = if (tint == p.text) p.accentSoft else tint, modifier = Modifier.size(24.dp))
        Text(label, style = AuroraType.body, color = tint)
    }
}

@Composable
fun GlassDialog(
    title: String, onDismiss: () -> Unit, confirmLabel: String, onConfirm: () -> Unit,
    message: String? = null, destructive: Boolean = false, content: @Composable ColumnScope.() -> Unit = {},
) {
    val p = LocalPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(p.bgBottom, RoundedCornerShape(28.dp)).glass(RoundedCornerShape(28.dp), strong = true).padding(24.dp),
        ) {
            Text(title, style = AuroraType.title, color = p.text)
            if (message != null) { Spacer(Modifier.height(10.dp)); Text(message, style = AuroraType.bodySoft, color = p.textSoft) }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)) { content() }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                SecondaryButton(stringResource(R.string.cancel), onClick = onDismiss)
                if (destructive) {
                    Row(
                        Modifier.background(p.heart, androidx.compose.foundation.shape.CircleShape).pressClickable(onClick = onConfirm).padding(horizontal = 22.dp, vertical = 12.dp),
                    ) { Text(confirmLabel, style = AuroraType.body, color = Color.White) }
                } else PrimaryButton(confirmLabel, onClick = onConfirm)
            }
        }
    }
}

@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, autoFocus: Boolean = false, onSearch: (() -> Unit)? = null) {
    val p = LocalPalette.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) try { focus.requestFocus() } catch (_: Exception) { } }
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        textStyle = AuroraType.body.copy(color = p.text), cursorBrush = SolidColor(p.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch?.invoke() }),
        modifier = modifier.focusRequester(focus),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().glass(RoundedCornerShape(18.dp), strong = true).padding(horizontal = 16.dp, vertical = 14.dp)) {
                if (value.isEmpty()) Text(hint, style = AuroraType.body, color = p.textFaint)
                inner()
            }
        },
    )
}
