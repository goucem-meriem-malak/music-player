package com.gmm.music.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gmm.music.ui.components.GlassSwitch
import com.gmm.music.ui.components.pressClickable
import com.gmm.music.ui.theme.MusicType
import com.gmm.music.ui.theme.LocalPalette
import com.gmm.music.ui.theme.glass

@Composable
fun SettingGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Text(title.uppercase(), style = MusicType.label, color = p.accentSoft, modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 8.dp))
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(vertical = 6.dp), content = content)
}

@Composable
fun SettingRow(title: String, subtitle: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.pressClickable(onClick = onClick) else Modifier).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MusicType.body, color = p.text)
            if (subtitle != null) Text(subtitle, style = MusicType.caption, color = p.textSoft)
        }
        trailing()
    }
}

@Composable
fun SwitchRow(title: String, subtitle: String? = null, checked: Boolean, onChange: (Boolean) -> Unit) =
    SettingRow(title, subtitle) { GlassSwitch(checked, onChange) }

@Composable
fun SliderRow(title: String, valueLabel: String, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MusicType.body, color = p.text, modifier = Modifier.weight(1f))
            Text(valueLabel, style = MusicType.caption, color = p.accentSoft)
        }
        content()
    }
}
