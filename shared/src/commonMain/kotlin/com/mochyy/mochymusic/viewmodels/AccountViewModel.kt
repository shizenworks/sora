// AccountViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.constants.HideYoutubeShortsKey
import com.mochyy.mochymusic.ui.screens.settings.importsync.AccountContentSource
import com.mochyy.mochymusic.ui.utils.resize
import com.mochyy.mochymusic.utils.ioDispatcher
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.filterYoutubeShorts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class AccountContentType {
    PLAYLISTS, ALBUMS, ARTISTS
}

/**
 * What the signed-in account has on YouTube Music, read through [AccountContentSource]: the client
 * that can call it lives in innertube's jvmSharedMain, which commonMain can't see.
 */
class AccountViewModel(
    private val source: AccountContentSource,
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)

    val selectedContentType = MutableStateFlow(AccountContentType.PLAYLISTS)

    private suspend fun loadPlaylists() {
        val hideYoutubeShorts = dataStore.data.map { it[HideYoutubeShortsKey] ?: true }.first()
        source.playlists(hideYoutubeShorts).onSuccess {
            playlists.value = it.filterIsInstance<PlaylistItem>()
                .filterNot { playlist -> playlist.id == "SE" }
                .filterYoutubeShorts(hideYoutubeShorts)
        }.onFailure {
            source.reportFailure(it)
        }
    }

    init {
        viewModelScope.launch {
            loadPlaylists()
            source.albums().onSuccess {
                albums.value = it.filterIsInstance<AlbumItem>()
            }.onFailure {
                source.reportFailure(it)
            }
            source.artists().onSuccess {
                artists.value = it.filterIsInstance<ArtistItem>().map { artist ->
                    artist.copy(
                        thumbnail = artist.thumbnail?.resize(1200, 1200)
                    )
                }
            }.onFailure {
                source.reportFailure(it)
            }
        }

        viewModelScope.launch(ioDispatcher) {
            dataStore.data
                .map { it[HideYoutubeShortsKey] ?: true }
                .distinctUntilChanged()
                .collect {
                    if (playlists.value != null) {
                        loadPlaylists()
                    }
                }
        }
    }

    fun setSelectedContentType(contentType: AccountContentType) {
        selectedContentType.value = contentType
    }
}
