package com.mochyy.mochymusic.viewmodels

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.StateFlow

// Last.fm and Libre.fm keep their API clients, queue and sign-in in the Android app; the shared
// integrations view model only asks this contract who is signed in and passes a sign-in through.

interface ScrobblePlatform {
    /** plays still waiting to be sent, for the settings page */
    val pending: StateFlow<Int>

    /** each signed-in service by its name (LASTFM, LIBREFM) and the name it is signed in as */
    fun accounts(preferences: Preferences): Map<String, String>

    suspend fun signIn(site: String, username: String, password: String): String
    suspend fun signOut(site: String)

    /** true when every waiting play went out */
    suspend fun send(): Boolean
}
