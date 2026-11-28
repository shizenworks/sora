// AccountSwitcherViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.ViewModel
import com.mochyy.mochymusic.ui.screens.settings.importsync.AccountSwitcherState
import com.mochyy.mochymusic.ui.screens.settings.importsync.SavedAccountInfo
import com.mochyy.mochymusic.ui.screens.settings.importsync.SavedAccountsGateway
import kotlinx.coroutines.flow.StateFlow

/**
 * The switch-accounts page. The saved list, its switch and its removal live in [SavedAccountsGateway],
 * which on Android is backed by the sealed preferences store and the YouTube client; cookies never
 * reach this shared class, only the names and ids the page shows.
 */
class AccountSwitcherViewModel(
    private val accounts: SavedAccountsGateway,
) : ViewModel() {

    val state: StateFlow<AccountSwitcherState> = accounts.state

    fun switchTo(account: SavedAccountInfo) = accounts.switchTo(account.id)

    fun remove(account: SavedAccountInfo) = accounts.remove(account.id)
}
