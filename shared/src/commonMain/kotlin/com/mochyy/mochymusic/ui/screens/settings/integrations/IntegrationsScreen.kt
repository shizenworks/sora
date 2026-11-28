// mochy → settings → integrations: a row per service, always all of them, tapping one opens its own page

package com.mochyy.mochymusic.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.WorkOutGenresKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.backup
import com.mochyy.mochymusic.shared.resources.car_settings_integration_desc
import com.mochyy.mochymusic.shared.resources.car_settings_title
import com.mochyy.mochymusic.shared.resources.cast
import com.mochyy.mochymusic.shared.resources.integration_backup_desc
import com.mochyy.mochymusic.shared.resources.integration_backup_name
import com.mochyy.mochymusic.shared.resources.integration_discord
import com.mochyy.mochymusic.shared.resources.integration_discord_desc
import com.mochyy.mochymusic.shared.resources.integration_lastfm
import com.mochyy.mochymusic.shared.resources.integration_librefm
import com.mochyy.mochymusic.shared.resources.integration_listenbrainz_desc
import com.mochyy.mochymusic.shared.resources.integration_scrobbling_desc
import com.mochyy.mochymusic.shared.resources.integrations
import com.mochyy.mochymusic.shared.resources.module_youtube_desc
import com.mochyy.mochymusic.shared.resources.module_youtube_name
import com.mochyy.mochymusic.shared.resources.music_note
import com.mochyy.mochymusic.shared.resources.provider_youtube_music
import com.mochyy.mochymusic.shared.resources.work_out_genres
import com.mochyy.mochymusic.shared.resources.work_out_genres_off_desc
import com.mochyy.mochymusic.shared.resources.work_out_genres_on_desc
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.utils.rememberPreference
import org.jetbrains.compose.resources.stringResource

const val IntegrationsRoute = "integrations_settings"

// the youtube music logo is a red disc, this fills its corners so it sits as a tile like the others
private val YouTubeMusicRed = Color(0xFFFF0000)

// backup has no brand of its own, so its white cloud sits on a plain blue tile
private val BackupBlue = Color(0xFF2F7CF6)

/** which brand mark a scrobble service's row shows */
enum class ScrobbleBrand { LAST_FM, LIBRE_FM }

/** one scrobble service the page lists: its brand mark, the name :app's ScrobbleSite gives it, and its own page */
data class ScrobbleSiteRow(val brand: ScrobbleBrand, val label: String, val route: String)

/**
 * The pages the integration rows open. :app owns the navigation graph, so its routes come in as strings;
 * a null route means this platform has no such page (or not yet), and that row is left out.
 */
data class IntegrationRoutes(
    val androidAuto: String? = null,
    val discord: String? = null,
    val youtubeSync: String? = null,
    val backup: String? = null,
)

@Composable
fun IntegrationsScreen(
    navController: NavController,
    scrobbleSites: List<ScrobbleSiteRow> = emptyList(),
    routes: IntegrationRoutes = IntegrationRoutes(),
) {
    SubSettingsScaffold(
        title = stringResource(Res.string.integrations),
        onBack = { navController.navigateUp() },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // android auto is an android screen, so another platform passes no route and never sees the row
            routes.androidAuto?.let { route ->
                IntegrationRow(
                    name = stringResource(Res.string.car_settings_title),
                    description = stringResource(Res.string.car_settings_integration_desc),
                    icon = Res.drawable.cast,
                    onClick = { navController.navigate(route) },
                )
            }
            routes.discord?.let { route ->
                IntegrationRow(
                    name = "Discord",
                    description = stringResource(Res.string.integration_discord_desc),
                    icon = Res.drawable.integration_discord,
                    onClick = { navController.navigate(route) },
                )
            }
            scrobbleSites.forEach { site ->
                IntegrationRow(
                    name = site.label,
                    description = stringResource(Res.string.integration_scrobbling_desc, site.label),
                    icon = when (site.brand) {
                        ScrobbleBrand.LAST_FM -> Res.drawable.integration_lastfm
                        ScrobbleBrand.LIBRE_FM -> Res.drawable.integration_librefm
                    },
                    onClick = { navController.navigate(site.route) },
                )
            }
            IntegrationRow(
                name = "ListenBrainz",
                description = stringResource(Res.string.integration_listenbrainz_desc),
                icon = Res.drawable.integration_librefm,
                onClick = { navController.navigate(ListenBrainzRoute) },
            )
            // the genres the stats page shows are asked of Last.fm, artist names only; the
            // switch travels with the scrobbling services that share its account
            var workOutGenres by rememberPreference(WorkOutGenresKey, defaultValue = true)
            IntegrationSwitchRow(
                name = stringResource(Res.string.work_out_genres),
                description = stringResource(
                    if (workOutGenres) Res.string.work_out_genres_on_desc else Res.string.work_out_genres_off_desc,
                ),
                icon = Res.drawable.music_note,
                checked = workOutGenres,
                onCheckedChange = { workOutGenres = it },
            )
            routes.youtubeSync?.let { route ->
                IntegrationRow(
                    name = stringResource(Res.string.module_youtube_name),
                    description = stringResource(Res.string.module_youtube_desc),
                    icon = Res.drawable.provider_youtube_music,
                    iconBackground = YouTubeMusicRed,
                    onClick = { navController.navigate(route) },
                )
            }
            routes.backup?.let { route ->
                IntegrationRow(
                    name = stringResource(Res.string.integration_backup_name),
                    description = stringResource(Res.string.integration_backup_desc),
                    icon = Res.drawable.backup,
                    iconBackground = BackupBlue,
                    iconPadding = 7.dp,
                    onClick = { navController.navigate(route) },
                )
            }
        }

        Spacer(Modifier.height(140.dp))
    }
}
