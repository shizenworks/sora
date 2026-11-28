package com.mochyy.mochymusic.ui.screens.settings.importsync

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.mochyy.mochymusic.constants.YtmSyncKey
import com.mochyy.mochymusic.importer.ResyncStart
import com.mochyy.mochymusic.importer.account.AccountImportState
import com.mochyy.mochymusic.importer.account.AccountService
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.account
import com.mochyy.mochymusic.shared.resources.account_import_btn_multiple
import com.mochyy.mochymusic.shared.resources.account_import_btn_single
import com.mochyy.mochymusic.shared.resources.account_import_choose_what
import com.mochyy.mochymusic.shared.resources.account_import_in_library_count
import com.mochyy.mochymusic.shared.resources.account_import_manage_on_sync
import com.mochyy.mochymusic.shared.resources.account_import_next_add_yt
import com.mochyy.mochymusic.shared.resources.account_import_no_playlists
import com.mochyy.mochymusic.shared.resources.account_import_not_in_account
import com.mochyy.mochymusic.shared.resources.account_import_paste_link_instead
import com.mochyy.mochymusic.shared.resources.account_import_pick_resync_prompt
import com.mochyy.mochymusic.shared.resources.account_import_reading_count
import com.mochyy.mochymusic.shared.resources.account_import_resync_subtitle
import com.mochyy.mochymusic.shared.resources.account_import_select_all
import com.mochyy.mochymusic.shared.resources.account_import_select_none
import com.mochyy.mochymusic.shared.resources.account_import_signed_in_service
import com.mochyy.mochymusic.shared.resources.account_import_signin_again
import com.mochyy.mochymusic.shared.resources.account_import_signin_subtitle
import com.mochyy.mochymusic.shared.resources.account_import_signin_title
import com.mochyy.mochymusic.shared.resources.account_import_song_single
import com.mochyy.mochymusic.shared.resources.account_import_songs_multiple
import com.mochyy.mochymusic.shared.resources.account_import_stop
import com.mochyy.mochymusic.shared.resources.account_import_tick_below
import com.mochyy.mochymusic.shared.resources.account_import_try_again
import com.mochyy.mochymusic.shared.resources.account_import_use_different
import com.mochyy.mochymusic.shared.resources.account_import_ytm_needs_signin
import com.mochyy.mochymusic.shared.resources.account_import_ytm_sync_on_notice
import com.mochyy.mochymusic.shared.resources.account_resync_looking
import com.mochyy.mochymusic.shared.resources.account_resync_missing
import com.mochyy.mochymusic.shared.resources.check
import com.mochyy.mochymusic.shared.resources.close
import com.mochyy.mochymusic.shared.resources.favorite
import com.mochyy.mochymusic.shared.resources.import_sync_btn_ok
import com.mochyy.mochymusic.shared.resources.link
import com.mochyy.mochymusic.shared.resources.login
import com.mochyy.mochymusic.shared.resources.playlist_add
import com.mochyy.mochymusic.shared.resources.playlist_play
import com.mochyy.mochymusic.shared.resources.refresh
import com.mochyy.mochymusic.shared.resources.yt_login_signed_in_as
import com.mochyy.mochymusic.shared.resources.yt_sync_sign_in_title
import com.mochyy.mochymusic.ui.component.DefaultDialog
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.viewmodels.AccountImportViewModel
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

const val AccountImportRoutePattern = "account_import/{service}?resync={resync}&then={then}"

fun accountImportRoute(service: AccountService) = "account_import/${service.route}"

/** the same sign-in, opened by a playlist's re-sync so it reads just that playlist again */
fun accountResyncRoute(
    service: AccountService,
    playlistId: String,
) = "account_import/${service.route}?resync=$playlistId"

/** what the page's resync argument says for re-sync all */
const val ResyncAllArg = "all"

/** re-sync all through each service's sign-in in turn, services first to last */
fun accountResyncAllRoute(services: List<AccountService>) =
    "account_import/${services.first().route}?resync=$ResyncAllArg&then=${services.drop(1).joinToString(",") { it.route }}"

/**
 * The account import page. The sign-in web view itself is the platform's: [signInSurface] draws it
 * and reports what the page sends, so Android keeps its WebView capture while desktop and iOS can
 * bring their own later. A platform that passes none never opens this page.
 */
