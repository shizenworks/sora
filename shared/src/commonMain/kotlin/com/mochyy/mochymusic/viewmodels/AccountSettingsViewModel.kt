// AccountSettingsViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.ui.screens.settings.importsync.AccountSessionGateway
import com.mochyy.mochymusic.utils.ioDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The account settings page's sign-out and session save. The platform's [AccountSessionGateway] keeps
 * the session: Android seals it in its preferences store, and it (not this shared class) replaces the
 * process after a token change.
 */
class AccountSettingsViewModel(
    private val session: AccountSessionGateway,
) : ViewModel() {

    fun logoutAndClearSyncedContent(onCookieChange: (String) -> Unit) {
        viewModelScope.launch(ioDispatcher) {

            session.clearSyncedContent()

            session.forgetAccount()

            onCookieChange("")
        }
    }

    fun logoutKeepData(onCookieChange: (String) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            session.forgetAccount()
            withContext(Dispatchers.Main) {
                onCookieChange("")
            }
        }
    }

    fun saveTokenAndRestart(
        cookie: String,
        visitorData: String,
        dataSyncId: String,
        accountName: String,
        accountEmail: String,
        accountChannelHandle: String,
    ) {
        viewModelScope.launch(ioDispatcher) {
            session.saveSession(
                cookie = cookie,
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                accountName = accountName,
                accountEmail = accountEmail,
                accountChannelHandle = accountChannelHandle,
            )
            withContext(Dispatchers.Main) {
                session.restartApp()
            }
        }
    }
}
