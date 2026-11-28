// CloudBackupViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.constants.CloudBackupAutoKey
import com.mochyy.mochymusic.constants.CloudBackupEveryMinutesKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** the backup page under integrations: which cloud, back up now, the schedule, and restoring */
@OptIn(FlowPreview::class)
class CloudBackupViewModel(
    private val platform: CloudBackupPlatform,
    private val preferences: DataStore<Preferences>,
) : ViewModel() {
    val account: StateFlow<BackupAccount?> = platform.account
    val step: StateFlow<BackupStep> = platform.step
    val login: StateFlow<BackupLogin> = platform.login

    private val _backups = MutableStateFlow<CloudBackupList>(CloudBackupList.Loading)
    val backups: StateFlow<CloudBackupList> = _backups.asStateFlow()

    init {
        // the switch, the slider and the account all decide the schedule; a drag along the slider settles first
        viewModelScope.launch {
            combine(preferences.data, account) { prefs, account ->
                val on = (prefs[CloudBackupAutoKey] ?: false) && account != null
                on to (prefs[CloudBackupEveryMinutesKey] ?: BackupScheduleDefaults.DefaultEveryMinutes)
            }
                .distinctUntilChanged()
                // the first value is what's already scheduled, only changes after it need work
                .drop(1)
                .debounce(600)
                .collect { (on, everyMinutes) -> platform.applySchedule(on, everyMinutes) }
        }
    }

    // WebDAV has no app to register, so it is always ready to take an address
    fun isConfigured(kind: BackupCloud): Boolean = kind == BackupCloud.WEBDAV || platform.isConfigured(kind)

    suspend fun loginUri(kind: BackupCloud): String = platform.loginUri(kind)

    fun cancelWaiting() = platform.cancelWaiting()

    fun dismissLoginProblem() = platform.dismissProblem()

    /** null when the server took it, otherwise what to tell the user */
    suspend fun connectWebDav(address: String, username: String, password: String): String? =
        platform.connectWebDav(address, username, password)

    fun disconnect() {
        viewModelScope.launch { platform.disconnect() }
    }

    // a scheduled backup already running shows its own progress here, so a tap meanwhile does nothing
    fun backUpNow() {
        if (step.value != BackupStep.Idle) return
        viewModelScope.launch { platform.backUpNow() }
    }

    fun acknowledge() = platform.acknowledge()

    fun loadBackups() {
        _backups.value = CloudBackupList.Loading
        viewModelScope.launch {
            _backups.value = platform.backups().fold(
                onSuccess = { CloudBackupList.Found(it) },
                onFailure = { CloudBackupList.Failed(it.message ?: "Couldn't read the cloud backups") },
            )
        }
    }

    /** downloads and unpacks the backup, then restarts mochy so the next start swaps it in */
    fun restore(backup: BackupFile) {
        viewModelScope.launch {
            if (!platform.restore(backup)) return@launch
            // a beat on the finished state, so the restart doesn't look like a crash
            delay(1_200)
            platform.restartToFinish()
        }
    }

    /** the page's relative time for the last backup */
    fun relativeTime(millis: Long): String = platform.relativeTime(millis)
}
