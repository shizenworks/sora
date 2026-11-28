// MoodAndGenresViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.api.YouTubeRepository
import com.mochyy.mochymusic.utils.ioDispatcher
import com.music.innertube.models.YTItem
import com.music.innertube.pages.MoodAndGenres
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private object MoodCoverCache {
    val covers = mutableStateMapOf<String, List<String>>()
    private val requested = MutableStateFlow<Set<String>>(emptySet())

    /** adds the key unless a request for it is already running; one caller gets true */
    fun claim(key: String): Boolean {
        while (true) {
            val current = requested.value
            if (key in current) return false
            if (requested.compareAndSet(current, current + key)) return true
        }
    }

    fun release(key: String) {
        requested.update { it - key }
    }
}

fun coverKey(browseId: String, params: String?): String = "$browseId|${params.orEmpty()}"

// Koin builds it (di/OnlineModule.kt); the page and covers come from the platform's YouTube facade
class MoodAndGenresViewModel(
    private val youTube: YouTubeRepository,
) : ViewModel() {
    val moodAndGenres = MutableStateFlow<List<MoodAndGenres>?>(null)

    val covers = MoodCoverCache.covers

    private val gate = Semaphore(3)

    init {
        // on io: the page is parsed where it's read, and on the main thread that hitched the search tab opening
        viewModelScope.launch(ioDispatcher) {
            youTube
                .moodAndGenres()
                .onSuccess {
                    moodAndGenres.value = it
                }.onFailure {
                    reportFailure(it)
                }
        }
    }

    fun requestCovers(browseId: String, params: String?) {
        val key = coverKey(browseId, params)
        if (!MoodCoverCache.claim(key)) return
        viewModelScope.launch(ioDispatcher) {
            gate.withPermit {
                youTube.browse(browseId, params)
                    .onSuccess { result ->
                        val urls = result.items
                            .asSequence()
                            .flatMap { it.items.asSequence() }
                            .mapNotNull(YTItem::thumbnail)
                            .distinct()
                            .take(2)
                            .toList()
                        if (urls.isNotEmpty()) {
                            MoodCoverCache.covers[key] = urls
                        }
                    }
                    .onFailure {

                        MoodCoverCache.release(key)
                    }
            }
        }
    }
}
