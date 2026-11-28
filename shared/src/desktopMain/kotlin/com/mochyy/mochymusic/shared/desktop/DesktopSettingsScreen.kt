// DesktopSettingsScreen.kt
// The desktop Settings tab: the shared settings pages from commonMain, reached through the tab's own
// navigation. The desktop launcher provides the composition locals those pages read (LocalAppPreferences,
// LocalSubSettingsChrome, LocalPlayerAwareWindowInsets) at the root, as every platform does.

package com.mochyy.mochymusic.shared.desktop

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.audiopass_hub_title
import com.mochyy.mochymusic.shared.resources.shell_coming_body
import com.mochyy.mochymusic.shared.resources.shell_coming_title
import com.mochyy.mochymusic.shared.resources.transition_title
import com.mochyy.mochymusic.shared.settings.PlaybackPreferences
import com.mochyy.mochymusic.shared.ui.settings.CodecBadgeWriter
import com.mochyy.mochymusic.shared.ui.settings.SharedSettingsScreen
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.screens.settings.PlaybackSettingsRoute
import com.mochyy.mochymusic.ui.screens.settings.PlaybackSettingsScreen
import org.jetbrains.compose.resources.stringResource

private const val DesktopSettingsHomeRoute = "desktop_settings"

@Composable
internal fun DesktopSettingsScreen(
    settings: PlaybackPreferences,
    codecBadge: CodecBadgeWriter,
) {
    val navController = rememberNavController()
    // built once: the screen above recomposes with playback and preference changes, and a new builder
    // each time would make NavHost rebuild its graph and put the user back on the settings list mid-page
    val graphBuilder: NavGraphBuilder.() -> Unit = remember(navController) {
        {
            composable(DesktopSettingsHomeRoute) {
                SharedSettingsScreen(
                    settings = settings,
                    codecBadge = codecBadge,
                    onOpenPlayback = { navController.navigate(PlaybackSettingsRoute) },
                    // no AI translator, ListenBrainz or Proxy rows yet: nothing on desktop translates lyrics,
                    // scrobbles or routes its HTTP clients through the proxy, and desktop has no sealed store
                    // (Android's SecretPreferences) for the keys and tokens those pages save. A page whose
                    // settings do nothing, holding a secret in plain text, is worse than no page; each comes
                    // back with the feature that reads it and a secure store
                )
            }
            composable(PlaybackSettingsRoute) {
                PlaybackSettingsScreen(navController)
            }
            // Playback's related rows open Audio Lab pages the Android app registers; desktop has no page
            // for them yet, so they open this instead of failing navigation
            composable("crossmix_settings") {
                DesktopMissingPage(navController, stringResource(Res.string.transition_title))
            }
            composable("audio_pass") {
                DesktopMissingPage(navController, stringResource(Res.string.audiopass_hub_title))
            }
        }
    }
    NavHost(
        navController = navController,
        startDestination = DesktopSettingsHomeRoute,
        modifier = Modifier.fillMaxSize(),
        builder = graphBuilder,
    )
}

// a page the shared settings screens link to but this platform hasn't ported: says so, and goes back
@Composable
private fun DesktopMissingPage(navController: NavController, title: String) {
    SubSettingsScaffold(title = title, onBack = { navController.navigateUp() }) {
        Text(stringResource(Res.string.shell_coming_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.shell_coming_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
