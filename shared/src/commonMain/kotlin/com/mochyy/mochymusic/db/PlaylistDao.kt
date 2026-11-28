package com.mochyy.mochymusic.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import com.mochyy.mochymusic.constants.PlaylistSortType
import com.mochyy.mochymusic.db.entities.Album
import com.mochyy.mochymusic.db.entities.ImportedAlbumRow
import com.mochyy.mochymusic.db.entities.ImportedPlaylist
import com.mochyy.mochymusic.db.entities.ImportedSong
import com.mochyy.mochymusic.db.entities.ImportedSourceCount
import com.mochyy.mochymusic.db.entities.Playlist
import com.mochyy.mochymusic.db.entities.PlaylistSong
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.extensions.reversed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// The playlist and imported-music queries every platform can run: each one returns a Flow or suspends.
// The blocking ones stay in the app's DatabaseDao.
@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId ORDER BY position")
    fun playlistSongs(playlistId: String): Flow<List<PlaylistSong>>

    @Query("SELECT DISTINCT playlistId FROM playlist_song_map WHERE songId IN (:songIds)")
    suspend fun playlistIdsContainingSongs(songIds: List<String>): List<String>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY rowId")
    fun playlistsByCreateDateAsc(): Flow<List<Playlist>>

    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY lastUpdateTime",
    )
    fun playlistsByUpdatedDateAsc(): Flow<List<Playlist>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY name")
    fun playlistsByNameAsc(): Flow<List<Playlist>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE isEditable AND bookmarkedAt IS NOT NULL ORDER BY name")
    fun editablePlaylistsByNameAsc(): Flow<List<Playlist>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun playlistsBySongCountAsc(): Flow<List<Playlist>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE id = :playlistId")
    fun playlist(playlistId: String): Flow<Playlist?>

    // songs added to a playlist after its entry [afterEntryId]; entry ids only ever grow, so these are the ones added since
    @Query("SELECT songId FROM playlist_song_map WHERE playlistId = :playlistId AND id > :afterEntryId")
    fun playlistSongIdsAfter(playlistId: String, afterEntryId: Long): Flow<List<String>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE isEditable AND bookmarkedAt IS NOT NULL ORDER BY rowId")
    fun editablePlaylistsByCreateDateAsc(): Flow<List<Playlist>>

    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE browseId = :browseId")
    fun playlistByBrowseId(browseId: String): Flow<Playlist?>

    /** Just the ids, for ticking off songs that are already in the playlist without loading them. */
    @Query("SELECT songId FROM playlist_song_map WHERE playlistId = :playlistId")
    fun playlistSongIds(playlistId: String): Flow<List<String>>

    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE name LIKE '%' || :query || '%' LIMIT :previewSize",
    )
    fun searchPlaylists(
        query: String,
        previewSize: Int = Int.MAX_VALUE,
    ): Flow<List<Playlist>>

    // Only songs still in the library or liked count as a service's: one taken back out of both
    // is gone from the user's music, and listing it under "Spotify" would bring it back.

    @Query(
        "SELECT imported_song.source, COUNT(*) AS songCount FROM imported_song " +
            "JOIN song ON song.id = imported_song.songId " +
            "WHERE song.inLibrary IS NOT NULL OR song.liked GROUP BY imported_song.source"
    )
    fun importedSourceCounts(): Flow<List<ImportedSourceCount>>

    /** The newest few covers of a source, for its card in the Library. */
    @Query(
        "SELECT song.thumbnailUrl FROM song JOIN imported_song ON song.id = imported_song.songId " +
            "WHERE imported_song.source = :source AND song.thumbnailUrl IS NOT NULL " +
            "AND (song.inLibrary IS NOT NULL OR song.liked) " +
            "ORDER BY imported_song.importedAt DESC LIMIT 4"
    )
    fun importedCovers(source: String): Flow<List<String>>

    @Transaction
    @Query(
        "SELECT song.* FROM song JOIN imported_song ON song.id = imported_song.songId " +
            "WHERE imported_song.source = :source AND (song.inLibrary IS NOT NULL OR song.liked) " +
            "ORDER BY song.title"
    )
    fun importedSongsByNameAsc(source: String): Flow<List<Song>>

    // one row per service and album, newest import first, for the imported tab's album cards
    @Query(
        "SELECT imported_song.source AS source, album.thumbnailUrl AS thumbnailUrl FROM imported_song " +
            "JOIN song ON song.id = imported_song.songId JOIN album ON album.id = song.albumId " +
            "WHERE song.inLibrary IS NOT NULL OR song.liked " +
            "GROUP BY imported_song.source, album.id ORDER BY MAX(imported_song.importedAt) DESC"
    )
    fun importedAlbumRows(): Flow<List<ImportedAlbumRow>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        "SELECT * FROM album WHERE EXISTS(SELECT 1 FROM song JOIN imported_song ON imported_song.songId = song.id " +
            "WHERE song.albumId = album.id AND imported_song.source = :source " +
            "AND (song.inLibrary IS NOT NULL OR song.liked)) ORDER BY title"
    )
    fun importedAlbums(source: String): Flow<List<Album>>

    /** Which service each imported playlist came from. */
    @Query("SELECT * FROM imported_playlist")
    fun importedPlaylists(): Flow<List<ImportedPlaylist>>

    /** Where one playlist came from, for its menu's Re-sync; null for a playlist made here. */
    @Query("SELECT * FROM imported_playlist WHERE playlistId = :playlistId")
    fun importedPlaylist(playlistId: String): Flow<ImportedPlaylist?>

    /** Where each liked song was imported from, for Liked songs' import order. A song from two services has two rows. */
    @Query("SELECT imported_song.* FROM imported_song JOIN song ON song.id = imported_song.songId WHERE song.liked")
    fun likedImportSources(): Flow<List<ImportedSong>>

    fun playlists(
        sortType: PlaylistSortType,
        descending: Boolean,
    ) = when (sortType) {
        PlaylistSortType.CREATE_DATE -> playlistsByCreateDateAsc()
        PlaylistSortType.NAME -> playlistsByNameAsc().map { playlists -> playlists.sortedByPlaylistName() }
        PlaylistSortType.SONG_COUNT -> playlistsBySongCountAsc()
        PlaylistSortType.LAST_UPDATED -> playlistsByUpdatedDateAsc()
    }.map { it.reversed(descending) }
}
