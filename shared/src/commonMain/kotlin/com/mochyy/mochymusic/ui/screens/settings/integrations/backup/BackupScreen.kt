// mochy → settings → integrations → backup: which cloud, back up now, the schedule, and bringing a backup back

package com.mochyy.mochymusic.ui.screens.settings.integrations.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.backup
import com.mochyy.mochymusic.shared.resources.backup_connect_first
import com.mochyy.mochymusic.shared.resources.backup_disconnect
import com.mochyy.mochymusic.shared.resources.backup_disconnect_desc
import com.mochyy.mochymusic.shared.resources.backup_done_toast
import com.mochyy.mochymusic.shared.resources.backup_downloading
import com.mochyy.mochymusic.shared.resources.backup_dropbox_desc
import com.mochyy.mochymusic.shared.resources.backup_dropbox_notice
import com.mochyy.mochymusic.shared.resources.backup_finishing_sign_in
import com.mochyy.mochymusic.shared.resources.backup_group_backups
import com.mochyy.mochymusic.shared.resources.backup_group_where
import com.mochyy.mochymusic.shared.resources.backup_last
import com.mochyy.mochymusic.shared.resources.backup_last_failed
import com.mochyy.mochymusic.shared.resources.backup_never
import com.mochyy.mochymusic.shared.resources.backup_no_browser
import com.mochyy.mochymusic.shared.resources.backup_not_set_up
import com.mochyy.mochymusic.shared.resources.backup_now
import com.mochyy.mochymusic.shared.resources.backup_onedrive_desc
import com.mochyy.mochymusic.shared.resources.backup_packing
import com.mochyy.mochymusic.shared.resources.backup_page_subtitle
import com.mochyy.mochymusic.shared.resources.backup_restore_desc
import com.mochyy.mochymusic.shared.resources.backup_restore_title
import com.mochyy.mochymusic.shared.resources.backup_restored
import com.mochyy.mochymusic.shared.resources.backup_signed_in_as
import com.mochyy.mochymusic.shared.resources.backup_unpacking
import com.mochyy.mochymusic.shared.resources.backup_uploading
import com.mochyy.mochymusic.shared.resources.backup_waiting_browser
import com.mochyy.mochymusic.shared.resources.backup_webdav_connected
import com.mochyy.mochymusic.shared.resources.backup_webdav_desc
import com.mochyy.mochymusic.shared.resources.check
import com.mochyy.mochymusic.shared.resources.cloud
import com.mochyy.mochymusic.shared.resources.integration_backup_name
import com.mochyy.mochymusic.shared.resources.integrations
import com.mochyy.mochymusic.shared.resources.logout
import com.mochyy.mochymusic.shared.resources.restore
import com.mochyy.mochymusic.shared.resources.storage
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.screens.settings.integrations.IntegrationPageSubtitle
import com.mochyy.mochymusic.ui.screens.settings.storage.StorageDanger
import com.mochyy.mochymusic.ui.screens.settings.storage.StorageMuted
import com.mochyy.mochymusic.ui.screens.settings.storage.formatSize
import com.mochyy.mochymusic.ui.screens.settings.storage.storageText
import com.mochyy.mochymusic.viewmodels.BackupAccount
import com.mochyy.mochymusic.viewmodels.BackupCloud
import com.mochyy.mochymusic.viewmodels.BackupLogin
import com.mochyy.mochymusic.viewmodels.BackupStep
import com.mochyy.mochymusic.viewmodels.CloudBackupViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

const val BackupRoute = "integrations_settings/backup"

// onedrive stays off the page until its microsoft app is registered, the code behind it is ready
private val ShownClouds = listOf(BackupCloud.DROPBOX, BackupCloud.WEBDAV)

/**
 * What the page asks of the app: the browser, the toasts, the resume callback that ends a waiting
 * sign-in, and the two sheets (they live in :app's popup kit). Android passes all of them.
 */
