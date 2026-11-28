package com.mochyy.mochymusic.ui.screens.settings.importsync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.check
import com.mochyy.mochymusic.shared.resources.close
import com.mochyy.mochymusic.shared.resources.import_progress_added_to
import com.mochyy.mochymusic.shared.resources.import_progress_background_hint
import com.mochyy.mochymusic.shared.resources.import_progress_copied_toast
import com.mochyy.mochymusic.shared.resources.import_progress_copy_not_found
import com.mochyy.mochymusic.shared.resources.import_progress_copy_not_found_desc
import com.mochyy.mochymusic.shared.resources.import_progress_done_btn
import com.mochyy.mochymusic.shared.resources.import_progress_found_ytm
import com.mochyy.mochymusic.shared.resources.import_progress_fraction
import com.mochyy.mochymusic.shared.resources.import_progress_liked_songs_fallback
import com.mochyy.mochymusic.shared.resources.import_progress_new_playlists
import com.mochyy.mochymusic.shared.resources.import_progress_not_found
import com.mochyy.mochymusic.shared.resources.import_progress_songs_count
import com.mochyy.mochymusic.shared.resources.import_progress_songs_liked
import com.mochyy.mochymusic.shared.resources.import_progress_starting
import com.mochyy.mochymusic.shared.resources.import_progress_status_done
import com.mochyy.mochymusic.shared.resources.import_progress_status_no_import
import com.mochyy.mochymusic.shared.resources.import_progress_status_resync_failed
import com.mochyy.mochymusic.shared.resources.import_progress_status_stopped
import com.mochyy.mochymusic.shared.resources.import_progress_stop_import
import com.mochyy.mochymusic.shared.resources.import_progress_stop_import_desc
import com.mochyy.mochymusic.shared.resources.import_progress_title_importing
import com.mochyy.mochymusic.shared.resources.import_progress_title_resync
import com.mochyy.mochymusic.shared.resources.import_progress_unmatched_more
import com.mochyy.mochymusic.shared.resources.import_progress_youtube_failures
import com.mochyy.mochymusic.shared.resources.import_sync_import_title
import com.mochyy.mochymusic.shared.resources.import_sync_progress_failed
import com.mochyy.mochymusic.shared.resources.share
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val UNMATCHED_SHOWN = 200

