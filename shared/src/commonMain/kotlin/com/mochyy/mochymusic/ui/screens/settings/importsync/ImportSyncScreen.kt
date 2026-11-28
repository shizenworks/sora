package com.mochyy.mochymusic.ui.screens.settings.importsync

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.backup
import com.mochyy.mochymusic.shared.resources.import_sync_backup_title
import com.mochyy.mochymusic.shared.resources.import_sync_btn_ok
import com.mochyy.mochymusic.shared.resources.import_sync_export_csv_desc
import com.mochyy.mochymusic.shared.resources.import_sync_export_csv_detail
import com.mochyy.mochymusic.shared.resources.import_sync_export_csv_title
import com.mochyy.mochymusic.shared.resources.import_sync_exporting
import com.mochyy.mochymusic.shared.resources.import_sync_import_desc
import com.mochyy.mochymusic.shared.resources.import_sync_import_detail
import com.mochyy.mochymusic.shared.resources.import_sync_import_title
import com.mochyy.mochymusic.shared.resources.import_sync_progress_failed
import com.mochyy.mochymusic.shared.resources.import_sync_progress_finished_count
import com.mochyy.mochymusic.shared.resources.import_sync_progress_importing_count
import com.mochyy.mochymusic.shared.resources.import_sync_progress_new_songs_multiple
import com.mochyy.mochymusic.shared.resources.import_sync_progress_new_songs_single
import com.mochyy.mochymusic.shared.resources.import_sync_progress_resynced_multiple
import com.mochyy.mochymusic.shared.resources.import_sync_progress_resynced_single
import com.mochyy.mochymusic.shared.resources.import_sync_progress_resyncing
import com.mochyy.mochymusic.shared.resources.import_sync_progress_stopped
import com.mochyy.mochymusic.shared.resources.import_sync_sync_desc_connected
import com.mochyy.mochymusic.shared.resources.import_sync_sync_desc_disconnected
import com.mochyy.mochymusic.shared.resources.import_sync_sync_title
import com.mochyy.mochymusic.shared.resources.import_sync_title
import com.mochyy.mochymusic.shared.resources.import_sync_tunemymusic_desc
import com.mochyy.mochymusic.shared.resources.import_sync_tunemymusic_detail
import com.mochyy.mochymusic.shared.resources.import_sync_tunemymusic_title
import com.mochyy.mochymusic.shared.resources.playlist_add
import com.mochyy.mochymusic.shared.resources.playlist_play
import com.mochyy.mochymusic.shared.resources.sync
import com.mochyy.mochymusic.shared.resources.web_link
import com.mochyy.mochymusic.ui.component.DefaultDialog
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** experimental hub: import from other services, sync a youtube account, tunemymusic csv, and backup */
@Composable
fun ImportSyncScreen(
    navController: NavController,
    viewModel: ImportSyncViewModel = koinViewModel(),
    // the platform's "save as" dialog; a platform without one doesn't show the export row
    saver: ImportCsvSaver? = null,
    // Android's device-integration row leans on its own sheets, so it comes in as a row from the host
    devicesRow: (@Composable () -> SettingsItem)? = null,
) {
    val progress by viewModel.importProgress.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()
    val signedIn = rememberYouTubeSignedIn()
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val devices = devicesRow?.invoke()

    SubSettingsScaffold(
        title = stringResource(Res.string.import_sync_title),
        onBack = { navController.navigateUp() },
    ) {
        if (progress.isRunning || progress.isDone) {
            val resync = progress.resync
            ImportInfoCard {
                Text(
                    text = when {
                        resync != null && progress.isRunning -> stringResource(Res.string.import_sync_progress_resyncing, resync.playlistName)
                        progress.isRunning -> stringResource(
                                    Res.string.import_sync_progress_importing_count,
                                    progress.processedTracks,
                                    progress.totalTracks,
                                )
                        progress.cancelled -> stringResource(Res.string.import_sync_progress_stopped)
                        resync != null && resync.failed.size < resync.playlists -> {
                            val countText = if (resync.added == 1) {
                                stringResource(Res.string.import_sync_progress_new_songs_single)
                            } else {
                                stringResource(Res.string.import_sync_progress_new_songs_multiple, resync.added)
                            }
                            if (resync.playlists == 1) {
                                stringResource(Res.string.import_sync_progress_resynced_single, resync.playlistName, countText)
                            } else {
                                stringResource(Res.string.import_sync_progress_resynced_multiple, resync.playlists, countText)
                            }
                        }
                        progress.error != null -> stringResource(Res.string.import_sync_progress_failed)
                        else -> stringResource(Res.string.import_sync_progress_finished_count, progress.matchedTracks, progress.totalTracks)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (progress.isRunning && progress.totalTracks > 0) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress.processedTracks.toFloat() / progress.totalTracks },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            SettingsGroup(
                style = SettingsGroupStyle.Grouped,
                items = listOf(
                    SettingsItem(
                        title = { Text(if (progress.isRunning) "See progress" else "See results") },
                        icon = painterResource(Res.drawable.playlist_play),
                        iconShape = CircleShape,
                        onClick = { navController.navigate(ImportProgressRoute) { launchSingleTop = true } },
                    )
                ),
            )
            Spacer(Modifier.height(16.dp))
        }

        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items = listOfNotNull(
                SettingsItem(
                    title = { Text(stringResource(Res.string.import_sync_import_title)) },
                    highlightKey = "Import",
                    descriptionText = stringResource(Res.string.import_sync_import_desc),
                    detail = stringResource(Res.string.import_sync_import_detail),
                    icon = painterResource(Res.drawable.playlist_add),
                    iconShape = CircleShape,
                    onClick = { navController.navigate(ImportProvidersRoute) },
                ),
                SettingsItem(
                    title = { Text(stringResource(Res.string.import_sync_sync_title)) },
                    highlightKey = "Sync",
                    descriptionText = if (signedIn) stringResource(Res.string.import_sync_sync_desc_connected)
                    else stringResource(Res.string.import_sync_sync_desc_disconnected),
                    icon = painterResource(Res.drawable.sync),
                    iconShape = CircleShape,
                    onClick = { navController.navigate(YouTubeSyncRoute) },
                ),
                // a platform with no device integration passes no row, and it simply isn't there
                devices,
                SettingsItem(
                    title = { Text(stringResource(Res.string.import_sync_tunemymusic_title)) },
                    highlightKey = "TuneMyMusic CSV",
                    descriptionText = stringResource(Res.string.import_sync_tunemymusic_desc),
                    detail = stringResource(Res.string.import_sync_tunemymusic_detail),
                    icon = painterResource(Res.drawable.web_link),
                    iconShape = CircleShape,
                    onClick = { navController.navigate(ImportTuneMyMusicRoute) },
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))

        if (saver != null) {
            SettingsGroup(
                title = stringResource(Res.string.import_sync_backup_title),
                style = SettingsGroupStyle.Grouped,
                items = listOf(
                    SettingsItem(
                        title = { Text(stringResource(Res.string.import_sync_export_csv_title)) },
                        highlightKey = "Export to Mochy CSV",
                        descriptionText = stringResource(Res.string.import_sync_export_csv_desc),
                        detail = stringResource(Res.string.import_sync_export_csv_detail),
                        icon = painterResource(Res.drawable.backup),
                        iconShape = CircleShape,
                        onClick = { saver.save("mochy-library-$today.csv") { uri -> if (uri != null) viewModel.export(uri) } },
                    ),
                ),
            )
        }
    }

    exportMessage?.let { message ->
        val working = message == "Exporting…"
        DefaultDialog(
            onDismiss = { if (!working) viewModel.clearExportMessage() },
            buttons = {
                if (!working) TextButton(onClick = viewModel::clearExportMessage) { Text(stringResource(Res.string.import_sync_btn_ok)) }
            },
        ) {
            Text(if (working) stringResource(Res.string.import_sync_exporting) else message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