class BackupScreenHost(
    val toast: (message: String, long: Boolean) -> Unit = { _, _ -> },
    /** true when a browser took the url, false when the phone has none */
    val openUrl: (url: String) -> Boolean = { false },
    val onResume: (@Composable (onResume: () -> Unit) -> Unit)? = null,
    val openWebDav: (CloudBackupViewModel) -> Unit = {},
    val openRestore: (CloudBackupViewModel) -> Unit = {},
)

@Composable
fun BackupScreen(
    navController: NavController,
    viewModel: CloudBackupViewModel = koinViewModel(),
    host: BackupScreenHost = BackupScreenHost(),
) {
    val scope = rememberCoroutineScope()
    val account by viewModel.account.collectAsState()
    val login by viewModel.login.collectAsState()
    val step by viewModel.step.collectAsState()
    val noBrowserMessage = stringResource(Res.string.backup_no_browser)
    val cloudLabel = account?.kind?.label.orEmpty()
    val doneMessage = if (account != null) stringResource(Res.string.backup_done_toast, cloudLabel) else ""

    LaunchedEffect(login) {
        (login as? BackupLogin.Failed)?.let {
            host.toast(it.message, true)
            viewModel.dismissLoginProblem()
        }
    }
    LaunchedEffect(step) {
        when (val now = step) {
            BackupStep.BackedUp -> {
                if (doneMessage.isNotEmpty()) host.toast(doneMessage, false)
                viewModel.acknowledge()
            }
            is BackupStep.Failed -> {
                host.toast(now.message, true)
                viewModel.acknowledge()
            }
            else -> Unit
        }
    }
    // back from the browser without an answer, give the redirect a moment to land before the rows go back to sign in
    host.onResume?.invoke {
        scope.launch {
            delay(1_500)
            viewModel.cancelWaiting()
        }
    }

    fun signIn(kind: BackupCloud) {
        if (kind == BackupCloud.WEBDAV) {
            host.openWebDav(viewModel)
            return
        }
        scope.launch {
            val uri = viewModel.loginUri(kind)
            if (!host.openUrl(uri)) {
                viewModel.cancelWaiting()
                host.toast(noBrowserMessage, true)
            }
        }
    }

    SubSettingsScaffold(
        title = stringResource(Res.string.integration_backup_name),
        onBack = { navController.navigateUp() },
        backLabel = stringResource(Res.string.integrations),
    ) {
        IntegrationPageSubtitle(stringResource(Res.string.backup_page_subtitle))
        CloudGroup(
            account = account,
            login = login,
            isConfigured = viewModel::isConfigured,
            onSignIn = ::signIn,
            onDisconnect = viewModel::disconnect,
        )
        Spacer(Modifier.height(16.dp))
        BackupsGroup(
            account = account,
            step = step,
            relativeTime = viewModel::relativeTime,
            onBackUp = viewModel::backUpNow,
            onRestore = {
                viewModel.loadBackups()
                host.openRestore(viewModel)
            },
        )
        account?.lastProblem?.takeIf { step == BackupStep.Idle }?.let { problem ->
            Text(
                stringResource(Res.string.backup_last_failed, problem),
                style = storageText(13f, FontWeight.Medium, StorageDanger, -0.2f),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        BackupScheduleCards(connected = account != null)
        Spacer(Modifier.height(140.dp))
    }
}

/** a row per cloud, the connected one ticked, and a way to disconnect */
@Composable
private fun CloudGroup(
    account: BackupAccount?,
    login: BackupLogin,
    isConfigured: (BackupCloud) -> Boolean,
    onSignIn: (BackupCloud) -> Unit,
    onDisconnect: () -> Unit,
) {
    val clouds = ShownClouds.map { kind ->
        val connected = account?.kind == kind
        val configured = isConfigured(kind)
        SettingsItem(
            title = { Text(kind.label) },
            highlightKey = kind.label,
            searchText = kind.label,
            descriptionText = when {
                login is BackupLogin.InBrowser && login.kind == kind -> stringResource(Res.string.backup_waiting_browser)
                login is BackupLogin.Finishing && login.kind == kind -> stringResource(Res.string.backup_finishing_sign_in)
                connected && kind == BackupCloud.WEBDAV -> stringResource(Res.string.backup_webdav_connected, account?.name.orEmpty())
                connected -> stringResource(Res.string.backup_signed_in_as, account?.name.orEmpty())
                !configured -> stringResource(Res.string.backup_not_set_up)
                else -> stringResource(
                    when (kind) {
                        BackupCloud.DROPBOX -> Res.string.backup_dropbox_desc
                        BackupCloud.ONEDRIVE -> Res.string.backup_onedrive_desc
                        BackupCloud.WEBDAV -> Res.string.backup_webdav_desc
                    },
                )
            },
            icon = painterResource(if (kind == BackupCloud.WEBDAV) Res.drawable.storage else Res.drawable.cloud),
            iconShape = CircleShape,
            enabled = configured,
            // tapping the connected one signs in again, to switch accounts or fix a login that stopped working
            onClick = { onSignIn(kind) },
            trailingContent = if (connected) {
                { Icon(painterResource(Res.drawable.check), contentDescription = null) }
            } else {
                null
            },
        )
    }
    val disconnect = account?.let {
        SettingsItem(
            title = { Text(stringResource(Res.string.backup_disconnect)) },
            descriptionText = stringResource(Res.string.backup_disconnect_desc, it.kind.label),
            icon = painterResource(Res.drawable.logout),
            iconShape = CircleShape,
            onClick = onDisconnect,
        )
    }
    SettingsGroup(
        title = stringResource(Res.string.backup_group_where),
        style = SettingsGroupStyle.Grouped,
        items = clouds + listOfNotNull(disconnect),
    )
    // dropbox's app is still in development mode, so new sign-ins can stop; someone already connected keeps working
    if (account?.kind != BackupCloud.DROPBOX && isConfigured(BackupCloud.DROPBOX)) {
        Text(
            stringResource(Res.string.backup_dropbox_notice),
            style = storageText(13f, FontWeight.Medium, StorageMuted, -0.2f),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )
    }
}

/** back up now, with how the last one went or how far this one is, and restore */
@Composable
private fun BackupsGroup(
    account: BackupAccount?,
    step: BackupStep,
    relativeTime: (Long) -> String,
    onBackUp: () -> Unit,
    onRestore: () -> Unit,
) {
    val resting = step == BackupStep.Idle
    val status = stepText(step) ?: when {
        account == null -> stringResource(Res.string.backup_connect_first)
        account.lastBackupAt > 0 -> stringResource(
            Res.string.backup_last,
            relativeTime(account.lastBackupAt),
            formatSize(account.lastBackupBytes),
        )
        else -> stringResource(Res.string.backup_never)
    }
    SettingsGroup(
        title = stringResource(Res.string.backup_group_backups),
        style = SettingsGroupStyle.Grouped,
        items = listOf(
            SettingsItem(
                title = { Text(stringResource(Res.string.backup_now)) },
                highlightKey = "Back up now",
                descriptionText = status,
                icon = painterResource(Res.drawable.backup),
                iconShape = CircleShape,
                enabled = account != null && resting,
                onClick = onBackUp,
            ),
            SettingsItem(
                title = { Text(stringResource(Res.string.backup_restore_title)) },
                highlightKey = "Restore a backup",
                descriptionText = stringResource(Res.string.backup_restore_desc),
                icon = painterResource(Res.drawable.restore),
                iconShape = CircleShape,
                enabled = account != null && resting,
                onClick = onRestore,
            ),
        ),
    )
}

/** how far a backup or restore has got, or null while nothing is running */
@Composable
fun stepText(step: BackupStep): String? = when (step) {
    is BackupStep.Packing -> stringResource(Res.string.backup_packing, percent(step.progress))
    is BackupStep.Uploading -> stringResource(Res.string.backup_uploading, percent(step.progress))
    is BackupStep.Downloading -> stringResource(Res.string.backup_downloading, percent(step.progress))
    is BackupStep.Unpacking -> stringResource(Res.string.backup_unpacking, percent(step.progress))
    BackupStep.Restored -> stringResource(Res.string.backup_restored)
    else -> null
}

private fun percent(progress: Float): Int = (progress * 100).toInt().coerceIn(0, 100)
