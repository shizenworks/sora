// Android's cloud backup, behind the shared backup page's contract.

package com.mochyy.mochymusic.ui.screens.settings.integrations.backup

import android.content.Context
import android.text.format.DateUtils
import com.mochyy.mochymusic.backup.CloudAccount
import com.mochyy.mochymusic.backup.CloudBackupFile
import com.mochyy.mochymusic.backup.CloudBackupManager
import com.mochyy.mochymusic.backup.CloudBackupSchedule
import com.mochyy.mochymusic.backup.CloudBackupStep
import com.mochyy.mochymusic.backup.CloudKind
import com.mochyy.mochymusic.backup.CloudLogin
import com.mochyy.mochymusic.backup.CloudSignIn
import com.mochyy.mochymusic.importer.transfer.TransferRestore
import com.mochyy.mochymusic.viewmodels.BackupAccount
import com.mochyy.mochymusic.viewmodels.BackupCloud
import com.mochyy.mochymusic.viewmodels.BackupFile
import com.mochyy.mochymusic.viewmodels.BackupLogin
import com.mochyy.mochymusic.viewmodels.BackupStep
import com.mochyy.mochymusic.viewmodels.CloudBackupPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** the app's cloud backup manager, sign-in and work schedule, behind the shared page's contract */
class AndroidCloudBackupPlatform(
    private val context: Context,
    private val manager: CloudBackupManager,
    private val signIn: CloudSignIn,
) : CloudBackupPlatform {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val account: StateFlow<BackupAccount?> = manager.account
        .map { it?.toShared() }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val step: StateFlow<BackupStep> = manager.step
        .map { it.toShared() }
        .stateIn(scope, SharingStarted.Eagerly, BackupStep.Idle)

    override val login: StateFlow<BackupLogin> = signIn.login
        .map { it.toShared() }
        .stateIn(scope, SharingStarted.Eagerly, BackupLogin.Idle)

    override fun isConfigured(kind: BackupCloud): Boolean = signIn.isConfigured(kind.toApp())

    override suspend fun loginUri(kind: BackupCloud): String = signIn.loginUri(kind.toApp()).toString()

    override fun cancelWaiting() = signIn.cancelWaiting()

    override fun dismissProblem() = signIn.dismissProblem()

    override suspend fun connectWebDav(address: String, username: String, password: String): String? =
        manager.connectWebDav(address, username, password)?.let(manager::messageOf)

    override suspend fun disconnect() = manager.disconnect()

    override suspend fun backUpNow() {
        manager.backUpNow()
    }

    override fun acknowledge() = manager.acknowledge()

    override fun applySchedule(on: Boolean, everyMinutes: Int) = CloudBackupSchedule.apply(context, on, everyMinutes)

    override suspend fun backups(): Result<List<BackupFile>> =
        manager.backups().fold(
            onSuccess = { list -> Result.success(list.map { it.toShared() }) },
            // keep the manager's own wording, the restore sheet shows it as it always has
            onFailure = { Result.failure<List<BackupFile>>(Exception(manager.messageOf(it))) },
        )

    override suspend fun restore(backup: BackupFile): Boolean =
        manager.restore(CloudBackupFile(backup.id, backup.name, backup.bytes, backup.modifiedAt))

    override suspend fun restartToFinish() = TransferRestore.restartToFinish(context)

    override fun relativeTime(millis: Long): String =
        DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

    private fun CloudAccount.toShared(): BackupAccount = BackupAccount(
        kind = kind.toShared(),
        name = name,
        lastBackupAt = lastBackupAt,
        lastBackupBytes = lastBackupBytes,
        lastProblem = lastProblem?.let(manager::messageOf),
        serverUrl = serverUrl,
        username = username,
    )

    private fun CloudBackupFile.toShared(): BackupFile = BackupFile(
        id = id,
        name = name,
        bytes = bytes,
        modifiedAt = modifiedAt,
        device = device,
    )

    private fun CloudBackupStep.toShared(): BackupStep = when (this) {
        is CloudBackupStep.Idle -> BackupStep.Idle
        is CloudBackupStep.Packing -> BackupStep.Packing(progress)
        is CloudBackupStep.Uploading -> BackupStep.Uploading(progress)
        is CloudBackupStep.Downloading -> BackupStep.Downloading(progress)
        is CloudBackupStep.Unpacking -> BackupStep.Unpacking(progress)
        is CloudBackupStep.BackedUp -> BackupStep.BackedUp
        is CloudBackupStep.Restored -> BackupStep.Restored
        is CloudBackupStep.Failed -> BackupStep.Failed(message)
    }

    private fun CloudLogin.toShared(): BackupLogin = when (this) {
        is CloudLogin.Idle -> BackupLogin.Idle
        is CloudLogin.InBrowser -> BackupLogin.InBrowser(kind.toShared())
        is CloudLogin.Finishing -> BackupLogin.Finishing(kind.toShared())
        is CloudLogin.Failed -> BackupLogin.Failed(manager.messageOf(problem))
    }

    private fun CloudKind.toShared(): BackupCloud = BackupCloud.entries.first { it.id == name }

    private fun BackupCloud.toApp(): CloudKind = CloudKind.entries.first { it.name == id }
}
