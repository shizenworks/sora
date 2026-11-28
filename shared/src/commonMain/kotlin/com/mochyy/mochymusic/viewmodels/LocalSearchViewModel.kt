// LocalSearchViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.constants.HideVideoSongsKey
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.db.entities.Album
import com.mochyy.mochymusic.db.entities.Artist
import com.mochyy.mochymusic.db.entities.LocalItem
import com.mochyy.mochymusic.db.entities.Playlist
import com.mochyy.mochymusic.db.entities.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Koin builds it (di/LibraryModule.kt). preferences is the one DataStore Android hands in, the same one
// context.dataStore gave before the move, so the hide-videos switch reads from the same place.
@OptIn(ExperimentalCoroutinesApi::class)
class LocalSearchViewModel(
    database: MochyDatabase,
    preferences: DataStore<Preferences>,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LocalFilter.ALL)

    val result =
        combine(
            query,
            filter,
            preferences.data.map { it[HideVideoSongsKey] ?: false }.distinctUntilChanged()
        ) { query, filter, hideVideoSongs ->
            Triple(query, filter, hideVideoSongs)
        }.flatMapLatest { (query, filter, hideVideoSongs) ->
            if (query.isEmpty()) {
                flowOf(LocalSearchResult("", filter, emptyMap()))
            } else {
                when (filter) {
                    LocalFilter.ALL ->
                        combine(
                            database.songDao.searchSongs(query, PREVIEW_SIZE),
                            database.songDao.searchAlbums(query, PREVIEW_SIZE),
                            database.songDao.searchArtists(query, PREVIEW_SIZE),
                            database.playlistDao.searchPlaylists(query, PREVIEW_SIZE),
                        ) { songs, albums, artists, playlists ->
                            val filteredSongs = if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                            filteredSongs + albums + artists + playlists
                        }

                    LocalFilter.SONG -> database.songDao.searchSongs(query).map { songs ->
                        if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                    }
                    LocalFilter.ALBUM -> database.songDao.searchAlbums(query)
                    LocalFilter.ARTIST -> database.songDao.searchArtists(query)
                    LocalFilter.PLAYLIST -> database.playlistDao.searchPlaylists(query)
                }.map { list ->
                    LocalSearchResult(
                        query = query,
                        filter = filter,
                        map =
                        list.groupBy {
                            when (it) {
                                is Song -> LocalFilter.SONG
                                is Album -> LocalFilter.ALBUM
                                is Artist -> LocalFilter.ARTIST
                                is Playlist -> LocalFilter.PLAYLIST
                            }
                        },
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LocalSearchResult("", filter.value, emptyMap())
        )

    companion object {
        const val PREVIEW_SIZE = 3
    }
}

enum class LocalFilter {
    ALL,
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST,
}

data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<LocalItem>>,
)
