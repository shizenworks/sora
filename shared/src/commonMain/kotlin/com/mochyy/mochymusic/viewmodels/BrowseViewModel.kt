// BrowseViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.api.YouTubeRepository
import com.music.innertube.models.YTItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// Koin builds it (di/OnlineModule.kt); browse comes from the platform's YouTube facade
class BrowseViewModel(
    private val youTube: YouTubeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId: String? = savedStateHandle.get<String>("browseId")

    val items = MutableStateFlow<List<YTItem>?>(emptyList())
    val title = MutableStateFlow<String?>("")

    init {
        viewModelScope.launch {
            browseId?.let {
                youTube.browse(browseId, null).onSuccess { result ->

                    title.value = result.title

                    val allItems = result.items.flatMap { it.items }
                    items.value = allItems
                }.onFailure {
                    reportFailure(it)
                }
            }
        }
    }
}
