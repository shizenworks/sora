package com.mochyy.mochymusic.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal actual fun readPreferencesBlocking(store: DataStore<Preferences>): Preferences =
    runBlocking(Dispatchers.IO) { store.data.first() }
