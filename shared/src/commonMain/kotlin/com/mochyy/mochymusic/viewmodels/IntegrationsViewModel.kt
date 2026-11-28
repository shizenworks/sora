// IntegrationsViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings → Integrations: the scrobbling accounts, and the plays still waiting to be sent. */
class IntegrationsViewModel(
    private val platform: ScrobblePlatform,
    preferences: DataStore<Preferences>,
) : ViewModel() {

    /** The name each service is signed in as, by service name; a service that isn't signed in is left out. */
    val accounts: StateFlow<Map<String, String>> = preferences.data
        .map { prefs -> platform.accounts(prefs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val pending: StateFlow<Int> = platform.pending

    /** What "Send now" did: true when everything went out, false when plays were kept for later. */
    private val _sendResult = MutableStateFlow<Boolean?>(null)
    val sendResult: StateFlow<Boolean?> = _sendResult.asStateFlow()

    suspend fun signIn(site: String, username: String, password: String): String =
        platform.signIn(site, username, password)

    fun signOut(site: String) {
        viewModelScope.launch { platform.signOut(site) }
    }

    fun sendNow() {
        viewModelScope.launch { _sendResult.value = platform.send() }
    }

    fun clearSendResult() {
        _sendResult.value = null
    }
}
