// YouTubeBrowseViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.api.YouTubeRepository
import com.mochyy.mochymusic.constants.HideExplicitKey
import com.mochyy.mochymusic.constants.HideVideoSongsKey
import com.mochyy.mochymusic.constants.HideYoutubeShortsKey
import com.mochyy.mochymusic.utils.PreferencesCache
import com.music.innertube.pages.BrowseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// Koin builds it (di/OnlineModule.kt); browse comes from the platform's YouTube facade
class YouTubeBrowseViewModel(
    private val youTube: YouTubeRepository,
    private val preferences: DataStore<Preferences>,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            val prefs = PreferencesCache.current(preferences)
            val hideExplicit = prefs[HideExplicitKey] ?: false
            val hideVideoSongs = prefs[HideVideoSongsKey] ?: false
            val hideYoutubeShorts = prefs[HideYoutubeShortsKey] ?: true
            youTube
                .browse(browseId, params)
                .onSuccess {
                    result.value = it
                        .filterExplicit(hideExplicit)
                        .filterVideoSongs(hideVideoSongs)
                        .filterYoutubeShorts(hideYoutubeShorts)
                }.onFailure {
                    reportFailure(it)
                }
        }
    }
}
