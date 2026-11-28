// OnlinePlaylistViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.api.YouTubeRepository
import com.mochyy.mochymusic.constants.HideVideoSongsKey
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.utils.PreferencesCache
import com.mochyy.mochymusic.utils.ioDispatcher
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterVideoSongs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Koin builds it (di/OnlineModule.kt); the playlist comes from the platform's YouTube facade
class OnlinePlaylistViewModel(
    private val youTube: YouTubeRepository,
    private val preferences: DataStore<Preferences>,
    database: MochyDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val playlistId = savedStateHandle.get<String>("playlistId")!!

    val playlist = MutableStateFlow<PlaylistItem?>(null)
    val playlistSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val relatedItems = MutableStateFlow<List<YTItem>>(emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    val dbPlaylist = database.playlistDao.playlistByBrowseId(playlistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    var continuation: String? = null
        private set

    private var proactiveLoadJob: Job? = null

    init {
        fetchInitialPlaylistData()
    }

    private fun fetchInitialPlaylistData() {
        viewModelScope.launch(ioDispatcher) {
            _isLoading.value = true
            _error.value = null
            continuation = null
            proactiveLoadJob?.cancel()

            youTube.playlist(playlistId)
                .onSuccess { playlistPage ->
                    playlist.value = playlistPage.playlist
                    playlistSongs.value = applySongFilters(playlistPage.songs)
                    relatedItems.value = playlistPage.related ?: emptyList()
                    continuation = playlistPage.songsContinuation
                    _isLoading.value = false
                    if (continuation != null) {
                        startProactiveBackgroundLoading()
                    }
                }.onFailure { throwable ->
                    _error.value = throwable.message ?: "Failed to load playlist"
                    _isLoading.value = false
                    reportFailure(throwable)
                }
        }
    }

    private fun startProactiveBackgroundLoading() {
        proactiveLoadJob?.cancel()
        proactiveLoadJob = viewModelScope.launch(ioDispatcher) {
            var currentProactiveToken = continuation
            while (currentProactiveToken != null && isActive) {

                if (_isLoadingMore.value) {

                    break
                }

                youTube.playlistContinuation(currentProactiveToken)
                    .onSuccess { playlistContinuationPage ->
                        val currentSongs = playlistSongs.value.toMutableList()
                        currentSongs.addAll(playlistContinuationPage.songs)
                        playlistSongs.value = applySongFilters(currentSongs)
                        currentProactiveToken = playlistContinuationPage.continuation

                        this@OnlinePlaylistViewModel.continuation = currentProactiveToken
                    }.onFailure { throwable ->
                        reportFailure(throwable)
                        currentProactiveToken = null
                    }
            }

        }
    }

    fun loadMoreSongs() {
        if (_isLoadingMore.value) return

        val tokenForManualLoad = continuation ?: return

        proactiveLoadJob?.cancel()
        _isLoadingMore.value = true

        viewModelScope.launch(ioDispatcher) {
            youTube.playlistContinuation(tokenForManualLoad)
                .onSuccess { playlistContinuationPage ->
                    val currentSongs = playlistSongs.value.toMutableList()
                    currentSongs.addAll(playlistContinuationPage.songs)
                    playlistSongs.value = applySongFilters(currentSongs)
                    continuation = playlistContinuationPage.continuation
                }.onFailure { throwable ->
                    reportFailure(throwable)
                }.also {
                    _isLoadingMore.value = false

                    if (continuation != null && isActive) {
                        startProactiveBackgroundLoading()
                    }
                }
        }
    }

    fun retry() {
        proactiveLoadJob?.cancel()
        fetchInitialPlaylistData()
    }

    private fun applySongFilters(songs: List<SongItem>): List<SongItem> {
        val hideVideoSongs = PreferencesCache.current(preferences)[HideVideoSongsKey] ?: false
        return songs
            .distinctBy { it.id }
            .filterVideoSongs(hideVideoSongs)
    }

    override fun onCleared() {
        super.onCleared()
        proactiveLoadJob?.cancel()
    }
}
