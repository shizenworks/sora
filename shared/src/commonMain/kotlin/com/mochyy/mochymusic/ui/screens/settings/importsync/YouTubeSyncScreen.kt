package com.mochyy.mochymusic.ui.screens.settings.importsync

import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.account
import com.mochyy.mochymusic.shared.resources.cloud
import com.mochyy.mochymusic.shared.resources.group
import com.mochyy.mochymusic.shared.resources.history
import com.mochyy.mochymusic.shared.resources.import_sync_btn_ok
import com.mochyy.mochymusic.shared.resources.import_sync_sync_title
import com.mochyy.mochymusic.shared.resources.login
import com.mochyy.mochymusic.shared.resources.playlist_add
import com.mochyy.mochymusic.shared.resources.refresh
import com.mochyy.mochymusic.shared.resources.restore
import com.mochyy.mochymusic.shared.resources.sync
import com.mochyy.mochymusic.shared.resources.undo
import com.mochyy.mochymusic.shared.resources.yt_sync_account_header
import com.mochyy.mochymusic.shared.resources.yt_sync_auto_sync_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_auto_sync_title
import com.mochyy.mochymusic.shared.resources.yt_sync_cancel
import com.mochyy.mochymusic.shared.resources.yt_sync_dialog_sign_out_body
import com.mochyy.mochymusic.shared.resources.yt_sync_dialog_sign_out_title
import com.mochyy.mochymusic.shared.resources.yt_sync_dialog_upload_body
import com.mochyy.mochymusic.shared.resources.yt_sync_dialog_upload_create
import com.mochyy.mochymusic.shared.resources.yt_sync_dialog_upload_title
import com.mochyy.mochymusic.shared.resources.yt_sync_exclude_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_exclude_title
import com.mochyy.mochymusic.shared.resources.yt_sync_keep_in_sync_header
import com.mochyy.mochymusic.shared.resources.yt_sync_lost_likes_confirm
import com.mochyy.mochymusic.shared.resources.yt_sync_lost_likes_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_lost_likes_dialog_body
import com.mochyy.mochymusic.shared.resources.yt_sync_lost_likes_dialog_title
import com.mochyy.mochymusic.shared.resources.yt_sync_lost_likes_none_body
import com.mochyy.mochymusic.shared.resources.yt_sync_lost_likes_none_title
import com.mochyy.mochymusic.shared.resources.yt_sync_lost_likes_title
import com.mochyy.mochymusic.shared.resources.yt_sync_new_playlists_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_new_playlists_title
import com.mochyy.mochymusic.shared.resources.yt_sync_no_account_hint
import com.mochyy.mochymusic.shared.resources.yt_sync_now_desc_history
import com.mochyy.mochymusic.shared.resources.yt_sync_now_desc_no_history
import com.mochyy.mochymusic.shared.resources.yt_sync_now_header
import com.mochyy.mochymusic.shared.resources.yt_sync_now_title
import com.mochyy.mochymusic.shared.resources.yt_sync_other_apps_header
import com.mochyy.mochymusic.shared.resources.yt_sync_pull_history_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_pull_history_now_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_pull_history_now_title
import com.mochyy.mochymusic.shared.resources.yt_sync_pull_history_title
import com.mochyy.mochymusic.shared.resources.yt_sync_report_plays_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_report_plays_title
import com.mochyy.mochymusic.shared.resources.yt_sync_sign_in_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_sign_in_title
import com.mochyy.mochymusic.shared.resources.yt_sync_sign_out
import com.mochyy.mochymusic.shared.resources.yt_sync_signed_in
import com.mochyy.mochymusic.shared.resources.yt_sync_switch_accounts_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_switch_accounts_title
import com.mochyy.mochymusic.shared.resources.yt_sync_undo_both
import com.mochyy.mochymusic.shared.resources.yt_sync_undo_both_hint
import com.mochyy.mochymusic.shared.resources.yt_sync_undo_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_undo_dialog_body
import com.mochyy.mochymusic.shared.resources.yt_sync_undo_dialog_title
import com.mochyy.mochymusic.shared.resources.yt_sync_undo_phone
import com.mochyy.mochymusic.shared.resources.yt_sync_undo_title
import com.mochyy.mochymusic.shared.resources.yt_sync_upload_playlists_desc
import com.mochyy.mochymusic.shared.resources.yt_sync_upload_playlists_detail
import com.mochyy.mochymusic.shared.resources.yt_sync_upload_playlists_title
import com.mochyy.mochymusic.ui.screens.settings.previews.YouTubeSyncPreview
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.AccountEmailKey
import com.mochyy.mochymusic.constants.AccountNameKey
import com.mochyy.mochymusic.constants.YtExcludeImportsKey
import com.mochyy.mochymusic.constants.YtPullHistoryKey
import com.mochyy.mochymusic.constants.YtReportPlaysKey
import com.mochyy.mochymusic.constants.YtSyncNewPlaylistsKey
import com.mochyy.mochymusic.constants.YtmSyncKey
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.DefaultDialog
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun YouTubeSyncScreen(
    navController: NavController,
    viewModel: ImportSyncViewModel = koinViewModel(),
) {
    val signedIn = rememberYouTubeSignedIn()
    val accountName by rememberPreference(AccountNameKey, "")
    val accountEmail by rememberPreference(AccountEmailKey, "")
    val (autoSync, onAutoSyncChange) = rememberPreference(YtmSyncKey, true)
    val (syncNewPlaylists, onSyncNewPlaylistsChange) = rememberPreference(YtSyncNewPlaylistsKey, false)
    val (reportPlays, onReportPlaysChange) = rememberPreference(YtReportPlaysKey, true)
    val (pullHistory, onPullHistoryChange) = rememberPreference(YtPullHistoryKey, false)
    val (excludeImports, onExcludeImportsChange) = rememberPreference(YtExcludeImportsKey, false)
    val state by viewModel.youTubeState.collectAsState()

    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmUpload by remember { mutableStateOf(false) }
    var confirmUndo by remember { mutableStateOf(false) }
    // how many lost likes the dialog offers to bring back, null while it's closed
    var lostLikes by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    SubSettingsScaffold(
        title = stringResource(Res.string.import_sync_sync_title),
        onBack = { navController.navigateUp() },
    ) {
        com.mochyy.mochymusic.ui.screens.gesture.PortedPreviewHeader { YouTubeSyncPreview() }
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(16.dp))
        SettingsGroup(
            title = stringResource(Res.string.yt_sync_account_header),
            style = SettingsGroupStyle.Grouped,
            items =
                listOf(
                    if (signedIn) {
                        SettingsItem(
                            title = { Text(accountName.ifBlank { stringResource(Res.string.yt_sync_signed_in) }) },
                            descriptionText = accountEmail.ifBlank { "YouTube Music" },
                            icon = painterResource(Res.drawable.account),
                            iconShape = CircleShape,
                            trailingContent = {
                                TextButton(onClick = { confirmSignOut = true }) { Text(stringResource(Res.string.yt_sync_sign_out)) }
                            },
                        )
                    } else {
                        SettingsItem(
                            title = { Text(stringResource(Res.string.yt_sync_sign_in_title)) },
                            descriptionText = stringResource(Res.string.yt_sync_sign_in_desc),
                            icon = painterResource(Res.drawable.login),
                            iconShape = CircleShape,
                            onClick = { navController.navigate(YouTubeLoginRoute) },
                        )
                    },
                    SettingsItem(
                        title = { Text(stringResource(Res.string.yt_sync_switch_accounts_title)) },
                        descriptionText = stringResource(Res.string.yt_sync_switch_accounts_desc),
                        icon = painterResource(Res.drawable.group),
                        iconShape = CircleShape,
                        onClick = { navController.navigate(AccountSwitcherRoute) },
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(Res.string.yt_sync_keep_in_sync_header),
            style = SettingsGroupStyle.Grouped,
            items =
                listOf(
                    toggleItem(
                        title = stringResource(Res.string.yt_sync_auto_sync_title),
                        description = stringResource(Res.string.yt_sync_auto_sync_desc),
                        icon = Res.drawable.sync,
                        checked = autoSync,
                        enabled = signedIn,
                        onChange = onAutoSyncChange,
                    ),
                    toggleItem(
                        title = stringResource(Res.string.yt_sync_new_playlists_title),
                        description = stringResource(Res.string.yt_sync_new_playlists_desc),
                        icon = Res.drawable.playlist_add,
                        checked = syncNewPlaylists,
                        enabled = signedIn,
                        onChange = onSyncNewPlaylistsChange,
                    ),
                    toggleItem(
                        title = stringResource(Res.string.yt_sync_report_plays_title),
                        description = stringResource(Res.string.yt_sync_report_plays_desc),
                        icon = Res.drawable.history,
                        checked = reportPlays,
                        enabled = signedIn,
                        onChange = onReportPlaysChange,
                    ),
                    toggleItem(
                        title = stringResource(Res.string.yt_sync_pull_history_title),
                        description = stringResource(Res.string.yt_sync_pull_history_desc),
                        icon = Res.drawable.history,
                        checked = pullHistory,
                        enabled = signedIn,
                        onChange = onPullHistoryChange,
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(Res.string.yt_sync_other_apps_header),
            style = SettingsGroupStyle.Grouped,
            items =
                listOf(
                    toggleItem(
                        title = stringResource(Res.string.yt_sync_exclude_title),
                        description = stringResource(Res.string.yt_sync_exclude_desc),
                        icon = Res.drawable.cloud,
                        checked = excludeImports,
                        enabled = true,
                        onChange = onExcludeImportsChange,
                    ),
                ),
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            title = stringResource(Res.string.yt_sync_now_header),
            style = SettingsGroupStyle.Grouped,
            items =
                listOf(
                    SettingsItem(
                        title = { Text(stringResource(Res.string.yt_sync_now_title)) },
                        descriptionText =
                            if (pullHistory) {
                                stringResource(
                                    Res.string.yt_sync_now_desc_history,
                                )
                            } else {
                                stringResource(Res.string.yt_sync_now_desc_no_history)
                            },
                        icon = painterResource(Res.drawable.refresh),
                        iconShape = CircleShape,
                        enabled = signedIn && !state.running,
                        onClick = viewModel::syncNow,
                    ),
                    SettingsItem(
                        title = { Text(stringResource(Res.string.yt_sync_pull_history_now_title)) },
                        descriptionText = stringResource(Res.string.yt_sync_pull_history_now_desc),
                        icon = painterResource(Res.drawable.history),
                        iconShape = CircleShape,
                        enabled = signedIn && !state.running,
                        onClick = viewModel::pullHistoryNow,
                    ),
                    SettingsItem(
                        title = { Text(stringResource(Res.string.yt_sync_undo_title)) },
                        descriptionText = stringResource(Res.string.yt_sync_undo_desc),
                        icon = painterResource(Res.drawable.undo),
                        iconShape = CircleShape,
                        enabled = signedIn && state.canUndo && !state.running,
                        onClick = { confirmUndo = true },
                    ),
                    SettingsItem(
                        title = { Text(stringResource(Res.string.yt_sync_lost_likes_title)) },
                        descriptionText = stringResource(Res.string.yt_sync_lost_likes_desc),
                        icon = painterResource(Res.drawable.restore),
                        iconShape = CircleShape,
                        enabled = !state.running,
                        onClick = { scope.launch { lostLikes = viewModel.lostLikesCount() } },
                    ),
                    SettingsItem(
                        title = { Text(stringResource(Res.string.yt_sync_upload_playlists_title)) },
                        descriptionText = stringResource(Res.string.yt_sync_upload_playlists_desc),
                        detail = stringResource(Res.string.yt_sync_upload_playlists_detail),
                        icon = painterResource(Res.drawable.cloud),
                        iconShape = CircleShape,
                        enabled = signedIn && !state.running,
                        onClick = { confirmUpload = true },
                    ),
                ),
        )

        if (state.running || state.message != null) {
            Spacer(Modifier.height(16.dp))
            ImportInfoCard {
                if (state.running) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(state.label, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    Text(state.message.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = viewModel::clearYouTubeMessage) { Text(stringResource(Res.string.import_sync_btn_ok)) }
                }
            }
        }

        if (!signedIn) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.yt_sync_no_account_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmSignOut) {
        DefaultDialog(
            onDismiss = { confirmSignOut = false },
            horizontalAlignment = Alignment.Start,
            title = { Text(stringResource(Res.string.yt_sync_dialog_sign_out_title)) },
            buttons = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(Res.string.yt_sync_cancel)) }
                TextButton(onClick = {
                    confirmSignOut = false
                    viewModel.signOut()
                }) { Text(stringResource(Res.string.yt_sync_sign_out)) }
            },
        ) {
            Text(
                stringResource(Res.string.yt_sync_dialog_sign_out_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    if (confirmUndo) {
        DefaultDialog(
            onDismiss = { confirmUndo = false },
            horizontalAlignment = Alignment.Start,
            title = { Text(stringResource(Res.string.yt_sync_undo_dialog_title)) },
            buttons = {
                TextButton(onClick = { confirmUndo = false }) { Text(stringResource(Res.string.yt_sync_cancel)) }
                TextButton(onClick = {
                    confirmUndo = false
                    viewModel.undoLastSync(withYouTube = false)
                }) { Text(stringResource(Res.string.yt_sync_undo_phone)) }
                TextButton(
                    enabled = signedIn,
                    onClick = {
                        confirmUndo = false
                        viewModel.undoLastSync(withYouTube = true)
                    },
                ) { Text(stringResource(Res.string.yt_sync_undo_both)) }
            },
        ) {
            Text(
                stringResource(Res.string.yt_sync_undo_dialog_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.yt_sync_undo_both_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    lostLikes?.let { count ->
        LostLikesDialog(
            count = count,
            onDismiss = { lostLikes = null },
            onConfirm = {
                lostLikes = null
                viewModel.bringBackLikes()
            },
        )
    }

    if (confirmUpload) {
        DefaultDialog(
            onDismiss = { confirmUpload = false },
            horizontalAlignment = Alignment.Start,
            title = { Text(stringResource(Res.string.yt_sync_dialog_upload_title)) },
            buttons = {
                TextButton(onClick = { confirmUpload = false }) { Text(stringResource(Res.string.yt_sync_cancel)) }
                TextButton(onClick = {
                    confirmUpload = false
                    viewModel.uploadLocalPlaylists()
                }) { Text(stringResource(Res.string.yt_sync_dialog_upload_create)) }
            },
        ) {
            Text(
                stringResource(Res.string.yt_sync_dialog_upload_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun toggleItem(
    title: String,
    description: String,
    icon: DrawableResource,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) = SettingsItem(
    title = { Text(title) },
    highlightKey = title,
    descriptionText = description,
    icon = painterResource(icon),
    iconShape = CircleShape,
    enabled = enabled,
    onClick = { onChange(!checked) },
    trailingContent = { AppSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled) },
)

@Composable
private fun LostLikesDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.Start,
        title = {
            Text(
                if (count == 0) stringResource(Res.string.yt_sync_lost_likes_none_title)
                else pluralStringResource(Res.plurals.yt_sync_lost_likes_dialog_title, count, count),
            )
        },
        buttons = {
            if (count == 0) {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.import_sync_btn_ok)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.yt_sync_cancel)) }
                TextButton(onClick = onConfirm) { Text(stringResource(Res.string.yt_sync_lost_likes_confirm)) }
            }
        },
    ) {
        Text(
            stringResource(if (count == 0) Res.string.yt_sync_lost_likes_none_body else Res.string.yt_sync_lost_likes_dialog_body),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
