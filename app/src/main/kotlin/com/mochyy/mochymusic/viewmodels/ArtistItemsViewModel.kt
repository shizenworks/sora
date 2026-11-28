// ArtistItemsViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.mochyy.mochymusic.constants.HideExplicitKey
import com.mochyy.mochymusic.constants.HideVideoSongsKey
import com.mochyy.mochymusic.models.ItemsPage
import com.mochyy.mochymusic.utils.get
import com.mochyy.mochymusic.utils.reportException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Koin builds it (di/koin/OnlineKoin.kt); the artist shelves stay on the app's ItemsPage
class ArtistItemsViewModel(
    private val preferences: DataStore<Preferences>,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artistId: String? = savedStateHandle.get<String>("artistId")
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val title = MutableStateFlow("")
    val itemsPage = MutableStateFlow<ItemsPage?>(null)
    val loadFailed = MutableStateFlow(false)
    private var loadingMore = false

    init {
        load()
    }

    fun retry() {
        loadFailed.value = false
        load()
    }

    private fun load() {
        viewModelScope.launch {
            YouTube
                .artistItems(
                    BrowseEndpoint(
                        browseId = browseId,
                        params = params,
                    ),
                ).onSuccess { artistItemsPage ->
                    val hideExplicit = preferences.get(HideExplicitKey, false)
                    val hideVideoSongs = preferences.get(HideVideoSongsKey, false)
                    title.value = artistItemsPage.title
                    itemsPage.value =
                        ItemsPage(
                            items = artistItemsPage.items
                                .distinctBy { it.id }
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs),
                            continuation = artistItemsPage.continuation,
                        )
                }.onFailure {
                    loadFailed.value = true
                    reportException(it)
                }
        }
    }

    fun loadMore() {
        // the list asks again on every frame the last row is in view; one page at a time
        if (loadingMore) return
        val oldItemsPage = itemsPage.value ?: return
        val continuation = oldItemsPage.continuation ?: return
        loadingMore = true
        viewModelScope.launch {
            YouTube
                .artistItemsContinuation(continuation)
                .onSuccess { artistItemsContinuationPage ->
                    val hideExplicit = preferences.get(HideExplicitKey, false)
                    val hideVideoSongs = preferences.get(HideVideoSongsKey, false)
                    itemsPage.update {
                        ItemsPage(
                            items =
                            (oldItemsPage.items + artistItemsContinuationPage.items)
                                .distinctBy { it.id }
                                .filterExplicit(hideExplicit)
                                .filterVideoSongs(hideVideoSongs),
                            continuation = artistItemsContinuationPage.continuation,
                        )
                    }
                }.onFailure {
                    reportException(it)
                }
            loadingMore = false
        }
    }
}
