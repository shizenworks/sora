// UserProfileViewModel.kt
//
// What the profile page shows: listening totals, what you've been playing, your playlists and the
// artists you play most. All of it comes from the local library and play history.

package com.mochyy.mochymusic.viewmodels

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.db.entities.Artist
import com.mochyy.mochymusic.db.entities.Playlist
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.db.nowLocal
import com.mochyy.mochymusic.ui.screens.recap.toStoredMillis
import com.mochyy.mochymusic.utils.ioDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** One "Total listens" card: how many different songs, and for how long, over [label]'s span. */
@Immutable
data class ListenTotal(val label: String, val songs: Int, val playTimeMs: Long) {
    companion object {
        /** Shown until the first count arrives, so the numbers roll up from zero instead of popping in. */
        val Placeholder = listOf(ListenTotal("ALL TIME", 0, 0L), ListenTotal("THIS MONTH", 0, 0L), ListenTotal("THIS WEEK", 0, 0L))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModel(
    private val database: MochyDatabase,
) : ViewModel() {

    val recentSongs: StateFlow<List<Song>?> = database.historyDao.recentlyPlayedSongs(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Bookmarked playlists, the most recently changed first. */
    val playlists: StateFlow<List<Playlist>?> = database.playlistDao.playlistsByUpdatedDateAsc()
        .map { it.asReversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val likedCount: StateFlow<Int> = database.songDao.likedSongsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val topArtists: StateFlow<List<Artist>?> = database.historyDao.mostPlayedArtists(fromTimeStamp = 0L, limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // the total play time changes with every logged play, which is exactly when these go stale
    val listens: StateFlow<List<ListenTotal>?> = database.historyDao.totalListeningTimeMs()
        .distinctUntilChanged()
        .mapLatest { countListens() }
        .flowOn(ioDispatcher)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Keeps every list above loaded for as long as the caller stays collecting, so the profile page
     * has them the moment it opens instead of filling in while it animates.
     */
    suspend fun keepWarm() {
        merge(recentSongs, playlists, likedCount, topArtists, listens).collect { }
    }

    private suspend fun countListens(): List<ListenTotal> {
        val today = nowLocal().date
        val monthStart = LocalDate(today.year, today.month.ordinal + 1, 1).toStoredMillis()
        val weekStart = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY).toStoredMillis()
        // one read of the whole history; the month and the week are slices of it
        val events = database.historyDao.recapEvents(0L, today.plus(1, DateTimeUnit.DAY).toStoredMillis())

        fun total(label: String, from: Long): ListenTotal {
            val songs = HashSet<String>()
            var ms = 0L
            for (e in events) {
                if (e.timestamp < from) continue
                songs += e.songId
                ms += e.playTime.coerceAtLeast(0L)
            }
            return ListenTotal(label, songs.size, ms)
        }

        return listOf(total("ALL TIME", 0L), total("THIS MONTH", monthStart), total("THIS WEEK", weekStart))
    }
}
