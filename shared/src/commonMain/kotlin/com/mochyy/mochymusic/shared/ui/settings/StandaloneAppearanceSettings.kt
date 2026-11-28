package com.mochyy.mochymusic.shared.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.mochyy.mochymusic.constants.AccentColorKey
import com.mochyy.mochymusic.constants.PureBlackKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.settings_accent_color
import com.mochyy.mochymusic.shared.resources.settings_appearance_category
import com.mochyy.mochymusic.shared.resources.settings_default_accent
import com.mochyy.mochymusic.shared.resources.settings_pure_black
import com.mochyy.mochymusic.shared.resources.settings_write_failed
import com.mochyy.mochymusic.shared.settings.PlaybackPreferences
import com.mochyy.mochymusic.shared.ui.theme.MochyAccentPresets
import kotlinx.coroutines.launch
import okio.IOException
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun StandaloneAppearanceSettings(settings: PlaybackPreferences) {
    val preferences by settings.values.collectAsState(initial = emptyPreferences())
    val selected = preferences[AccentColorKey] ?: 0
    val pureBlack = preferences[PureBlackKey] ?: false
    val scope = rememberCoroutineScope()
    var writeFailed by remember { mutableStateOf(false) }

    fun <T> save(key: Preferences.Key<T>, value: T) {
        scope.launch {
            writeFailed = try {
                settings.set(key, value)
                false
            } catch (_: IOException) {
                true
            }
        }
    }

    SettingsSection(stringResource(Res.string.settings_appearance_category)) {
        SettingsCard {
            Text(
                stringResource(Res.string.settings_accent_color),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val defaultLabel = stringResource(Res.string.settings_default_accent)
                for (argb in listOf(0) + MochyAccentPresets) {
                    val name = if (argb == 0) defaultLabel else "#${(argb and 0xFFFFFF).toString(16).padStart(6, '0')}"
                    Box(
                        Modifier.size(44.dp)
                            .selectable(selected == argb, role = Role.RadioButton) { save(AccentColorKey, argb) }
                            .semantics { contentDescription = name }
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(if (argb == 0) Color(0xFF8E8E8E) else Color(argb))
                            .border(2.dp, if (selected == argb) Color.White else Color.Transparent, CircleShape),
                    )
                }
            }
            PlaybackSettingRow(stringResource(Res.string.settings_pure_black), pureBlack) { save(PureBlackKey, it) }
        }
    }
    if (writeFailed) {
        Text(stringResource(Res.string.settings_write_failed), color = MaterialTheme.colorScheme.error)
    }
}
