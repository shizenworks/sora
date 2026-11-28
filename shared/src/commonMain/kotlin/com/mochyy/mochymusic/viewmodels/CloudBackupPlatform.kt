package com.mochyy.mochymusic.viewmodels

import kotlinx.coroutines.flow.StateFlow

// Cloud backup is OAuth browsers, Android activities, WorkManager schedules and a restart to finish.
// The shared backup page reads this contract; the Android app backs it with CloudBackupManager,
// CloudSignIn and CloudBackupSchedule, so nothing about the cloud itself moves.

/** the clouds the page offers */
enum class BackupCloud(val id: String, val label: String) {
    DROPBOX("Dropbox", "Dropbox"),
    ONEDRIVE("OneDrive", "OneDrive"),
    WEBDAV("WebDav", "WebDAV"),
}

/** the connected cloud, in the parts the page shows */
data class BackupAccount(
    val kind: BackupCloud,
    val name: String,
    val lastBackupAt: Long = 0L,
    val lastBackupBytes: Long = 0L,
    /** why the last scheduled backup didn't go through, already worded; null once one works again */
    val lastProblem: String? = null,
    /** the server address, for the WebDAV sign-in sheet to start from */
    val serverUrl: String = "",
    val username: String = "",
)

/** where a backup or a restore stands; progress runs from 0 to 1 */
sealed interface BackupStep {
    data object Idle : BackupStep
    data class Packing(val progress: Float) : BackupStep
    data class Uploading(val progress: Float) : BackupStep
    data class Downloading(val progress: Float) : BackupStep
    data class Unpacking(val progress: Float) : BackupStep
    data object BackedUp : BackupStep
    data object Restored : BackupStep
    data class Failed(val message: String) : BackupStep
}

/** where a browser sign-in to Dropbox or OneDrive is */
sealed interface BackupLogin {
    data object Idle : BackupLogin
    data class InBrowser(val kind: BackupCloud) : BackupLogin
    data class Finishing(val kind: BackupCloud) : BackupLogin
    data class Failed(val message: String) : BackupLogin
}

/** one backup zip sitting in the cloud */
data class BackupFile(
    val id: String,
    val name: String,
    val bytes: Long,
    val modifiedAt: Long,
    /** the phone it came from, read back out of the name the backup was given */
    val device: String,
)

/** what the restore sheet shows: still asking the cloud, the backups it found, or why it couldn't */
sealed interface CloudBackupList {
    data object Loading : CloudBackupList
    data class Found(val backups: List<BackupFile>) : CloudBackupList
    data class Failed(val message: String) : CloudBackupList
}

/** the schedule defaults the page's sliders step through */
object BackupScheduleDefaults {
    const val DefaultEveryMinutes = 24 * 60
    const val DefaultKeep = 5

    // 30 minutes up to a week, the steps the "how often" slider snaps to
    val EveryMinutesSteps = listOf(30, 60, 3 * 60, 6 * 60, 12 * 60, 24 * 60, 3 * 24 * 60, 7 * 24 * 60)
    val KeepSteps = (1..10).toList()
}

interface CloudBackupPlatform {
    val account: StateFlow<BackupAccount?>
    val step: StateFlow<BackupStep>
    val login: StateFlow<BackupLogin>

    fun isConfigured(kind: BackupCloud): Boolean
    suspend fun loginUri(kind: BackupCloud): String
    fun cancelWaiting()
    fun dismissProblem()

    /** null when the server took it, otherwise what to tell the user */
    suspend fun connectWebDav(address: String, username: String, password: String): String?
    suspend fun disconnect()
    suspend fun backUpNow()
    fun acknowledge()

    /** starts or stops the periodic backup, the way the platform schedules work */
    fun applySchedule(on: Boolean, everyMinutes: Int)

    suspend fun backups(): Result<List<BackupFile>>
    suspend fun restore(backup: BackupFile): Boolean

    /** restarts mochy after a restore, so the next start swaps the restored data in */
    suspend fun restartToFinish()

    /** the page's own relative time ("2 hours ago"), worded the platform's usual way */
    fun relativeTime(millis: Long): String
}
