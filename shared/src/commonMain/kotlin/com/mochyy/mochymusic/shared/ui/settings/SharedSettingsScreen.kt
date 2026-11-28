package com.mochyy.mochymusic.shared.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.emptyPreferences
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.graphic_eq
import com.mochyy.mochymusic.shared.resources.integration_listenbrainz_desc
import com.mochyy.mochymusic.shared.resources.music_history
import com.mochyy.mochymusic.shared.resources.proxy
import com.mochyy.mochymusic.shared.resources.proxy_settings_desc
import com.mochyy.mochymusic.shared.resources.settings
import com.mochyy.mochymusic.shared.resources.settings_ai_translator
import com.mochyy.mochymusic.shared.resources.settings_page_ai_translator_desc
import com.mochyy.mochymusic.shared.resources.settings_page_playback_desc
import com.mochyy.mochymusic.shared.resources.settings_player_display
import com.mochyy.mochymusic.shared.resources.settings_playback_category
import com.mochyy.mochymusic.shared.resources.translate
import com.mochyy.mochymusic.shared.resources.wifi_proxy
import com.mochyy.mochymusic.shared.settings.PlaybackPreferences
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.screens.settings.linkItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SharedSettingsScreen(
    settings: PlaybackPreferences,
    codecBadge: CodecBadgeWriter,
    modifier: Modifier = Modifier,
    // the shared settings pages need a platform that can navigate to them; desktop passes these, iOS
    // leaves them null and keeps the single page it had
    onOpenPlayback: (() -> Unit)? = null,
    onOpenAiTranslator: (() -> Unit)? = null,
    onOpenListenBrainz: (() -> Unit)? = null,
    onOpenProxy: (() -> Unit)? = null,
) {
    val preferences by settings.values.collectAsState(initial = emptyPreferences())
    val pages = listOfNotNull(
        onOpenPlayback?.let {
            linkItem(
                key = "Playback",
                title = stringResource(Res.string.settings_playback_category),
                description = stringResource(Res.string.settings_page_playback_desc),
                icon = painterResource(Res.drawable.graphic_eq),
                onClick = it,
            )
        },
        onOpenAiTranslator?.let {
            linkItem(
                key = "AI translator",
                title = stringResource(Res.string.settings_ai_translator),
                description = stringResource(Res.string.settings_page_ai_translator_desc),
                icon = painterResource(Res.drawable.translate),
                onClick = it,
            )
        },
        onOpenListenBrainz?.let {
            linkItem(
                key = "ListenBrainz",
                title = "ListenBrainz",
                description = stringResource(Res.string.integration_listenbrainz_desc),
                icon = painterResource(Res.drawable.music_history),
                onClick = it,
            )
        },
        onOpenProxy?.let {
            linkItem(
                key = "Proxy",
                title = stringResource(Res.string.proxy),
                description = stringResource(Res.string.proxy_settings_desc),
                icon = painterResource(Res.drawable.wifi_proxy),
                onClick = it,
            )
        },
    )

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            stringResource(Res.string.settings),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
        )
        if (pages.isNotEmpty()) {
            SettingsGroup(style = SettingsGroupStyle.Grouped, items = pages)
        }
        SettingsSection(stringResource(Res.string.settings_playback_category)) {
            Text(
                stringResource(Res.string.settings_player_display),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
            SettingsCard {
                CodecBadgeRows(codecBadgePrefs(preferences), codecBadge)
            }
        }
        StandaloneAppearanceSettings(settings)
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
internal fun PlaybackSettingRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
