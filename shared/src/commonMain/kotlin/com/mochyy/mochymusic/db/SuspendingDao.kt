package com.mochyy.mochymusic.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.mochyy.mochymusic.db.entities.AlbumArtistMap
import com.mochyy.mochymusic.db.entities.AlbumEntity
import com.mochyy.mochymusic.db.entities.ArtistEntity
import com.mochyy.mochymusic.db.entities.Event
import com.mochyy.mochymusic.db.entities.FormatEntity
import com.mochyy.mochymusic.db.entities.ImportSession
import com.mochyy.mochymusic.db.entities.ImportSessionSong
import com.mochyy.mochymusic.db.entities.ImportedLike
import com.mochyy.mochymusic.db.entities.ImportedPlaylist
import com.mochyy.mochymusic.db.entities.ImportedPlaylistTrack
import com.mochyy.mochymusic.db.entities.ImportedSong
import com.mochyy.mochymusic.db.entities.LyricsEntity
import com.mochyy.mochymusic.db.entities.PlayCountEntity
import com.mochyy.mochymusic.db.entities.Playlist
import com.mochyy.mochymusic.db.entities.PlaylistEntity
import com.mochyy.mochymusic.db.entities.PlaylistEvent
import com.mochyy.mochymusic.db.entities.PlaylistSongMap
import com.mochyy.mochymusic.db.entities.RecognitionHistory
import com.mochyy.mochymusic.db.entities.RelatedSongMap
import com.mochyy.mochymusic.db.entities.SearchHistory
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.db.entities.SongAlbumMap
import com.mochyy.mochymusic.db.entities.SongArtistMap
import com.mochyy.mochymusic.db.entities.SongEdit
import com.mochyy.mochymusic.db.entities.SongEntity
import kotlinx.datetime.LocalDateTime

// Suspend versions of the app DatabaseDao's blocking members, for code that already runs in a coroutine
// and for the shared database later (Room on desktop and iOS only runs suspend or Flow functions).
// Same SQL, conflict strategy and @Transaction as the blocking original of the same name. They live in
// their own DAO because a suspend function can't share a name and parameters with a blocking one in
// DatabaseDao. Once the blocking ones are gone, DatabaseDao can extend this.
// Already covered elsewhere in shared: getSongByIdBlocking = SongDao.getSongById,
// songEditBlocking = SongDao.songEdit(..).first(), getImportedPlaylist = PlaylistDao.importedPlaylist(..).first(),
// artistSongs = MochyDatabase.artistSongs.
// The six members that take an app model keep their mapping in app/: their SQL and transaction are the
// insertSongWithArtists / refreshSong / insertAlbumWithSongs / updateAlbumFromPage / updatePlaylistIfChanged
// helpers at the end, fed with entities the caller mapped.
@Dao
interface SuspendingDao {
    // ---- songs and artists ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: SongArtistMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: SongAlbumMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: AlbumArtistMap)

    @Update
    suspend fun update(song: SongEntity)

    @Update
    suspend fun update(artist: ArtistEntity)

    @Update
    suspend fun update(album: AlbumEntity)

    @Upsert
    suspend fun upsert(edit: SongEdit)

    @Upsert
    suspend fun upsert(lyrics: LyricsEntity)

    @Upsert
    suspend fun upsert(song: SongEntity)

    @Upsert
    suspend fun upsert(map: SongAlbumMap)

    @Upsert
    suspend fun upsert(format: FormatEntity)

    @Delete
    suspend fun delete(songArtistMap: SongArtistMap)

    @Delete
    suspend fun delete(artist: ArtistEntity)

    @Delete
    suspend fun delete(song: SongEntity)

    @Delete
    suspend fun delete(album: AlbumEntity)

    @Delete
    suspend fun delete(albumArtistMap: AlbumArtistMap)

    @Delete
    suspend fun delete(lyrics: LyricsEntity)

