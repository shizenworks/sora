// The ListenBrainz page, ported from the design in the screenshots: a user token instead of a
// sign-in, scrobbling on its own rules — a playing-now when a song starts, a listen whenever one
// runs to its end — the first-artist-only switch, and the timing sliders Last.fm's services read,
// shared here because this is the scrobbling home.

package com.mochyy.mochymusic.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.ListenBrainzEnabledKey
import com.mochyy.mochymusic.constants.ListenBrainzPrimaryArtistOnlyKey
import com.mochyy.mochymusic.constants.ListenBrainzTokenKey
import com.mochyy.mochymusic.constants.ScrobbleDelayPercentKey
import com.mochyy.mochymusic.constants.ScrobbleDelaySecondsKey
import com.mochyy.mochymusic.constants.ScrobbleMinSongDurationKey
import com.mochyy.mochymusic.scrobble.ScrobbleDefaults
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.integrations
import com.mochyy.mochymusic.shared.resources.listenbrainz_connected
import com.mochyy.mochymusic.shared.resources.listenbrainz_connected_desc
import com.mochyy.mochymusic.shared.resources.listenbrainz_enter_token
import com.mochyy.mochymusic.shared.resources.listenbrainz_enter_token_desc
import com.mochyy.mochymusic.shared.resources.listenbrainz_token_placeholder
import com.mochyy.mochymusic.shared.resources.listenbrainz_token_title
import com.mochyy.mochymusic.shared.resources.login
import com.mochyy.mochymusic.shared.resources.music_history
import com.mochyy.mochymusic.shared.resources.music_note
import com.mochyy.mochymusic.shared.resources.scrobble_account
import com.mochyy.mochymusic.shared.resources.scrobble_delay
import com.mochyy.mochymusic.shared.resources.scrobble_delay_subtitle
import com.mochyy.mochymusic.shared.resources.scrobble_listening_desc
import com.mochyy.mochymusic.shared.resources.scrobble_max_delay
import com.mochyy.mochymusic.shared.resources.scrobble_max_delay_subtitle
import com.mochyy.mochymusic.shared.resources.scrobble_min_duration
import com.mochyy.mochymusic.shared.resources.scrobble_min_duration_subtitle
import com.mochyy.mochymusic.shared.resources.scrobble_paused_desc
import com.mochyy.mochymusic.shared.resources.scrobble_primary_artist_only
import com.mochyy.mochymusic.shared.resources.scrobble_primary_artist_only_subtitle
import com.mochyy.mochymusic.shared.resources.scrobble_seconds_value
import com.mochyy.mochymusic.shared.resources.scrobble_timing
import com.mochyy.mochymusic.shared.resources.scrobble_to
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.TextFieldDialog
import com.mochyy.mochymusic.utils.rememberPreference
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

const val ListenBrainzRoute = "integrations_settings/listenbrainz"

