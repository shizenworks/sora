package com.mochyy.mochymusic.shared.settings

import androidx.datastore.preferences.core.booleanPreferencesKey

// Android reads the same names from its existing encrypted store.
object PlaybackPreferenceKeys {
    val showFormatBadge = booleanPreferencesKey("showFormatBadge")
    val showSampleRate = booleanPreferencesKey("showFormatSampleRate")
}