    @Transaction
    @Query("SELECT * FROM song_artist_map WHERE songId = :songId")
    suspend fun songArtistMap(songId: String): List<SongArtistMap>

    @Transaction
    @Query("SELECT * FROM album_artist_map WHERE albumId = :albumId")
    suspend fun albumArtistMaps(albumId: String): List<AlbumArtistMap>

    // just the rows, no artists or album, for checking thousands of likes at once
    @Query("SELECT * FROM song WHERE id IN (:songIds) AND NOT liked")
    suspend fun unlikedSongEntities(songIds: List<String>): List<SongEntity>

    // the same read as SongDao.getSongById, kept here so the transaction helpers below can look a song up
    @Transaction
    @Query("SELECT * FROM song WHERE id = :songId LIMIT 1")
    suspend fun songWithArtists(songId: String): Song?

    @Transaction
    @Query("SELECT * FROM artist WHERE name = :name")
    suspend fun artistByName(name: String): ArtistEntity?

    @Query("SELECT * FROM artist WHERE id = :id LIMIT 1")
    suspend fun getArtistById(id: String): ArtistEntity?

    @Query(
        "SELECT (SELECT COUNT(*) FROM song_artist_map WHERE artistId = :artistId) + " +
            "(SELECT COUNT(*) FROM album_artist_map WHERE artistId = :artistId)"
    )
    suspend fun artistUses(artistId: String): Int

    @Query("UPDATE song SET genre = :genre WHERE id = :songId")
    suspend fun setSongGenre(songId: String, genre: String)

    @Query("UPDATE song SET albumId = :albumId WHERE id = :songId")
    suspend fun setSongAlbum(songId: String, albumId: String)

    // counts a local album's songs and length again, and takes the first cover and year any of its files has
    @Query(
        "UPDATE album SET " +
            "songCount = (SELECT COUNT(*) FROM song_album_map WHERE albumId = :albumId), " +
            "duration = (SELECT COALESCE(SUM(song.duration), 0) FROM song " +
            "JOIN song_album_map ON song_album_map.songId = song.id WHERE song_album_map.albumId = :albumId), " +
            "thumbnailUrl = COALESCE(thumbnailUrl, :thumbnailUrl), year = COALESCE(year, :year) " +
            "WHERE id = :albumId"
    )
    suspend fun refreshLocalAlbum(albumId: String, thumbnailUrl: String?, year: Int?)

    @Query("UPDATE song SET lyricsOffset = :offsetMs WHERE id = :songId")
    suspend fun updateLyricsOffset(songId: String, offsetMs: Int)

    @Query("UPDATE song SET isDownloaded = :downloaded, dateDownload = :date WHERE id = :songId")
    suspend fun updateDownloadedInfo(songId: String, downloaded: Boolean, date: LocalDateTime?)

    @Query("UPDATE song SET totalPlayTime = totalPlayTime + :playTime WHERE id = :songId")
    suspend fun incrementTotalPlayTime(songId: String, playTime: Long)

    @Transaction
    @Query("UPDATE song SET inLibrary = :inLibrary, addedByLike = 0 WHERE id = :songId")
    suspend fun inLibrary(
        songId: String,
        inLibrary: LocalDateTime?,
    )

    @Transaction
    @Query("SELECT COUNT(1) FROM related_song_map WHERE songId = :songId LIMIT 1")
    suspend fun hasRelatedSongs(songId: String): Boolean

    @Transaction
    @Query(
        """
        SELECT song.*
        FROM (SELECT *
              FROM related_song_map
              GROUP BY relatedSongId) map
                 JOIN
             song
             ON song.id = map.relatedSongId
        WHERE songId = :songId
        """
    )
    suspend fun relatedSongs(songId: String): List<Song>

    @Transaction
    @Query("UPDATE song SET libraryAddToken = :libraryAddToken, libraryRemoveToken = :libraryRemoveToken WHERE id = :songId")
    suspend fun addLibraryTokens(
        songId: String,
        libraryAddToken: String?,
        libraryRemoveToken: String?,
    )

