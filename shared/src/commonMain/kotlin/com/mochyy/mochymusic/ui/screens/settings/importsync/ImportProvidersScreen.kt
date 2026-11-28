package com.mochyy.mochymusic.ui.screens.settings.importsync

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.importer.LinkSource
import com.mochyy.mochymusic.importer.account.AccountService
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.backup
import com.mochyy.mochymusic.shared.resources.import_link_apple_music_desc
import com.mochyy.mochymusic.shared.resources.import_link_deezer_desc
import com.mochyy.mochymusic.shared.resources.import_link_soundcloud_desc
import com.mochyy.mochymusic.shared.resources.import_link_spotify_desc
import com.mochyy.mochymusic.shared.resources.import_link_youtube_desc
import com.mochyy.mochymusic.shared.resources.import_mochy_backup_desc
import com.mochyy.mochymusic.shared.resources.import_mochy_backup_title
import com.mochyy.mochymusic.shared.resources.import_other_services_desc
import com.mochyy.mochymusic.shared.resources.import_other_services_detail
import com.mochyy.mochymusic.shared.resources.import_other_services_title
import com.mochyy.mochymusic.shared.resources.import_provider_apple_music_desc
import com.mochyy.mochymusic.shared.resources.import_provider_spotify_desc
import com.mochyy.mochymusic.shared.resources.import_provider_tidal_desc
import com.mochyy.mochymusic.shared.resources.import_provider_ytm_desc
import com.mochyy.mochymusic.shared.resources.import_providers_file_header
import com.mochyy.mochymusic.shared.resources.import_providers_link_header
import com.mochyy.mochymusic.shared.resources.import_providers_other_header
import com.mochyy.mochymusic.shared.resources.import_providers_signin_header
import com.mochyy.mochymusic.shared.resources.import_sync_import_title
import com.mochyy.mochymusic.shared.resources.import_sync_tunemymusic_title
import com.mochyy.mochymusic.shared.resources.import_tunemymusic_csv_desc
import com.mochyy.mochymusic.shared.resources.library_music
import com.mochyy.mochymusic.shared.resources.link
import com.mochyy.mochymusic.shared.resources.login
import com.mochyy.mochymusic.shared.resources.web_link
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportProvidersScreen(
    navController: NavController,
    viewModel: ImportSyncViewModel = koinViewModel(),
    // the platform's CSV picker; without one the backup row stays hidden
    picker: ImportFilePicker? = null,
    notify: (String) -> Unit = {},
) {
    var linkSource by rememberSaveable { mutableStateOf<LinkSource?>(null) }
    val backupItem =
        if (picker != null) {
            SettingsItem(
                title = { Text(stringResource(Res.string.import_mochy_backup_title)) },
                highlightKey = "Mochy backup",
                descriptionText = stringResource(Res.string.import_mochy_backup_desc),
                icon = painterResource(Res.drawable.backup),
                iconShape = CircleShape,
                onClick = { picker.pick("Mochy backup") { uri -> if (uri != null) viewModel.loadFile(uri, "Mochy backup") } },
            )
        } else {
            null
        }

    SubSettingsScaffold(
        title = stringResource(Res.string.import_sync_import_title),
        onBack = { navController.navigateUp() },
    ) {
        SettingsGroup(
            title = stringResource(Res.string.import_providers_signin_header),
            style = SettingsGroupStyle.Grouped,
            items =
                listOf(
                    AccountService.SPOTIFY to stringResource(Res.string.import_provider_spotify_desc),
                    AccountService.APPLE_MUSIC to stringResource(Res.string.import_provider_apple_music_desc),
                    AccountService.YOUTUBE_MUSIC to stringResource(Res.string.import_provider_ytm_desc),
                    AccountService.TIDAL to stringResource(Res.string.import_provider_tidal_desc),
                ).map { (service, description) ->
                    SettingsItem(
                        title = { Text(service.label) },
                        highlightKey = "${service.label} account",
                        descriptionText = description,
                        icon = painterResource(Res.drawable.login),
                        iconShape = CircleShape,
                        onClick = { navController.navigate(accountImportRoute(service)) },
                    )
                },
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(Res.string.import_providers_link_header),
            style = SettingsGroupStyle.Grouped,
            items =
                LinkSource.entries.map { source ->
                    SettingsItem(
                        title = { Text(source.label) },
                        highlightKey = source.label,
                        descriptionText =
                            when (source) {
                                LinkSource.SPOTIFY -> stringResource(Res.string.import_link_spotify_desc)
                                LinkSource.APPLE_MUSIC -> stringResource(Res.string.import_link_apple_music_desc)
                                LinkSource.DEEZER -> stringResource(Res.string.import_link_deezer_desc)
                                LinkSource.YOUTUBE -> stringResource(Res.string.import_link_youtube_desc)
                                LinkSource.SOUNDCLOUD -> stringResource(Res.string.import_link_soundcloud_desc)
                            },
                        icon = painterResource(Res.drawable.link),
                        iconShape = CircleShape,
                        onClick = { linkSource = source },
                    )
                },
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(Res.string.import_providers_file_header),
            style = SettingsGroupStyle.Grouped,
            items =
                listOfNotNull(
                    backupItem,
                    SettingsItem(
                        title = { Text(stringResource(Res.string.import_sync_tunemymusic_title)) },
                        highlightKey = "TuneMyMusic CSV",
                        descriptionText = stringResource(Res.string.import_tunemymusic_csv_desc),
                        icon = painterResource(Res.drawable.web_link),
                        iconShape = CircleShape,
                        onClick = { navController.navigate(ImportTuneMyMusicRoute) },
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(Res.string.import_providers_other_header),
            style = SettingsGroupStyle.Grouped,
            items =
                listOf(
                    SettingsItem(
                        title = { Text(stringResource(Res.string.import_other_services_title)) },
                        descriptionText = stringResource(Res.string.import_other_services_desc),
                        detail = stringResource(Res.string.import_other_services_detail),
                        icon = painterResource(Res.drawable.library_music),
                        iconShape = CircleShape,
                        onClick = { navController.navigate(ImportTuneMyMusicRoute) },
                    ),
                ),
        )
    }

    linkSource?.let { source ->
        LinkInputDialog(
            source = source,
            onDismiss = { linkSource = null },
            onSubmit = { link ->
                linkSource = null
                viewModel.loadLink(link)
            },
        )
    }

    PendingImportHost(viewModel = viewModel, navController = navController, notify = notify)
}