@Composable
fun ImportProgressScreen(
    navController: NavController,
    viewModel: ImportSyncViewModel = koinViewModel(),
    notify: (String) -> Unit = {},
) {
    val progress by viewModel.importProgress.collectAsState()
    val clipboard = LocalClipboardManager.current

    fun leave() {
        if (!navController.popBackStack(ImportSyncRoute, inclusive = false)) navController.navigateUp()
    }

    val resync = progress.resync
    SubSettingsScaffold(
        title =
            when {
                resync != null -> stringResource(Res.string.import_progress_title_resync)
                progress.isRunning -> stringResource(Res.string.import_progress_title_importing)
                else -> stringResource(Res.string.import_sync_import_title)
            },
        onBack = { navController.navigateUp() },
    ) {
        ImportInfoCard {
            val fraction = if (progress.totalTracks > 0) progress.processedTracks.toFloat() / progress.totalTracks else 0f
            Text(
                text =
                    when {
                        progress.isRunning -> {
                            stringResource(Res.string.import_progress_songs_count, progress.processedTracks, progress.totalTracks)
                        }

                        progress.cancelled -> {
                            stringResource(Res.string.import_progress_status_stopped)
                        }

                        progress.error != null -> {
                            if (resync !=
                                null
                            ) {
                                stringResource(Res.string.import_progress_status_resync_failed)
                            } else {
                                stringResource(Res.string.import_sync_progress_failed)
                            }
                        }

                        progress.isDone -> {
                            stringResource(Res.string.import_progress_status_done)
                        }

                        else -> {
                            stringResource(Res.string.import_progress_status_no_import)
                        }
                    },
                style = MaterialTheme.typography.headlineSmall,
            )
            if (progress.isRunning) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Text(
                    text = progress.currentLabel.ifEmpty { stringResource(Res.string.import_progress_starting) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(Res.string.import_progress_background_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (progress.isDone || progress.processedTracks > 0) {
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SummaryLine(
                        stringResource(Res.string.import_progress_found_ytm),
                        stringResource(Res.string.import_progress_fraction, progress.matchedTracks, progress.totalTracks),
                    )
                    if (resync != null &&
                        progress.isDone
                    ) {
                        SummaryLine(stringResource(Res.string.import_progress_added_to, resync.playlistName), resync.added.toString())
                    }
                    if (progress.playlistsCreated >
                        0
                    ) {
                        SummaryLine(stringResource(Res.string.import_progress_new_playlists), progress.playlistsCreated.toString())
                    }
                    if (progress.likedAdded >
                        0
                    ) {
                        SummaryLine(stringResource(Res.string.import_progress_songs_liked), progress.likedAdded.toString())
                    }
                    if (progress.unmatched.isNotEmpty()) {
                        SummaryLine(
                            stringResource(Res.string.import_progress_not_found),
                            progress.unmatched.size.toString(),
                        )
                    }
                }
            }
            progress.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            if (progress.youtubeFailures.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(Res.string.import_progress_youtube_failures, progress.youtubeFailures.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val copiedMessage = stringResource(Res.string.import_progress_copied_toast, progress.unmatched.size)

        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items =
                listOfNotNull(
                    if (progress.isRunning) {
                        SettingsItem(
                            title = { Text(stringResource(Res.string.import_progress_stop_import)) },
                            descriptionText = stringResource(Res.string.import_progress_stop_import_desc),
                            icon = painterResource(Res.drawable.close),
                            iconShape = CircleShape,
                            onClick = viewModel::cancelImport,
                        )
                    } else {
                        SettingsItem(
                            title = { Text(stringResource(Res.string.import_progress_done_btn)) },
                            icon = painterResource(Res.drawable.check),
                            iconShape = CircleShape,
                            onClick = {
                                viewModel.clearFinishedImport()
                                leave()
                            },
                        )
                    },
                    if (progress.unmatched.isNotEmpty()) {
                        SettingsItem(
                            title = { Text(stringResource(Res.string.import_progress_copy_not_found)) },
                            descriptionText = stringResource(Res.string.import_progress_copy_not_found_desc),
                            icon = painterResource(Res.drawable.share),
                            iconShape = CircleShape,
                            onClick = {
                                val text =
                                    progress.unmatched.joinToString("\n") { miss ->
                                        buildString {
                                            append(miss.track.title)
                                            if (miss.track.artist.isNotEmpty()) append(" - ").append(miss.track.artist)
                                            miss.playlistName?.let { append("  [").append(it).append(']') }
                                        }
                                    }
                                clipboard.setText(AnnotatedString(text))
                                notify(copiedMessage)
                            },
                        )
                    } else {
                        null
                    },
                ),
        )

        // listed once finished: re-laying out hundreds of rows on every progress tick is wasted work
        if (progress.unmatched.isNotEmpty() && !progress.isRunning) {
            val fallbackName = stringResource(Res.string.import_progress_liked_songs_fallback)
            Spacer(Modifier.height(16.dp))
            SettingsGroup(
                title = stringResource(Res.string.import_progress_not_found),
                style = SettingsGroupStyle.Grouped,
                items =
                    progress.unmatched.take(UNMATCHED_SHOWN).map { miss ->
                        SettingsItem(
                            title = { Text(miss.track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            descriptionText =
                                listOfNotNull(miss.track.artist.ifEmpty { null }, miss.playlistName ?: fallbackName)
                                    .joinToString(" · "),
                        )
                    },
            )
            if (progress.unmatched.size > UNMATCHED_SHOWN) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(Res.string.import_progress_unmatched_more, progress.unmatched.size - UNMATCHED_SHOWN),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
) {
    androidx.compose.foundation.layout.Row {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