    // ---- play events, search history and recognition history ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: Event)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: PlaylistEvent)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: RelatedSongMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playCountEntity: PlayCountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(searchHistory: SearchHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recognitionHistory: RecognitionHistory): Long

    @Delete
    suspend fun delete(event: Event)

    @Delete
    suspend fun delete(searchHistory: SearchHistory)

    @Delete
    suspend fun delete(recognitionHistory: RecognitionHistory)

    @Query("DELETE FROM lyrics WHERE provider NOT IN (:keepProviders)")
    suspend fun clearFetchedLyrics(keepProviders: List<String>): Int

    @Query("DELETE FROM format WHERE id = :songId")
    suspend fun deleteFormat(songId: String)

    @Query("DELETE FROM lyrics WHERE id = :songId")
    suspend fun deleteLyrics(songId: String)

    @Query("SELECT localUri FROM song WHERE localUri IS NOT NULL")
    suspend fun localUris(): List<String>

    @Query(
        "SELECT (SELECT COUNT(*) FROM song WHERE thumbnailUrl = :url) + (SELECT COUNT(*) FROM album WHERE thumbnailUrl = :url) + " +
            "(SELECT COUNT(*) FROM playlist WHERE thumbnailUrl = :url) + (SELECT COUNT(*) FROM artist WHERE thumbnailUrl = :url)"
    )
    suspend fun thumbnailUsers(url: String): Int

    // lets go of every cover link that starts with prefix, for clearing the saved local covers in one go
    @Query("UPDATE song SET thumbnailUrl = NULL WHERE thumbnailUrl LIKE :prefix || '%'")
    suspend fun forgetSongCovers(prefix: String)

    @Query("UPDATE album SET thumbnailUrl = NULL WHERE thumbnailUrl LIKE :prefix || '%'")
    suspend fun forgetAlbumCovers(prefix: String)

    @Query("UPDATE playlist SET thumbnailUrl = NULL WHERE thumbnailUrl LIKE :prefix || '%'")
    suspend fun forgetPlaylistCovers(prefix: String)

    @Query("UPDATE artist SET thumbnailUrl = NULL WHERE thumbnailUrl LIKE :prefix || '%'")
    suspend fun forgetArtistCovers(prefix: String)

    // ---- library upkeep ----

    @Query(
        "INSERT OR IGNORE INTO album (id, title, thumbnailUrl, songCount, duration, lastUpdateTime) " +
            "SELECT albumId, albumName, thumbnailUrl, 0, 0, :now FROM song " +
            "WHERE (inLibrary IS NOT NULL OR liked) AND albumId IS NOT NULL AND albumName IS NOT NULL AND albumName != '' " +
            "GROUP BY albumId"
    )
    suspend fun addMissingAlbumRows(now: LocalDateTime)

    @Query(
        "INSERT OR IGNORE INTO album_artist_map (albumId, artistId, `order`) " +
            "SELECT song.albumId, song_artist_map.artistId, 0 FROM song " +
            "JOIN song_artist_map ON song_artist_map.songId = song.id AND song_artist_map.position = 0 " +
            "JOIN album ON album.id = song.albumId " +
            "WHERE (song.inLibrary IS NOT NULL OR song.liked) " +
            "AND NOT EXISTS(SELECT 1 FROM album_artist_map WHERE album_artist_map.albumId = song.albumId) " +
            "GROUP BY song.albumId"
    )
    suspend fun addMissingAlbumArtists()

    @Transaction
    suspend fun addMissingLibraryAlbums() {
        addMissingAlbumRows(nowLocal())
        addMissingAlbumArtists()
    }

    @Query("DELETE FROM song_artist_map WHERE artistId IN (SELECT id FROM artist WHERE name GLOB '[0-9]*:[0-9][0-9]')")
    suspend fun unlinkTimestampArtists(): Int

