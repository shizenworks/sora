// the last.fm or libre.fm page: every song that counts is scrobbled, and offline plays wait for a network

package com.mochyy.mochymusic.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.LastFMUseNowPlaying
import com.mochyy.mochymusic.constants.LastFMUseSendLikes
import com.mochyy.mochymusic.constants.ScrobbleDelayPercentKey
import com.mochyy.mochymusic.constants.ScrobbleDelaySecondsKey
import com.mochyy.mochymusic.constants.ScrobbleMinSongDurationKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.scrobble_plays_waiting
import com.mochyy.mochymusic.shared.resources.cloud
import com.mochyy.mochymusic.shared.resources.favorite
import com.mochyy.mochymusic.shared.resources.integrations
import com.mochyy.mochymusic.shared.resources.login
import com.mochyy.mochymusic.shared.resources.logout
import com.mochyy.mochymusic.shared.resources.music_history
import com.mochyy.mochymusic.shared.resources.music_note
import com.mochyy.mochymusic.shared.resources.scrobble_account
import com.mochyy.mochymusic.shared.resources.scrobble_all_sent
import com.mochyy.mochymusic.shared.resources.scrobble_now_playing
import com.mochyy.mochymusic.shared.resources.scrobble_now_playing_desc
import com.mochyy.mochymusic.shared.resources.scrobble_not_set_up
import com.mochyy.mochymusic.shared.resources.scrobble_open_profile
import com.mochyy.mochymusic.shared.resources.scrobble_open_profile_desc
import com.mochyy.mochymusic.shared.resources.scrobble_page_subtitle
import com.mochyy.mochymusic.shared.resources.scrobble_paused_signed_in_as
import com.mochyy.mochymusic.shared.resources.scrobble_plays_waiting_desc
import com.mochyy.mochymusic.shared.resources.scrobble_send_later
import com.mochyy.mochymusic.shared.resources.scrobble_send_likes
import com.mochyy.mochymusic.shared.resources.scrobble_send_likes_desc
import com.mochyy.mochymusic.shared.resources.scrobble_sign_in_desc
import com.mochyy.mochymusic.shared.resources.scrobble_sign_in_title
import com.mochyy.mochymusic.shared.resources.scrobble_sign_out
import com.mochyy.mochymusic.shared.resources.scrobble_sign_out_desc
import com.mochyy.mochymusic.shared.resources.scrobble_signed_in_as
import com.mochyy.mochymusic.shared.resources.scrobble_to
import com.mochyy.mochymusic.shared.resources.scrobble_while_listening
import com.mochyy.mochymusic.shared.resources.web_link
import com.mochyy.mochymusic.scrobble.ScrobbleDefaults
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.viewmodels.IntegrationsViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

const val ScrobbleServiceRoute = "integrations_settings/scrobble/{site}"

fun scrobbleServiceRoute(site: String) = "integrations_settings/scrobble/$site"

/**
 * One scrobbling service, as the shared page needs it: the app's ScrobbleSite also holds the API
 * client and its build keys, which stay in :app. The id is the app enum's name, used to sign out.
 */
data class ScrobbleSiteInfo(
    val id: String,
    val label: String,
    val enabledKey: Preferences.Key<Boolean>,
    val isConfigured: Boolean,
    val profileUrl: (String) -> String,
)

/** the sign-in sheet and its toast live in :app's popup kit; the page calls this to open it */
class ScrobbleServiceHost(
    val toast: (message: String, long: Boolean) -> Unit = { _, _ -> },
    val showSignIn: () -> Unit = {},
)

