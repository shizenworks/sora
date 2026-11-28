// Android's scrobbling accounts and queue, behind the shared integrations view model's contract.

package com.mochyy.mochymusic.ui.screens.settings.integrations

import androidx.datastore.preferences.core.Preferences
import com.mochyy.mochymusic.scrobble.ScrobbleSite
import com.mochyy.mochymusic.scrobble.Scrobbler
import com.mochyy.mochymusic.viewmodels.ScrobblePlatform
import kotlinx.coroutines.flow.StateFlow

class AndroidScrobblePlatform(
    private val scrobbler: Scrobbler,
) : ScrobblePlatform {

    override val pending: StateFlow<Int> = scrobbler.pending

    override fun accounts(preferences: Preferences): Map<String, String> =
        ScrobbleSite.entries.mapNotNull { site ->
            val name = preferences[site.usernameKey]?.takeIf { preferences[site.sessionKey] != null }
            name?.let { site.name to it }
        }.toMap()

    override suspend fun signIn(site: String, username: String, password: String): String =
        scrobbler.signIn(siteOf(site), username, password)

    override suspend fun signOut(site: String) = scrobbler.signOut(siteOf(site))

    override suspend fun send(): Boolean = scrobbler.send()

    private fun siteOf(name: String): ScrobbleSite = ScrobbleSite.entries.first { it.name == name }
}