    @Query("DELETE FROM artist WHERE name GLOB '[0-9]*:[0-9][0-9]'")
    suspend fun deleteTimestampArtists(): Int

    @Transaction
    suspend fun purgeTimestampArtists(): Int {
        unlinkTimestampArtists()
        return deleteTimestampArtists()
    }

    // ---- playlists ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(map: PlaylistSongMap)

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Update
    suspend fun update(map: PlaylistSongMap)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlistSongMap: PlaylistSongMap)

    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    suspend fun playlistEntity(playlistId: String): PlaylistEntity?

    @Query("SELECT MAX(id) FROM playlist_song_map WHERE playlistId = :playlistId")
    suspend fun lastPlaylistEntryId(playlistId: String): Long?

    @Query("UPDATE playlist SET thumbnailUrl = :thumbnailUrl WHERE id = :playlistId AND thumbnailUrl IS :expected")
    suspend fun replacePlaylistCover(playlistId: String, expected: String?, thumbnailUrl: String): Int

    @Query("SELECT songId from playlist_song_map WHERE playlistId = :playlistId AND songId IN (:songIds)")
    suspend fun playlistDuplicates(
        playlistId: String,
        songIds: List<String>,
    ): List<String>

    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId AND songId IN (:songIds) AND position >= :fromPosition")
    suspend fun removeAddedSongs(playlistId: String, songIds: List<String>, fromPosition: Int)

    @Transaction
    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)

    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId AND position >= :from ORDER BY position")
    suspend fun playlistSongMaps(
        playlistId: String,
        from: Int,
    ): List<PlaylistSongMap>

    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE songId = :songId")
    suspend fun playlistSongMaps(songId: String): List<PlaylistSongMap>

    @Query("UPDATE playlist_song_map SET position = position + :by WHERE playlistId = :playlistId")
    suspend fun shiftPlaylistPositions(playlistId: String, by: Int)

    @Transaction
    @Query(
        """
        UPDATE playlist_song_map SET position =
            CASE
                WHEN position < :fromPosition THEN position + 1
                WHEN position > :fromPosition THEN position - 1
                ELSE :toPosition
            END
        WHERE playlistId = :playlistId AND position BETWEEN MIN(:fromPosition, :toPosition) AND MAX(:fromPosition, :toPosition)
    """,
    )
    suspend fun move(
        playlistId: String,
        fromPosition: Int,
        toPosition: Int,
    )

    @Transaction
    suspend fun addSongToPlaylist(playlist: Playlist, songIds: List<String>) {
        var position = playlist.songCount
        songIds.forEach { id ->
            insert(
                PlaylistSongMap(
                    songId = id,
                    playlistId = playlist.id,
                    position = position++
                )
            )
        }
    }

    // songs added to a playlist that mirrors youtube go on top, where people look for what they just added
    @Transaction
    suspend fun addSongsToPlaylistTop(playlist: Playlist, songIds: List<String>) {
        shiftPlaylistPositions(playlist.id, songIds.size)
        songIds.forEachIndexed { index, id ->
            insert(PlaylistSongMap(songId = id, playlistId = playlist.id, position = index))
        }
    }

    // ---- imported music and import sessions ----

    @Insert
    suspend fun insert(session: ImportSession): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: ImportSessionSong)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(imported: ImportedSong)

    @Query("SELECT DISTINCT songId FROM imported_song")
    suspend fun importedSongIds(): List<String>

    @Query("SELECT DISTINCT songId FROM imported_song WHERE source != :source")
    suspend fun importedSongIdsExcept(source: String): List<String>

    @Query("DELETE FROM imported_song WHERE songId = :songId AND source = :source")
    suspend fun deleteImportedSong(songId: String, source: String)

    @Query("UPDATE import_session_song SET liked = 1 WHERE sessionId = :sessionId AND songId = :songId")
    suspend fun markLikedByImport(sessionId: Long, songId: String)

    @Query(
        "SELECT import_session_song.songId AS songId, MAX(import_session.startedAt) AS importedAt FROM import_session_song " +
            "JOIN import_session ON import_session.id = import_session_song.sessionId " +
            "WHERE import_session_song.liked GROUP BY import_session_song.songId"
    )
    suspend fun likesFromImports(): List<ImportedLike>

    @Query("SELECT * FROM import_session WHERE source = :source ORDER BY id DESC")
    suspend fun importSessions(source: String): List<ImportSession>

    @Query("SELECT * FROM import_session WHERE source != :excluded ORDER BY id DESC")
    suspend fun importSessionsExcept(excluded: String): List<ImportSession>

    @Query("SELECT * FROM import_session_song WHERE sessionId = :sessionId")
    suspend fun importSessionSongs(sessionId: Long): List<ImportSessionSong>

    @Query("SELECT DISTINCT songId FROM import_session_song")
    suspend fun importSessionSongIds(): List<String>

    @Query("SELECT COUNT(*) FROM import_session_song WHERE songId = :songId AND sessionId != :sessionId")
    suspend fun otherSessionsWithSong(songId: String, sessionId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM import_session_song JOIN import_session ON import_session.id = import_session_song.sessionId " +
            "WHERE import_session_song.songId = :songId AND import_session.source = :source AND import_session.id != :sessionId"
    )
    suspend fun otherSessionsFromSource(songId: String, source: String, sessionId: Long): Int

    @Query("DELETE FROM import_session_song WHERE sessionId = :sessionId")
    suspend fun deleteImportSessionSongs(sessionId: Long)

    @Query("DELETE FROM import_session_song WHERE songId = :songId")
    suspend fun deleteImportSessionSong(songId: String)

    /** Forgets that [source]'s imports brought the song in; other services' imports keep it. */
    @Query(
        "DELETE FROM import_session_song WHERE songId = :songId " +
            "AND sessionId IN (SELECT id FROM import_session WHERE source = :source)"
    )
    suspend fun deleteImportSessionSong(songId: String, source: String)

    @Query("DELETE FROM import_session WHERE id = :sessionId")
    suspend fun deleteImportSession(sessionId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM import_session WHERE id = :sessionId)")
    suspend fun importSessionExists(sessionId: Long): Boolean

    @Query(
        "DELETE FROM import_session WHERE id = :sessionId " +
            "AND NOT EXISTS (SELECT 1 FROM import_session_song WHERE sessionId = :sessionId) " +
            "AND NOT EXISTS (SELECT 1 FROM imported_playlist WHERE sessionId = :sessionId)"
    )
    suspend fun deleteImportSessionIfEmpty(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playlist: ImportedPlaylist)

    @Query("SELECT * FROM imported_playlist WHERE source = :source AND remoteId = :remoteId ORDER BY importedAt DESC LIMIT 1")
    suspend fun importedPlaylistByRemoteId(source: String, remoteId: String): ImportedPlaylist?

    @Query(
        "UPDATE imported_playlist SET remoteId = COALESCE(:remoteId, remoteId), " +
            "remoteUrl = COALESCE(:remoteUrl, remoteUrl), lastSyncedAt = :syncedAt, " +
            "authorName = COALESCE(:authorName, authorName), authorAvatarUrl = COALESCE(:authorAvatarUrl, authorAvatarUrl), " +
            "authorUrl = COALESCE(:authorUrl, authorUrl) WHERE playlistId = :playlistId"
    )
    suspend fun updateImportedPlaylistOrigin(
        playlistId: String,
        remoteId: String?,
        remoteUrl: String?,
        syncedAt: LocalDateTime,
        authorName: String?,
        authorAvatarUrl: String?,
        authorUrl: String?,
    )

    @Query("SELECT trackKey FROM imported_playlist_track WHERE playlistId = :playlistId")
    suspend fun importedPlaylistTrackKeys(playlistId: String): List<String>

    @Query("SELECT COUNT(*) FROM imported_playlist_track WHERE playlistId = :playlistId")
    suspend fun importedPlaylistTrackCount(playlistId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertImportedPlaylistTracks(tracks: List<ImportedPlaylistTrack>)

    @Query("SELECT playlistId FROM imported_playlist WHERE sessionId = :sessionId")
    suspend fun importSessionPlaylistIds(sessionId: Long): List<String>

    @Query("DELETE FROM imported_playlist WHERE playlistId = :playlistId")
    suspend fun deleteImportedPlaylistRow(playlistId: String)

    @Query("DELETE FROM imported_playlist_track WHERE playlistId = :playlistId")
    suspend fun deleteImportedPlaylistTracks(playlistId: String)

    @Transaction
    suspend fun deleteImportedPlaylist(playlistId: String) {
        deleteImportedPlaylistRow(playlistId)
        deleteImportedPlaylistTracks(playlistId)
    }

    // ---- writes whose model is still in app/: the caller maps its model, the SQL and transaction stay here ----

    /**
     * The transaction behind app DatabaseDao's insert(MediaMetadata, block): [song] is
     * MediaMetadata.toSongEntity() after [block], [artists] its artists. Same SQL, conflict strategies and order.
     */
    @Transaction
    suspend fun insertSongWithArtists(song: SongEntity, artists: List<SongArtistInput>): Long {
        val rowId = insert(song)
        if (rowId == -1L) return -1L
        artists.filterNot { it.name.looksLikeTimestampName() }.forEachIndexed { index, artist ->
            val artistId = artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId()
            insert(
                ArtistEntity(
                    id = artistId,
                    name = artist.name,
                    channelId = artist.id,
                )
            )
            insert(
                SongArtistMap(
                    songId = song.id,
                    artistId = artistId,
                    position = index,
                )
            )
        }
        return rowId
    }

    /**
     * The transaction behind app DatabaseDao's update(Song, MediaMetadata): [fresh] is the fetched
     * metadata with the song edit already applied ([albumName] is the edited album when there is one),
     * [artists] its artists.
     */
    @Transaction
    suspend fun refreshSong(song: Song, fresh: SongEntity, artists: List<SongArtistInput>) {
        // whatever was edited by hand stays, a refetch only freshens the rest
        update(
            song.song.copy(
                title = fresh.title,
                duration = fresh.duration,
                thumbnailUrl = fresh.thumbnailUrl,
                albumId = fresh.albumId,
                albumName = fresh.albumName,
                libraryAddToken = fresh.libraryAddToken,
                libraryRemoveToken = fresh.libraryRemoveToken
            ),
        )
        songArtistMap(song.id).forEach { delete(it) }
        artists.forEachIndexed { index, artist ->
            val artistId = artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId()
            insert(
                ArtistEntity(
                    id = artistId,
                    name = artist.name,
                    channelId = artist.id,
                ),
            )
            insert(
                SongArtistMap(
                    songId = song.id,
                    artistId = artistId,
                    position = index,
                ),
            )
        }
    }

    /** The transaction behind app DatabaseDao's insert(AlbumPage); [songs] and [albumArtists] are mapped by the caller. */
    @Transaction
    suspend fun insertAlbumWithSongs(
        album: AlbumEntity,
        songs: List<SongInsert>,
        albumArtists: List<SongArtistInput>?,
    ) {
        if (insert(album) == -1L) return
        songs.forEach { entry ->
            insertSongWithArtists(entry.song, entry.artists)
            val existingSong = songWithArtists(entry.song.id)
            if (existingSong != null) {
                val refreshed = entry.refresh?.invoke(existingSong) ?: entry
                refreshSong(existingSong, refreshed.song, refreshed.artists)
            }
        }
        songs.forEachIndexed { index, entry ->
            upsert(
                SongAlbumMap(
                    songId = entry.song.id,
                    albumId = album.id,
                    index = index,
                )
            )
        }
        albumArtists?.let { artists ->
            val entities = artists.map { artist ->
                ArtistEntity(
                    id = artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId(),
                    name = artist.name,
                )
            }
            entities.forEach { insert(it) }
            entities.forEachIndexed { index, artist ->
                insert(
                    AlbumArtistMap(
                        albumId = album.id,
                        artistId = artist.id,
                        order = index,
                    )
                )
            }
        }
    }

    /**
     * The transaction behind app DatabaseDao's update(AlbumEntity, AlbumPage, artists): [updated] is the
     * album copy made from the page, [artistsToDelete] the old artists the caller decided to drop (null
     * to keep them), and [albumArtists] the page's artists (null when the page has none).
     */
    @Transaction
    suspend fun updateAlbumFromPage(
        album: AlbumEntity,
        updated: AlbumEntity,
        songs: List<SongInsert>,
        artistsToDelete: List<ArtistEntity>?,
        albumArtists: List<SongArtistInput>?,
    ) {
        update(updated)
        artistsToDelete?.forEach { delete(it) }
        songs.forEach { entry ->
            insertSongWithArtists(entry.song, entry.artists)
            val existingSong = songWithArtists(entry.song.id)
            if (existingSong != null) {
                val refreshed = entry.refresh?.invoke(existingSong) ?: entry
                refreshSong(existingSong, refreshed.song, refreshed.artists)
            }
        }
        songs.forEachIndexed { index, entry ->
            upsert(
                SongAlbumMap(
                    songId = entry.song.id,
                    albumId = updated.id,
                    index = index,
                )
            )
        }
        albumArtists?.let { artists ->
            albumArtistMaps(album.id).forEach { delete(it) }
            val entities = artists.map { artist ->
                ArtistEntity(
                    id = artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId(),
                    name = artist.name,
                )
            }
            entities.forEach { insert(it) }
            entities.forEachIndexed { index, artist ->
                insert(
                    AlbumArtistMap(
                        albumId = updated.id,
                        artistId = artist.id,
                        order = index,
                    )
                )
            }
        }
    }

    /**
     * The transaction behind app DatabaseDao's update(PlaylistEntity, PlaylistItem): [transform] applies
     * the item to the freshly read row (or to [fallback] when the row is gone), and nothing is written
     * when the result did not change.
     */
    @Transaction
    suspend fun updatePlaylistIfChanged(
        fallback: PlaylistEntity,
        transform: (PlaylistEntity) -> PlaylistEntity,
    ) {
        val current = playlistEntity(fallback.id) ?: fallback
        val updated = transform(current)
        if (updated != current) update(updated)
    }

    // ---- raw queries (the app's raw(SupportSQLiteQuery) and checkpoint) ----

    @RawQuery
    suspend fun raw(query: RoomRawQuery): Int

    suspend fun checkpoint() {
        raw(RoomRawQuery("PRAGMA wal_checkpoint(FULL)"))
    }
}

/** An artist to write with a song: its database id if known, otherwise it is looked up by name. */
data class SongArtistInput(
    val id: String?,
    val name: String,
)

/**
 * A song and its artists, already mapped off its model by the caller. When a song is already stored,
 * [refresh] maps that row to the values to write back, so a hand edit (SongEdit) still wins over the
 * refetch; null keeps [song] and [artists] as they are.
 */
data class SongInsert(
    val song: SongEntity,
    val artists: List<SongArtistInput>,
    val refresh: ((Song) -> SongInsert)? = null,
)

private val TIMESTAMP_NAME_REGEX = Regex("""^\d{1,2}:\d{2}(:\d{2})?$""")

private fun String.looksLikeTimestampName(): Boolean = TIMESTAMP_NAME_REGEX.matches(trim())
