// AccountSwitcherKeys.kt

package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.stringPreferencesKey

// the signed-in YouTube accounts the user kept, as one json list; the active account stays in the InnerTube keys
val SavedAccountsKey = stringPreferencesKey("savedAccounts")
