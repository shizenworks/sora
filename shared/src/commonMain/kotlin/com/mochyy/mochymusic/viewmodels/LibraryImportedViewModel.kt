// LibraryImportedViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.db.entities.Album
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.importer.ImportSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Backs "Imported from ...": the songs one service brought in, whatever playlist they went into.
 * Koin builds it (di/LibraryModule.kt); the rows come from the shared PlaylistDao.
 */
class LibraryImportedViewModel(
    database: MochyDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /** Null for a route with a source this build doesn't know - the screen then has nothing to show. */
    val source: ImportSource? = ImportSource.fromKey(savedStateHandle.get<String>("source"))

    val songs: StateFlow<List<Song>> = (source?.let { database.playlistDao.importedSongsByNameAsc(it.key) } ?: flowOf(emptyList<Song>()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // the albums this service's songs come from, for its albums card
    val albums: StateFlow<List<Album>> = (source?.let { database.playlistDao.importedAlbums(it.key) } ?: flowOf(emptyList<Album>()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