@Composable
fun AccountImportScreen(
    navController: NavController,
    viewModel: AccountImportViewModel = koinViewModel(),
    importViewModel: ImportSyncViewModel = koinViewModel(),
    signInSurface: AccountSignInSurface? = null,
    onClearWebSession: ((AccountService) -> Unit)? = null,
    resyncLinkDialog: (@Composable (sourceLabel: String, onDismiss: () -> Unit, onDone: (String) -> Unit) -> Unit)? = null,
    notify: (String) -> Unit = {},
    notifyShort: (String) -> Unit = notify,
) {
    val service = viewModel.service
    val state by viewModel.state.collectAsState()
    val ready by viewModel.ready.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val resyncStart by viewModel.resyncStart.collectAsState()
    val signedInToYouTube = rememberYouTubeSignedIn()
    var askingForLink by remember { mutableStateOf(false) }

    val resyncStartedPrefix = stringResource(Res.string.account_resync_looking, service.label)
    val missingNow = viewModel.missingFromAccount
    val resyncStarted =
        resyncStartedPrefix +
            if (missingNow.isEmpty()) "" else stringResource(Res.string.account_resync_missing, missingNow.joinToString(", "))

    // a re-sync carries on in the background once its songs are read, the toast at the end says how it went
    LaunchedEffect(resyncStart) {
        if (resyncStart != ResyncStart.Started) return@LaunchedEffect
        notify(resyncStarted)
        navController.navigateUp()
        // re-sync all: on to the next service that needs signing in
        if (viewModel.nextServices.isNotEmpty()) navController.navigate(accountResyncAllRoute(viewModel.nextServices))
    }

    // read and ready: on to the same review dialog every import uses
    LaunchedEffect(ready) {
        ready?.let {
            importViewModel.offer(it, service.label)
            viewModel.consumeReady()
        }
    }

    if (service == AccountService.YOUTUBE_MUSIC) {
        LaunchedEffect(signedInToYouTube) {
            if (signedInToYouTube && viewModel.state.value is AccountImportState.NeedsYouTubeSignIn) viewModel.refreshYouTube()
        }
    }

    fun signInAgain() {
        onClearWebSession?.invoke(service)
        viewModel.signInAgain()
    }

    when (val current = state) {
        AccountImportState.SigningIn -> {
            if (signInSurface != null) {
                signInSurface.Content(
                    AccountSignInPage(
                        service = service,
                        title = stringResource(Res.string.account_import_signin_title, service.label),
                        subtitle =
                            if (viewModel.isResync) {
                                stringResource(Res.string.account_import_resync_subtitle, service.label)
                            } else {
                                stringResource(Res.string.account_import_signin_subtitle, service.label)
                            },
                        onHeader = viewModel::onHeader,
                        onApplePageProbe = viewModel::onApplePageProbe,
                        onCookies = viewModel::onCookies,
                        onExit = { navController.navigateUp() },
                    )
                )
            }
        }

        else -> {
            SubSettingsScaffold(
                title = service.label,
                onBack = { navController.navigateUp() },
            ) {
                when (current) {
                    AccountImportState.SigningIn -> {
                        Unit
                    }

                    AccountImportState.NeedsYouTubeSignIn -> {
                        ImportInfoCard {
                            Text(
                                stringResource(Res.string.account_import_ytm_needs_signin),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        SettingsGroup(
                            style = SettingsGroupStyle.Grouped,
                            items =
                                listOf(
                                    SettingsItem(
                                        title = { Text(stringResource(Res.string.yt_sync_sign_in_title)) },
                                        icon = painterResource(Res.drawable.login),
                                        iconShape = CircleShape,
                                        onClick = { navController.navigate(YouTubeLoginRoute) },
                                    ),
                                ),
                        )
                    }

                    is AccountImportState.Loading -> {
                        BusyCard(current.label, fraction = null)
                    }

                    is AccountImportState.Reading -> {
                        BusyCard(
                            label =
                                "${current.label} " +
                                    stringResource(
                                        Res.string.account_import_reading_count,
                                        current.done + 1,
                                        current.total,
                                    ),
                            fraction = current.done.toFloat() / current.total.coerceAtLeast(1),
                        )
                        Spacer(Modifier.height(16.dp))
                        SettingsGroup(
                            style = SettingsGroupStyle.Grouped,
                            items =
                                listOf(
                                    SettingsItem(
                                        title = { Text(stringResource(Res.string.account_import_stop)) },
                                        icon = painterResource(Res.drawable.close),
                                        iconShape = CircleShape,
                                        onClick = viewModel::cancelReading,
                                    ),
                                ),
                        )
                    }

                    is AccountImportState.Failed -> {
                        ImportInfoCard {
                            Text(current.message, style = MaterialTheme.typography.bodyLarge)
                        }
                        Spacer(Modifier.height(16.dp))
                        SettingsGroup(
                            style = SettingsGroupStyle.Grouped,
                            items =
                                listOfNotNull(
                                    if (!current.signedOut) {
                                        SettingsItem(
                                            title = { Text(stringResource(Res.string.account_import_try_again)) },
                                            icon = painterResource(Res.drawable.refresh),
                                            iconShape = CircleShape,
                                            onClick = viewModel::retry,
                                        )
                                    } else {
                                        null
                                    },
                                    SettingsItem(
                                        title = { Text(stringResource(Res.string.account_import_signin_again)) },
                                        icon = painterResource(Res.drawable.login),
                                        iconShape = CircleShape,
                                        onClick = {
                                            if (service == AccountService.YOUTUBE_MUSIC) {
                                                navController.navigate(YouTubeLoginRoute)
                                            } else {
                                                signInAgain()
                                            }
                                        },
                                    ),
                                ),
                        )
                    }

                    is AccountImportState.Choosing -> {
                        // read once: Choosing now comes from shared, where a property can't be smart cast
                        val pickingFor = current.pickingFor
                        if (pickingFor != null) {
                            PickForResyncContent(
                                service = service,
                                state = current,
                                name = pickingFor,
                                onPick = viewModel::pickForResync,
                                onPasteLink = { askingForLink = true },
                            )
                        } else {
                            ChoosingContent(
                                service = service,
                                state = current,
                                onToggle = viewModel::toggle,
                                onSelectAll = viewModel::selectAll,
                                onSelectNone = viewModel::selectNone,
                                onImport = viewModel::readSelected,
                                onSwitchAccount = {
                                    if (service == AccountService.YOUTUBE_MUSIC) navController.navigate(YouTubeSyncRoute) else signInAgain()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    notice?.let { message ->
        DefaultDialog(
            onDismiss = viewModel::clearNotice,
            buttons = { TextButton(onClick = viewModel::clearNotice) { Text(stringResource(Res.string.import_sync_btn_ok)) } },
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }

    (resyncStart as? ResyncStart.Failed)?.let { failed ->
        DefaultDialog(
            onDismiss = viewModel::clearResyncStart,
            buttons = { TextButton(onClick = viewModel::clearResyncStart) { Text(stringResource(Res.string.import_sync_btn_ok)) } },
        ) {
            Text(failed.message, style = MaterialTheme.typography.bodyLarge)
        }
    }

    if (askingForLink && resyncLinkDialog != null) {
        resyncLinkDialog(
            service.label,
            { askingForLink = false },
            viewModel::resyncFromLink,
        )
    }

    PendingImportHost(viewModel = importViewModel, navController = navController, notify = notify, notifyAlreadyRunning = notifyShort)
}

/** a re-sync that couldn't find its playlist by name, the user says which one in the account it is */
@Composable
private fun PickForResyncContent(
    service: AccountService,
    state: AccountImportState.Choosing,
    name: String,
    onPick: (String) -> Unit,
    onPasteLink: () -> Unit,
) {
    ImportInfoCard {
        Text(
            stringResource(Res.string.account_import_pick_resync_prompt, name, service.label),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    Spacer(Modifier.height(16.dp))
    SettingsGroup(
        style = SettingsGroupStyle.Grouped,
        items =
            listOf(
                SettingsItem(
                    title = { Text(stringResource(Res.string.account_import_not_in_account)) },
                    descriptionText = stringResource(Res.string.account_import_paste_link_instead),
                    icon = painterResource(Res.drawable.link),
                    iconShape = CircleShape,
                    onClick = onPasteLink,
                ),
            ),
    )
    if (state.playlists.isEmpty()) return
    Spacer(Modifier.height(16.dp))
    SettingsGroup(
        title = stringResource(Res.string.account_import_in_library_count, state.playlists.size),
        style = SettingsGroupStyle.Grouped,
        items =
            state.playlists.map { playlist ->
                SettingsItem(
                    title = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    descriptionText =
                        listOfNotNull(
                            playlist.trackCount?.let {
                                if (it ==
                                    1
                                ) {
                                    stringResource(Res.string.account_import_song_single)
                                } else {
                                    stringResource(Res.string.account_import_songs_multiple, it)
                                }
                            },
                            playlist.subtitle,
                        ).joinToString(" · ").ifEmpty { null },
                    icon = playlist.coverUrl?.let { rememberAsyncImagePainter(it) } ?: painterResource(Res.drawable.playlist_play),
                    tintIcon = playlist.coverUrl == null,
                    iconShape = CircleShape,
                    onClick = { onPick(playlist.id) },
                )
            },
    )
}

@Composable
private fun BusyCard(
    label: String,
    fraction: Float?,
) {
    ImportInfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (fraction != null) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ChoosingContent(
    service: AccountService,
    state: AccountImportState.Choosing,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onImport: () -> Unit,
    onSwitchAccount: () -> Unit,
) {
    val autoSync by rememberPreference(YtmSyncKey, true)
    val selectedCount = state.selected.size
    val allSelected = selectedCount == state.playlists.size && selectedCount > 0

    SettingsGroup(
        style = SettingsGroupStyle.Grouped,
        items =
            listOf(
                SettingsItem(
                    title = {
                        Text(
                            state.accountName?.let { stringResource(Res.string.yt_login_signed_in_as, it) }
                                ?: stringResource(Res.string.account_import_signed_in_service, service.label),
                        )
                    },
                    descriptionText =
                        if (service ==
                            AccountService.YOUTUBE_MUSIC
                        ) {
                            stringResource(Res.string.account_import_manage_on_sync)
                        } else {
                            stringResource(Res.string.account_import_use_different)
                        },
                    icon = painterResource(Res.drawable.account),
                    iconShape = CircleShape,
                    onClick = onSwitchAccount,
                ),
                SettingsItem(
                    title = {
                        Text(
                            when (selectedCount) {
                                0 -> stringResource(Res.string.account_import_choose_what)
                                1 -> stringResource(Res.string.account_import_btn_single)
                                else -> stringResource(Res.string.account_import_btn_multiple, selectedCount)
                            },
                        )
                    },
                    descriptionText =
                        if (selectedCount ==
                            0
                        ) {
                            stringResource(Res.string.account_import_tick_below)
                        } else {
                            stringResource(Res.string.account_import_next_add_yt)
                        },
                    icon = painterResource(Res.drawable.playlist_add),
                    iconShape = CircleShape,
                    enabled = selectedCount > 0,
                    onClick = onImport,
                ),
                SettingsItem(
                    title = {
                        Text(
                            if (allSelected) {
                                stringResource(
                                    Res.string.account_import_select_none,
                                )
                            } else {
                                stringResource(Res.string.account_import_select_all)
                            },
                        )
                    },
                    icon = painterResource(Res.drawable.check),
                    iconShape = CircleShape,
                    onClick = if (allSelected) onSelectNone else onSelectAll,
                ),
            ),
    )

    if (service == AccountService.YOUTUBE_MUSIC && autoSync) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(Res.string.account_import_ytm_sync_on_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Spacer(Modifier.height(16.dp))

    if (state.playlists.isEmpty()) {
        ImportInfoCard {
            Text(stringResource(Res.string.account_import_no_playlists), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    SettingsGroup(
        title = stringResource(Res.string.account_import_in_library_count, state.playlists.size),
        style = SettingsGroupStyle.Grouped,
        items =
            state.playlists.map { playlist ->
                val checked = playlist.id in state.selected
                SettingsItem(
                    title = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    descriptionText =
                        listOfNotNull(
                            playlist.trackCount?.let {
                                if (it ==
                                    1
                                ) {
                                    stringResource(Res.string.account_import_song_single)
                                } else {
                                    stringResource(Res.string.account_import_songs_multiple, it)
                                }
                            },
                            playlist.subtitle,
                        ).joinToString(" · ").ifEmpty { null },
                    // the playlist's own cover from the service, when it has one
                    icon =
                        playlist.coverUrl?.let { rememberAsyncImagePainter(it) }
                            ?: painterResource(if (playlist.isLiked) Res.drawable.favorite else Res.drawable.playlist_play),
                    tintIcon = playlist.coverUrl == null,
                    iconShape = CircleShape,
                    onClick = { onToggle(playlist.id) },
                    trailingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle(playlist.id) }) },
                )
            },
    )
}
