// ExploreViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.api.YouTubeRepository
import com.mochyy.mochymusic.constants.HideExplicitKey
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.utils.PreferencesCache
import com.mochyy.mochymusic.utils.ioDispatcher
import com.music.innertube.models.filterExplicit
import com.music.innertube.pages.ExplorePage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Koin builds it (di/OnlineModule.kt); the page comes from the platform's YouTube facade
class ExploreViewModel(
    private val youTube: YouTubeRepository,
    private val database: MochyDatabase,
    private val preferences: DataStore<Preferences>,
) : ViewModel() {
    val explorePage = MutableStateFlow<ExplorePage?>(null)

    private suspend fun load() {
        youTube
            .explore()
            .onSuccess { page ->
                val artists: MutableMap<Int, String> = mutableMapOf()
                val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                database.historyDao.allArtistsByPlayTime().first().let { list ->
                    var favIndex = 0
                    for ((artistsIndex, artist) in list.withIndex()) {
                        artists[artistsIndex] = artist.id
                        if (artist.artist.bookmarkedAt != null) {
                            favouriteArtists[favIndex] = artist.id
                            favIndex++
                        }
                    }
                }
                explorePage.value =
                    page.copy(
                        newReleaseAlbums =
                        page.newReleaseAlbums
                            .sortedBy { album ->
                                val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                val firstArtistKey =
                                    artistIds.firstNotNullOfOrNull { artistId ->
                                        if (artistId in favouriteArtists.values) {
                                            favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                        } else {
                                            artists.entries.firstOrNull { it.value == artistId }?.key
                                        }
                                    } ?: Int.MAX_VALUE
                                firstArtistKey
                            }.filterExplicit(PreferencesCache.current(preferences)[HideExplicitKey] ?: false),
                    )
            }.onFailure {
                reportFailure(it)
            }
    }

    init {
        viewModelScope.launch(ioDispatcher) {
            load()
        }
    }
}
