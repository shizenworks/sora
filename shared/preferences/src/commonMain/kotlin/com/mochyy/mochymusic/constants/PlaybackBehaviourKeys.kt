package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

// the device volume (percent of max) that play raises it to when pause-on-mute is on and it's at zero; 0 leaves it alone
val PauseOnMuteRecoveryPercentKey = intPreferencesKey("pauseOnMuteRecoveryPercent")
// one default for both the slider and the service, so an untouched slider means what it shows
const val PauseOnMuteRecoveryDefault = 30

// fetch lyrics for the next couple of queued songs while one plays, on unmetered networks only
val PreloadQueueLyricsKey = booleanPreferencesKey("preloadQueueLyrics")