@Composable
fun ListenBrainzScreen(navController: NavController) {
    val (enabled, setEnabled) = rememberPreference(ListenBrainzEnabledKey, defaultValue = false)
    val (token, setToken) = rememberPreference(ListenBrainzTokenKey, defaultValue = "")
    val (primaryOnly, setPrimaryOnly) = rememberPreference(ListenBrainzPrimaryArtistOnlyKey, defaultValue = false)
    val (minDuration, setMinDuration) = rememberPreference(ScrobbleMinSongDurationKey, defaultValue = ScrobbleDefaults.DEFAULT_MIN_SONG_SECONDS)
    val (delayPercent, setDelayPercent) = rememberPreference(ScrobbleDelayPercentKey, defaultValue = ScrobbleDefaults.DEFAULT_PLAYED_FRACTION)
    val (delaySeconds, setDelaySeconds) = rememberPreference(ScrobbleDelaySecondsKey, defaultValue = ScrobbleDefaults.DEFAULT_MAX_WAIT_SECONDS)
    var tokenDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    val connected = token.isNotBlank()

    SubSettingsScaffold(
        title = "ListenBrainz",
        onBack = { navController.navigateUp() },
        backLabel = stringResource(Res.string.integrations),
    ) {
        com.mochyy.mochymusic.ui.component.PagePreview(com.mochyy.mochymusic.ui.component.SettingsPreviewPage.ListenBrainz)
        // SubSettingsScaffold already scrolls its content; a second verticalScroll inside it is measured
        // with an infinite height and throws
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsGroup(
                title = stringResource(Res.string.scrobble_account),
                style = SettingsGroupStyle.Grouped,
                items = listOf(
                    SettingsItem(
                        title = { Text(if (connected) stringResource(Res.string.listenbrainz_connected) else stringResource(Res.string.listenbrainz_enter_token)) },
                        descriptionText = if (connected) stringResource(Res.string.listenbrainz_connected_desc)
                        else stringResource(Res.string.listenbrainz_enter_token_desc),
                        icon = painterResource(Res.drawable.login),
                        iconShape = CircleShape,
                        onClick = { tokenDialog = true },
                    ),
                    SettingsItem(
                        title = { Text(stringResource(Res.string.scrobble_to, "ListenBrainz")) },
                        descriptionText = stringResource(
                            if (enabled) Res.string.scrobble_listening_desc else Res.string.scrobble_paused_desc,
                        ),
                        icon = painterResource(Res.drawable.music_history),
                        iconShape = CircleShape,
                        enabled = connected,
                        onClick = { setEnabled(!enabled) },
                        trailingContent = { AppSwitch(checked = enabled && connected, onCheckedChange = setEnabled, enabled = connected) },
                    ),
                    SettingsItem(
                        title = { Text(stringResource(Res.string.scrobble_primary_artist_only)) },
                        descriptionText = stringResource(Res.string.scrobble_primary_artist_only_subtitle),
                        icon = painterResource(Res.drawable.music_note),
                        iconShape = CircleShape,
                        enabled = connected,
                        onClick = { setPrimaryOnly(!primaryOnly) },
                        trailingContent = { AppSwitch(checked = primaryOnly, onCheckedChange = setPrimaryOnly, enabled = connected) },
                    ),
                ),
            )

            ScrobbleTimingGroup(
                minDuration = minDuration,
                onMinDuration = setMinDuration,
                delayPercent = delayPercent,
                onDelayPercent = setDelayPercent,
                delaySeconds = delaySeconds,
                onDelaySeconds = setDelaySeconds,
            )
            Spacer(Modifier.height(140.dp))
        }
    }

    if (tokenDialog) {
        TextFieldDialog(
            title = { Text(stringResource(Res.string.listenbrainz_token_title)) },
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(token),
            placeholder = { Text(stringResource(Res.string.listenbrainz_token_placeholder)) },
            isInputValid = { it.isNotBlank() },
            onDone = {
                setToken(it.trim())
                if (it.isNotBlank()) setEnabled(true)
                tokenDialog = false
            },
            onDismiss = { tokenDialog = false },
        )
    }
}

/** the timing sliders, the rule a play must meet before it counts */
@Composable
fun ScrobbleTimingGroup(
    minDuration: Int,
    onMinDuration: (Int) -> Unit,
    delayPercent: Float,
    onDelayPercent: (Float) -> Unit,
    delaySeconds: Int,
    onDelaySeconds: (Int) -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.scrobble_timing),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        TimingSlider(
            label = stringResource(Res.string.scrobble_min_duration),
            description = stringResource(Res.string.scrobble_min_duration_subtitle),
            valueText = stringResource(Res.string.scrobble_seconds_value, minDuration),
            value = minDuration.toFloat(),
            valueRange = 15f..120f,
            steps = 20,
            onValueChange = { onMinDuration(it.roundToInt()) },
        )
        TimingSlider(
            label = stringResource(Res.string.scrobble_delay),
            description = stringResource(Res.string.scrobble_delay_subtitle),
            valueText = "${(delayPercent * 100).roundToInt()}%",
            value = delayPercent,
            valueRange = 0.1f..1.0f,
            steps = 8,
            onValueChange = onDelayPercent,
        )
        TimingSlider(
            label = stringResource(Res.string.scrobble_max_delay),
            description = stringResource(Res.string.scrobble_max_delay_subtitle),
            valueText = stringResource(Res.string.scrobble_seconds_value, delaySeconds),
            value = delaySeconds.toFloat(),
            valueRange = 30f..300f,
            steps = 26,
            onValueChange = { onDelaySeconds(it.roundToInt()) },
        )
    }
}

/** one slider row with its label, description and the value it stands at */
@Composable
private fun TimingSlider(
    label: String,
    description: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    var dragging by remember { mutableFloatStateOf(value) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
        )
        Slider(
            value = dragging,
            onValueChange = {
                dragging = it
                onValueChange(it)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
