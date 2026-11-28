// OnlineSearchViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.models.filterYoutubeShorts
import com.music.innertube.pages.SearchSummaryPage
import com.mochyy.mochymusic.constants.HideExplicitKey
import com.mochyy.mochymusic.constants.HideVideoSongsKey
import com.mochyy.mochymusic.constants.HideYoutubeShortsKey
import com.mochyy.mochymusic.models.ItemsPage
import com.mochyy.mochymusic.utils.get
import com.mochyy.mochymusic.utils.reportException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// Koin builds it (di/koin/OnlineKoin.kt); filters and paging stay with the app's SearchFilter and ItemsPage
class OnlineSearchViewModel(
    private val preferences: DataStore<Preferences>,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // navigation already decoded it, decoding again turned a typed + into a space
    val query = savedStateHandle.get<String>("query").orEmpty()
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        YouTube
                            .searchSummary(query)
                            .onSuccess {
                                val hideExplicit = preferences.get(HideExplicitKey, false)
                                val hideVideoSongs = preferences.get(HideVideoSongsKey, false)
                                val hideYoutubeShorts = preferences.get(HideYoutubeShortsKey, true)
                                summaryPage =
                                    it.filterExplicit(
                                        hideExplicit,
                                    ).filterVideoSongs(hideVideoSongs).filterYoutubeShorts(hideYoutubeShorts).withoutRepeats()
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                val hideExplicit = preferences.get(HideExplicitKey, false)
                                val hideVideoSongs = preferences.get(HideVideoSongsKey, false)
                                val hideYoutubeShorts = preferences.get(HideYoutubeShortsKey, true)
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                hideExplicit,
                                            )
                                            .filterVideoSongs(hideVideoSongs)
                                            .filterYoutubeShorts(hideYoutubeShorts),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    // youtube sometimes lists the same song twice in one section, and two rows with one key closed the app
    private fun SearchSummaryPage.withoutRepeats() =
        copy(summaries = summaries.map { section -> section.copy(items = section.items.distinctBy { it.id }) })

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                val hideExplicit = preferences.get(HideExplicitKey, false)
                val hideVideoSongs = preferences.get(HideVideoSongsKey, false)
                val hideYoutubeShorts = preferences.get(HideYoutubeShortsKey, true)
                val newItems = searchResult.items
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
                    .filterYoutubeShorts(hideYoutubeShorts)
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + newItems).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }
    }
}
