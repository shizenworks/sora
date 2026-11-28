
package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// android auto options that aren't in PreferenceKeys.kt, kept here so car work doesn't collide with other edits

// the car's library shows a row of songs saved on this phone (local files and downloads)
val CarDeviceSongsKey = booleanPreferencesKey("carDeviceSongs")

// starting streams in the car over mobile data; off means only songs saved or cached on the phone play there
val CarMeteredStreamingKey = booleanPreferencesKey("carMeteredStreaming")

// playlists pinned to the car's Library, in the order they were picked: their ids joined with commas
val CarPinnedPlaylistIdsKey = stringPreferencesKey("carPinnedPlaylistIds")
