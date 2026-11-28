package com.mochyy.mochymusic.db

import com.mochyy.mochymusic.constants.ArtistSongSortType
import com.mochyy.mochymusic.db.daos.SpeedDialDao
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.extensions.reversed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// bump this with every schema change and add its migration to AllMigrations (Android) and SharedMigrations
// (desktop and iOS, nonAndroidMain); the import checks old exports against it too.
// Both InternalDatabase classes (app/ for Android, nonAndroidMain for desktop and iOS) read it.
const val MusicDatabaseVersion = 46

// The music database as shared code sees it, on every platform. Android's InternalDatabase (app/) and
// the desktop and iOS InternalDatabase (nonAndroidMain) both implement it, so shared code never names
// either class.
interface MochyDatabase {
    val songDao: SongDao
    val playlistDao: PlaylistDao
    val historyDao: HistoryDao
    val suspendingDao: SuspendingDao
    val speedDialDao: SpeedDialDao

    /**
     * The app DatabaseDao's artistSongs, for shared code: it needs queries from two shared DAOs
     * (SongDao and HistoryDao), so it lives here rather than in either. Same queries, ordering and limit
     * as the blocking original. Named apart from DatabaseDao.artistSongs: Android's MusicDatabase is both,
     * and Kotlin rejects one function inheriting two sets of default arguments.
     */
    fun observeArtistSongs(
        artistId: String,
        sortType: ArtistSongSortType,
        descending: Boolean,
        fromTimeStamp: Long? = null,
        toTimeStamp: Long? = null,
        limit: Int = -1,
    ): Flow<List<Song>> {
        val songsFlow = when (sortType) {
            ArtistSongSortType.CREATE_DATE -> songDao.artistSongsByCreateDateAsc(artistId)
            ArtistSongSortType.NAME ->
                songDao.artistSongsByNameAsc(artistId).map { artistSongs -> artistSongs.sortedBySongTitle() }

            ArtistSongSortType.PLAY_TIME -> {
                if (fromTimeStamp != null && toTimeStamp != null) {
                    historyDao.mostPlayedSongsByArtist(artistId, fromTimeStamp, toTimeStamp)
                } else {
                    songDao.artistSongsByPlayTimeAsc(artistId)
                }
            }
        }

        return songsFlow.map { songs ->
            val limitedSongs = if (limit > 0) songs.take(limit) else songs
            limitedSongs.reversed(descending)
        }
    }
}