@Composable
fun ScrobbleServiceScreen(
    navController: NavController,
    site: ScrobbleSiteInfo,
    viewModel: IntegrationsViewModel = koinViewModel(),
    host: ScrobbleServiceHost = ScrobbleServiceHost(),
) {
    val accounts by viewModel.accounts.collectAsState()
    val pending by viewModel.pending.collectAsState()
    val sendResult by viewModel.sendResult.collectAsState()
    val (nowPlaying, onNowPlayingChange) = rememberPreference(LastFMUseNowPlaying, defaultValue = true)
    val (sendLikes, onSendLikesChange) = rememberPreference(LastFMUseSendLikes, defaultValue = false)
    val (minDuration, onMinDurationChange) = rememberPreference(ScrobbleMinSongDurationKey, defaultValue = ScrobbleDefaults.DEFAULT_MIN_SONG_SECONDS)
    val (delayPercent, onDelayPercentChange) = rememberPreference(ScrobbleDelayPercentKey, defaultValue = ScrobbleDefaults.DEFAULT_PLAYED_FRACTION)
    val (delaySeconds, onDelaySecondsChange) = rememberPreference(ScrobbleDelaySecondsKey, defaultValue = ScrobbleDefaults.DEFAULT_MAX_WAIT_SECONDS)
    val allSent = stringResource(Res.string.scrobble_all_sent)
    val sendLater = stringResource(Res.string.scrobble_send_later)

    LaunchedEffect(sendResult) {
        sendResult?.let { sent ->
            host.toast(if (sent) allSent else sendLater, false)
            viewModel.clearSendResult()
        }
    }

    SubSettingsScaffold(
        title = site.label,
        onBack = { navController.navigateUp() },
        backLabel = stringResource(Res.string.integrations),
    ) {
        IntegrationPageSubtitle(stringResource(Res.string.scrobble_page_subtitle))
        ScrobbleAccountGroup(
            site = site,
            username = accounts[site.id],
            onSignIn = { host.showSignIn() },
            onSignOut = { viewModel.signOut(site.id) },
        )
        Spacer(Modifier.height(16.dp))

        // shared by both services, so it reads the same on either page
        val nowPlayingTitle = stringResource(Res.string.scrobble_now_playing)
        val sendLikesTitle = stringResource(Res.string.scrobble_send_likes)
        SettingsGroup(
            title = stringResource(Res.string.scrobble_while_listening),
            style = SettingsGroupStyle.Grouped,
            items = listOfNotNull(
                SettingsItem(
                    title = { Text(nowPlayingTitle) },
                    highlightKey = "Now Playing",
                    searchText = nowPlayingTitle,
                    descriptionText = stringResource(Res.string.scrobble_now_playing_desc),
                    icon = painterResource(Res.drawable.music_note),
                    iconShape = CircleShape,
                    onClick = { onNowPlayingChange(!nowPlaying) },
                    trailingContent = { AppSwitch(checked = nowPlaying, onCheckedChange = onNowPlayingChange) },
                ),
                SettingsItem(
                    title = { Text(sendLikesTitle) },
                    highlightKey = "Send likes",
                    searchText = sendLikesTitle,
                    descriptionText = stringResource(Res.string.scrobble_send_likes_desc),
                    icon = painterResource(Res.drawable.favorite),
                    iconShape = CircleShape,
                    onClick = { onSendLikesChange(!sendLikes) },
                    trailingContent = { AppSwitch(checked = sendLikes, onCheckedChange = onSendLikesChange) },
                ),
                if (pending > 0) {
                    val waiting = pluralStringResource(Res.plurals.scrobble_plays_waiting, pending, pending)
                    SettingsItem(
                        title = { Text(waiting) },
                        descriptionText = stringResource(Res.string.scrobble_plays_waiting_desc),
                        icon = painterResource(Res.drawable.cloud),
                        iconShape = CircleShape,
                        onClick = viewModel::sendNow,
                    )
                } else null,
            ),
        )

        // the rule a play must meet before it counts, Last.fm's own; ListenBrainz needs no
        // rule but shares the page family, so its sliders live on its own page
        ScrobbleTimingGroup(
            minDuration = minDuration,
            onMinDuration = onMinDurationChange,
            delayPercent = delayPercent,
            onDelayPercent = onDelayPercentChange,
            delaySeconds = delaySeconds,
            onDelaySeconds = onDelaySecondsChange,
        )

        Spacer(Modifier.height(140.dp))
    }
}

/** sign in, or once signed in: scrobbling on or paused, the profile, and sign out */
@Composable
private fun ScrobbleAccountGroup(
    site: ScrobbleSiteInfo,
    username: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val (enabled, onEnabledChange) = rememberPreference(site.enabledKey, defaultValue = true)
    val uriHandler = LocalUriHandler.current
    val items = if (username == null) {
        listOf(
            SettingsItem(
                title = { Text(stringResource(Res.string.scrobble_sign_in_title, site.label)) },
                highlightKey = site.label,
                descriptionText = stringResource(
                    if (site.isConfigured) Res.string.scrobble_sign_in_desc else Res.string.scrobble_not_set_up,
                ),
                icon = painterResource(Res.drawable.login),
                iconShape = CircleShape,
                enabled = site.isConfigured,
                onClick = onSignIn,
            ),
        )
    } else {
        listOf(
            SettingsItem(
                title = { Text(stringResource(Res.string.scrobble_to, site.label)) },
                highlightKey = site.label,
                descriptionText = stringResource(
                    if (enabled) Res.string.scrobble_signed_in_as else Res.string.scrobble_paused_signed_in_as,
                    username,
                ),
                icon = painterResource(Res.drawable.music_history),
                iconShape = CircleShape,
                onClick = { onEnabledChange(!enabled) },
                trailingContent = { AppSwitch(checked = enabled, onCheckedChange = onEnabledChange) },
            ),
            SettingsItem(
                title = { Text(stringResource(Res.string.scrobble_open_profile)) },
                descriptionText = stringResource(Res.string.scrobble_open_profile_desc, site.label),
                icon = painterResource(Res.drawable.web_link),
                iconShape = CircleShape,
                onClick = { uriHandler.openUri(site.profileUrl(username)) },
            ),
            SettingsItem(
                title = { Text(stringResource(Res.string.scrobble_sign_out)) },
                descriptionText = stringResource(Res.string.scrobble_sign_out_desc, site.label),
                icon = painterResource(Res.drawable.logout),
                iconShape = CircleShape,
                onClick = onSignOut,
            ),
        )
    }
    SettingsGroup(title = stringResource(Res.string.scrobble_account), style = SettingsGroupStyle.Grouped, items = items)
}
