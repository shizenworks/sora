package com.mochyy.mochymusic.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import androidx.sqlite.db.SupportSQLiteQuery
import com.mochyy.mochymusic.constants.ArtistSongSortType
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
import com.mochyy.mochymusic.db.entities.applyTo
import com.mochyy.mochymusic.extensions.reversed
import com.mochyy.mochymusic.extensions.toSQLiteQuery
import com.mochyy.mochymusic.models.MediaMetadata
import com.mochyy.mochymusic.models.toMediaMetadata
import com.mochyy.mochymusic.playlistcover.PlaylistCoverStore
import com.mochyy.mochymusic.ui.utils.resize
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistPage
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime

// The queries that can't run on every platform yet: blocking ones, the raw query, and the ones that need
// java or app-only code (MediaMetadata, the playlist cover store). The rest are in shared/; suspend
// versions of most blocking ones are in shared SuspendingDao.
@Dao
interface DatabaseDao : SongDao, PlaylistDao, HistoryDao {
    fun artistSongs(
        artistId: String,
        sortType: ArtistSongSortType,
        descending: Boolean,
        fromTimeStamp: Long? = null,
        toTimeStamp: Long? = null,
        limit: Int = -1
    ): Flow<List<Song>> {
        val songsFlow = when (sortType) {
            ArtistSongSortType.CREATE_DATE -> artistSongsByCreateDateAsc(artistId)
            ArtistSongSortType.NAME ->
                artistSongsByNameAsc(artistId).map { artistSongs -> artistSongs.sortedBySongTitle() }

            ArtistSongSortType.PLAY_TIME -> {
                if (fromTimeStamp != null && toTimeStamp != null) {
                    mostPlayedSongsByArtist(artistId, fromTimeStamp, toTimeStamp)
                } else {
                    artistSongsByPlayTimeAsc(artistId)
                }
            }
        }

        return songsFlow.map { songs ->
            val limitedSongs = if (limit > 0) songs.take(limit) else songs
            limitedSongs.reversed(descending)
        }
    }

    @Transaction
    @Query("SELECT * FROM song WHERE id = :songId LIMIT 1")
    fun getSongByIdBlocking(songId: String): Song?

    // just the rows, no artists or album, for checking thousands of likes at once
    @Query("SELECT * FROM song WHERE id IN (:songIds) AND NOT liked")
    fun unlikedSongEntities(songIds: List<String>): List<SongEntity>

    @Transaction
    @Query("SELECT * FROM song_artist_map WHERE songId = :songId")
    fun songArtistMap(songId: String): List<SongArtistMap>

    @Transaction
    @Query("SELECT * FROM album_artist_map WHERE albumId = :albumId")
    fun albumArtistMaps(albumId: String): List<AlbumArtistMap>

    // an album is only stored once something opens or saves it, so a song's album that was never opened gets a stand-in the album page fills in later
    @Query(
        "INSERT OR IGNORE INTO album (id, title, thumbnailUrl, songCount, duration, lastUpdateTime) " +
            "SELECT albumId, albumName, thumbnailUrl, 0, 0, :now FROM song " +
            "WHERE (inLibrary IS NOT NULL OR liked) AND albumId IS NOT NULL AND albumName IS NOT NULL AND albumName != '' " +
            "GROUP BY albumId"
    )
    fun addMissingAlbumRows(now: LocalDateTime)

    // a stand-in album borrows its song's main artist until its page brings the real ones
    @Query(
        "INSERT OR IGNORE INTO album_artist_map (albumId, artistId, `order`) " +
            "SELECT song.albumId, song_artist_map.artistId, 0 FROM song " +
            "JOIN song_artist_map ON song_artist_map.songId = song.id AND song_artist_map.position = 0 " +
            "JOIN album ON album.id = song.albumId " +
            "WHERE (song.inLibrary IS NOT NULL OR song.liked) " +
            "AND NOT EXISTS(SELECT 1 FROM album_artist_map WHERE album_artist_map.albumId = song.albumId) " +
            "GROUP BY song.albumId"
    )
    fun addMissingAlbumArtists()

    @Transaction
    fun addMissingLibraryAlbums() {
        addMissingAlbumRows(nowLocal())
        addMissingAlbumArtists()
    }

    @Query("UPDATE song SET albumId = :albumId WHERE id = :songId")
    fun setSongAlbum(songId: String, albumId: String)

    // counts a local album's songs and length again, and takes the first cover and year any of its files has
    @Query(
        "UPDATE album SET " +
            "songCount = (SELECT COUNT(*) FROM song_album_map WHERE albumId = :albumId), " +
            "duration = (SELECT COALESCE(SUM(song.duration), 0) FROM song " +
            "JOIN song_album_map ON song_album_map.songId = song.id WHERE song_album_map.albumId = :albumId), " +
            "thumbnailUrl = COALESCE(thumbnailUrl, :thumbnailUrl), year = COALESCE(year, :year) " +
            "WHERE id = :albumId"
    )
    fun refreshLocalAlbum(albumId: String, thumbnailUrl: String?, year: Int?)

    @Query("UPDATE song SET genre = :genre WHERE id = :songId")
    fun setSongGenre(songId: String, genre: String)

    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    fun playlistEntity(playlistId: String): PlaylistEntity?

    // the newest entry in a playlist, where "download new songs" starts counting from
    @Query("SELECT MAX(id) FROM playlist_song_map WHERE playlistId = :playlistId")
    fun lastPlaylistEntryId(playlistId: String): Long?

    // swaps only the cover, and only while it's still [expected], so a redraw that finishes late never undoes a newer edit
    @Query("UPDATE playlist SET thumbnailUrl = :thumbnailUrl WHERE id = :playlistId AND thumbnailUrl IS :expected")
    fun replacePlaylistCover(playlistId: String, expected: String?, thumbnailUrl: String): Int

    @Transaction
    @Query("SELECT COUNT(*) from playlist_song_map WHERE playlistId = :playlistId AND songId = :songId LIMIT 1")
    fun checkInPlaylist(
        playlistId: String,
        songId: String,
    ): Int

    @Query("SELECT songId from playlist_song_map WHERE playlistId = :playlistId AND songId IN (:songIds)")
    fun playlistDuplicates(
        playlistId: String,
        songIds: List<String>,
    ): List<String>

    /** Takes back songs just appended to a playlist (they sit at [fromPosition] or later). */
    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId AND songId IN (:songIds) AND position >= :fromPosition")
    fun removeAddedSongs(playlistId: String, songIds: List<String>, fromPosition: Int)

    @Query("UPDATE playlist_song_map SET position = position + :by WHERE playlistId = :playlistId")
    fun shiftPlaylistPositions(playlistId: String, by: Int)

    // songs added to a playlist that mirrors youtube go on top, where people look for what they just added
    @Transaction
    fun addSongsToPlaylistTop(playlist: Playlist, songIds: List<String>) {
        shiftPlaylistPositions(playlist.id, songIds.size)
        songIds.forEachIndexed { index, id ->
            insert(PlaylistSongMap(songId = id, playlistId = playlist.id, position = index))
        }
    }

    @Transaction
    fun addSongToPlaylist(playlist: Playlist, songIds: List<String>) {
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

    @Query("UPDATE song SET isDownloaded = :downloaded, dateDownload = :date WHERE id = :songId")
    fun updateDownloadedInfo(songId: String, downloaded: Boolean, date: LocalDateTime?)

    @Transaction
    @Query("DELETE FROM event")
    fun clearListenHistory()

    @Transaction
    @Query("DELETE FROM search_history")
    fun clearSearchHistory()

    @Transaction
    @Query("DELETE FROM recognition_history")
    fun clearRecognitionHistory()

    @Transaction
    @Query("DELETE FROM recognition_history WHERE id = :id")
    fun deleteRecognitionHistoryById(id: Long)

    @Transaction
    @Query("UPDATE recognition_history SET liked = :liked WHERE id = :id")
    fun updateRecognitionHistoryLiked(id: Long, liked: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recognitionHistory: RecognitionHistory): Long

    @Delete
    fun delete(recognitionHistory: RecognitionHistory)

    @Query("UPDATE song SET totalPlayTime = totalPlayTime + :playTime WHERE id = :songId")
    fun incrementTotalPlayTime(songId: String, playTime: Long)

    @Query("UPDATE playCount SET count = count + 1 WHERE song = :songId AND year = :year AND month = :month")
    fun incrementPlayCount(songId: String, year: Int, month: Int)

    fun incrementPlayCount(songId: String) {
        val time = nowLocal().toJavaLocalDateTime().atOffset(ZoneOffset.UTC)
        var oldCount: Int
        runBlocking {
            oldCount = getPlayCountByMonth(songId, time.year, time.monthValue).first()
        }

        if (oldCount <= 0) {
            insert(PlayCountEntity(songId, time.year, time.monthValue, 0))
        }
        incrementPlayCount(songId, time.year, time.monthValue)
    }

    @Transaction
    @Query("UPDATE song SET inLibrary = :inLibrary, addedByLike = 0 WHERE id = :songId")
    fun inLibrary(
        songId: String,
        inLibrary: LocalDateTime?,
    )

    @Transaction
    @Query("UPDATE song SET libraryAddToken = :libraryAddToken, libraryRemoveToken = :libraryRemoveToken WHERE id = :songId")
    fun addLibraryTokens(
        songId: String,
        libraryAddToken: String?,
        libraryRemoveToken: String?,
    )

    @Transaction
    @Query("SELECT COUNT(1) FROM related_song_map WHERE songId = :songId LIMIT 1")
    fun hasRelatedSongs(songId: String): Boolean

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
    fun relatedSongs(songId: String): List<Song>

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
    fun move(
        playlistId: String,
        fromPosition: Int,
        toPosition: Int,
    )

    @Transaction
    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId")
    fun clearPlaylist(playlistId: String)

    @Transaction
    @Query("SELECT * FROM artist WHERE name = :name")
    fun artistByName(name: String): ArtistEntity?

    @Query("SELECT * FROM artist WHERE id = :id LIMIT 1")
    fun getArtistById(id: String): ArtistEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(album: AlbumEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: SongArtistMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: SongAlbumMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: AlbumArtistMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: PlaylistSongMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(searchHistory: SearchHistory)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(event: Event)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(event: PlaylistEvent)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: RelatedSongMap)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playCountEntity: PlayCountEntity): Long

    @Query("DELETE FROM song_artist_map WHERE artistId IN (SELECT id FROM artist WHERE name GLOB '[0-9]*:[0-9][0-9]')")
    fun unlinkTimestampArtists(): Int

    @Query("DELETE FROM artist WHERE name GLOB '[0-9]*:[0-9][0-9]'")
    fun deleteTimestampArtists(): Int

    @Transaction
    fun purgeTimestampArtists(): Int {
        unlinkTimestampArtists()
        return deleteTimestampArtists()
    }

    /** every fetched lyric (and every saved "not found") goes, so they're looked up again; ones from [keepProviders] stay */
    @Query("DELETE FROM lyrics WHERE provider NOT IN (:keepProviders)")
    fun clearFetchedLyrics(keepProviders: List<String>): Int

    @Transaction
    fun insert(
        mediaMetadata: MediaMetadata,
        block: (SongEntity) -> SongEntity = { it },
    ) {
        if (insert(mediaMetadata.toSongEntity().let(block)) == -1L) return

        mediaMetadata.artists.filterNot { it.name.looksLikeTimestampName() }.forEachIndexed { index, artist ->
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
                    songId = mediaMetadata.id,
                    artistId = artistId,
                    position = index,
                )
            )
        }
    }

    @Transaction
    fun insert(albumPage: AlbumPage) {
        if (insert(
                AlbumEntity(
                    id = albumPage.album.browseId,
                    playlistId = albumPage.album.playlistId,
                    title = albumPage.album.title,
                    year = albumPage.album.year,
                    thumbnailUrl = albumPage.album.thumbnail,
                    songCount = albumPage.songs.size,
                    duration = albumPage.songs.sumOf { it.duration ?: 0 },
                    explicit = albumPage.album.explicit || albumPage.songs.any { it.explicit },
                    description = albumPage.description,
                ),
            ) == -1L
        ) {
            return
        }
        albumPage.songs
            .map(SongItem::toMediaMetadata)
            .onEach(::insert)
            .onEach {
                val existingSong = getSongByIdBlocking(it.id)
                if (existingSong != null) {
                    update(existingSong, it)
                }
            }.mapIndexed { index, song ->
                SongAlbumMap(
                    songId = song.id,
                    albumId = albumPage.album.browseId,
                    index = index,
                )
            }.forEach(::upsert)
        albumPage.album.artists
            ?.map { artist ->
                ArtistEntity(
                    id = artist.id ?: artistByName(artist.name)?.id
                    ?: ArtistEntity.generateArtistId(),
                    name = artist.name,
                )
            }?.onEach(::insert)
            ?.mapIndexed { index, artist ->
                AlbumArtistMap(
                    albumId = albumPage.album.browseId,
                    artistId = artist.id,
                    order = index,
                )
            }?.forEach(::insert)
    }

    @Transaction
    fun update(
        song: Song,
        fetched: MediaMetadata,
    ) {
        // whatever was edited by hand stays, a refetch only freshens the rest
        val edit = songEditBlocking(song.id)
        val mediaMetadata = edit?.applyTo(fetched) ?: fetched
        update(
            song.song.copy(
                title = mediaMetadata.title,
                duration = mediaMetadata.duration,
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumId = mediaMetadata.album?.id,
                albumName = edit?.album ?: mediaMetadata.album?.title,
                libraryAddToken = mediaMetadata.libraryAddToken,
                libraryRemoveToken = mediaMetadata.libraryRemoveToken
            ),
        )
        songArtistMap(song.id).forEach(::delete)
        mediaMetadata.artists.forEachIndexed { index, artist ->
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

    @Update
    fun update(song: SongEntity)

    @Query("UPDATE song SET lyricsOffset = :offsetMs WHERE id = :songId")
    fun updateLyricsOffset(songId: String, offsetMs: Int)

    @Query("SELECT * FROM song_edit WHERE songId = :songId")
    fun songEditBlocking(songId: String): SongEdit?

    @Upsert
    fun upsert(edit: SongEdit)

    // ---- imported music ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(imported: ImportedSong)

    /** Every song that was imported from anywhere, for the library sync to leave alone. */
    @Query("SELECT DISTINCT songId FROM imported_song")
    fun importedSongIds(): List<String>

    /**
     * The songs imported from anywhere but [source]. YouTube sync passes its own source: the songs
     * it brought in follow the YouTube library, so they may leave it again.
     */
    @Query("SELECT DISTINCT songId FROM imported_song WHERE source != :source")
    fun importedSongIdsExcept(source: String): List<String>

    @Query("DELETE FROM imported_song WHERE songId = :songId AND source = :source")
    fun deleteImportedSong(songId: String, source: String)

    // ---- import sessions ----

    @Insert
    fun insert(session: ImportSession): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(row: ImportSessionSong)

    @Query("UPDATE import_session_song SET liked = 1 WHERE sessionId = :sessionId AND songId = :songId")
    fun markLikedByImport(sessionId: Long, songId: String)

    @Query(
        "SELECT import_session_song.songId AS songId, MAX(import_session.startedAt) AS importedAt FROM import_session_song " +
            "JOIN import_session ON import_session.id = import_session_song.sessionId " +
            "WHERE import_session_song.liked GROUP BY import_session_song.songId"
    )
    fun likesFromImports(): List<ImportedLike>

    @Query("SELECT * FROM import_session WHERE source = :source ORDER BY id DESC")
    fun importSessions(source: String): List<ImportSession>

    @Query("SELECT * FROM import_session WHERE source != :excluded ORDER BY id DESC")
    fun importSessionsExcept(excluded: String): List<ImportSession>

    @Query("SELECT * FROM import_session_song WHERE sessionId = :sessionId")
    fun importSessionSongs(sessionId: Long): List<ImportSessionSong>

    /** Every song an import session brought in, for the library sync to leave alone. */
    @Query("SELECT DISTINCT songId FROM import_session_song")
    fun importSessionSongIds(): List<String>

    /** Any other session that still has a claim on the song: it stays in the library for that one. */
    @Query("SELECT COUNT(*) FROM import_session_song WHERE songId = :songId AND sessionId != :sessionId")
    fun otherSessionsWithSong(songId: String, sessionId: Long): Int

    /** Another session from the same service still has the song, so it keeps that service's tag. */
    @Query(
        "SELECT COUNT(*) FROM import_session_song JOIN import_session ON import_session.id = import_session_song.sessionId " +
            "WHERE import_session_song.songId = :songId AND import_session.source = :source AND import_session.id != :sessionId"
    )
    fun otherSessionsFromSource(songId: String, source: String, sessionId: Long): Int

    @Query("DELETE FROM import_session_song WHERE sessionId = :sessionId")
    fun deleteImportSessionSongs(sessionId: Long)

    @Query("DELETE FROM import_session_song WHERE songId = :songId")
    fun deleteImportSessionSong(songId: String)

    /** Forgets that [source]'s imports brought the song in; other services' imports keep it. */
    @Query(
        "DELETE FROM import_session_song WHERE songId = :songId " +
            "AND sessionId IN (SELECT id FROM import_session WHERE source = :source)"
    )
    fun deleteImportSessionSong(songId: String, source: String)

    @Query("DELETE FROM import_session WHERE id = :sessionId")
    fun deleteImportSession(sessionId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM import_session WHERE id = :sessionId)")
    fun importSessionExists(sessionId: Long): Boolean

    /** A session that ended up bringing nothing in has nothing to undo, so it isn't kept. */
    @Query(
        "DELETE FROM import_session WHERE id = :sessionId " +
            "AND NOT EXISTS (SELECT 1 FROM import_session_song WHERE sessionId = :sessionId) " +
            "AND NOT EXISTS (SELECT 1 FROM imported_playlist WHERE sessionId = :sessionId)"
    )
    fun deleteImportSessionIfEmpty(sessionId: Long)

    // ---- imported playlists ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playlist: ImportedPlaylist)

    @Query("SELECT * FROM imported_playlist WHERE playlistId = :playlistId")
    fun getImportedPlaylist(playlistId: String): ImportedPlaylist?

    /** The local copy of a service's playlist, found by its id there; the newest, if one was deleted and imported again. */
    @Query("SELECT * FROM imported_playlist WHERE source = :source AND remoteId = :remoteId ORDER BY importedAt DESC LIMIT 1")
    fun importedPlaylistByRemoteId(source: String, remoteId: String): ImportedPlaylist?

    /** remembers where the playlist lives and who made it, anything unknown keeps what's already saved */
    @Query(
        "UPDATE imported_playlist SET remoteId = COALESCE(:remoteId, remoteId), " +
            "remoteUrl = COALESCE(:remoteUrl, remoteUrl), lastSyncedAt = :syncedAt, " +
            "authorName = COALESCE(:authorName, authorName), authorAvatarUrl = COALESCE(:authorAvatarUrl, authorAvatarUrl), " +
            "authorUrl = COALESCE(:authorUrl, authorUrl) WHERE playlistId = :playlistId"
    )
    fun updateImportedPlaylistOrigin(
        playlistId: String,
        remoteId: String?,
        remoteUrl: String?,
        syncedAt: LocalDateTime,
        authorName: String?,
        authorAvatarUrl: String?,
        authorUrl: String?,
    )

    @Query("SELECT trackKey FROM imported_playlist_track WHERE playlistId = :playlistId")
    fun importedPlaylistTrackKeys(playlistId: String): List<String>

    /** About how many songs the playlist had on its service the last time it was read. */
    @Query("SELECT COUNT(*) FROM imported_playlist_track WHERE playlistId = :playlistId")
    fun importedPlaylistTrackCount(playlistId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertImportedPlaylistTracks(tracks: List<ImportedPlaylistTrack>)

    @Query("SELECT playlistId FROM imported_playlist WHERE sessionId = :sessionId")
    fun importSessionPlaylistIds(sessionId: Long): List<String>

    @Query("DELETE FROM imported_playlist WHERE playlistId = :playlistId")
    fun deleteImportedPlaylistRow(playlistId: String)

    @Query("DELETE FROM imported_playlist_track WHERE playlistId = :playlistId")
    fun deleteImportedPlaylistTracks(playlistId: String)

    @Transaction
    fun deleteImportedPlaylist(playlistId: String) {
        deleteImportedPlaylistRow(playlistId)
        deleteImportedPlaylistTracks(playlistId)
    }

    // ---- files imported from the phone ----

    /** Every file already in the library, so a storage scan only offers the new ones. */
    @Query("SELECT localUri FROM song WHERE localUri IS NOT NULL")
    fun localUris(): List<String>

    // how many songs, albums, playlists and artists show this picture: local songs share cover files, so one only goes with its last user
    @Query(
        "SELECT (SELECT COUNT(*) FROM song WHERE thumbnailUrl = :url) + (SELECT COUNT(*) FROM album WHERE thumbnailUrl = :url) + " +
            "(SELECT COUNT(*) FROM playlist WHERE thumbnailUrl = :url) + (SELECT COUNT(*) FROM artist WHERE thumbnailUrl = :url)"
    )
    fun thumbnailUsers(url: String): Int

    // lets go of every cover link that starts with prefix, for clearing the saved local covers in one go
    @Query("UPDATE song SET thumbnailUrl = NULL WHERE thumbnailUrl LIKE :prefix || '%'")
    fun forgetSongCovers(prefix: String)

    @Query("UPDATE album SET thumbnailUrl = NULL WHERE thumbnailUrl LIKE :prefix || '%'")
    fun forgetAlbumCovers(prefix: String)

    @Query("UPDATE playlist SET thumbnailUrl = NULL WHERE thumbnailUrl LIKE :prefix || '%'")
    fun forgetPlaylistCovers(prefix: String)

    @Query("UPDATE artist SET thumbnailUrl = NULL WHERE thumbnailUrl LIKE :prefix || '%'")
    fun forgetArtistCovers(prefix: String)

    // the songs and albums that still name an artist, so a name that got split into its artists only goes once nothing uses it
    @Query(
        "SELECT (SELECT COUNT(*) FROM song_artist_map WHERE artistId = :artistId) + " +
            "(SELECT COUNT(*) FROM album_artist_map WHERE artistId = :artistId)"
    )
    fun artistUses(artistId: String): Int

    @Query("DELETE FROM format WHERE id = :songId")
    fun deleteFormat(songId: String)

    @Query("DELETE FROM lyrics")
    suspend fun clearLyrics()

    @Query("DELETE FROM lyrics WHERE id = :songId")
    fun deleteLyrics(songId: String)

    @Update
    fun update(artist: ArtistEntity)

    @Update
    fun update(album: AlbumEntity)

    @Update
    fun update(playlist: PlaylistEntity)

    @Update
    fun update(map: PlaylistSongMap)

    @Transaction
    fun update(
        artist: ArtistEntity,
        artistPage: ArtistPage,
        preferredThumbnailUrl: String? = null,
    ) {
        update(
            artist.copy(
                name = artistPage.artist.title,
                thumbnailUrl = preferredThumbnailUrl
                    ?: artistPage.artist.thumbnail?.resize(544, 544)
                    ?: artist.thumbnailUrl,
                lastUpdateTime = nowLocal()
            )
        )
    }

    @Transaction
    fun update(
        album: AlbumEntity,
        albumPage: AlbumPage,
        artists: List<ArtistEntity>? = emptyList(),
    ) {
        update(
            album.copy(
                id = albumPage.album.browseId,
                playlistId = albumPage.album.playlistId,
                title = albumPage.album.title,
                year = albumPage.album.year,
                thumbnailUrl = albumPage.album.thumbnail,
                songCount = albumPage.songs.size,
                duration = albumPage.songs.sumOf { it.duration ?: 0 },
                explicit = albumPage.album.explicit || albumPage.songs.any { it.explicit },
                description = albumPage.description ?: album.description,
            ),
        )
        if (artists?.size != albumPage.album.artists?.size) {
            artists?.forEach(::delete)
        }
        albumPage.songs
            .map(SongItem::toMediaMetadata)
            .onEach(::insert)
            .onEach {
                val existingSong = getSongByIdBlocking(it.id)
                if (existingSong != null) {
                    update(existingSong, it)
                }
            }.mapIndexed { index, song ->
                SongAlbumMap(
                    songId = song.id,
                    albumId = albumPage.album.browseId,
                    index = index,
                )
            }.forEach(::upsert)

        albumPage.album.artists?.let { artists ->

            albumArtistMaps(album.id).forEach(::delete)
            artists
                .map { artist ->
                    ArtistEntity(
                        id = artist.id ?: artistByName(artist.name)?.id
                        ?: ArtistEntity.generateArtistId(),
                        name = artist.name,
                    )
                }.onEach(::insert)
                .mapIndexed { index, artist ->
                    AlbumArtistMap(
                        albumId = albumPage.album.browseId,
                        artistId = artist.id,
                        order = index,
                    )
                }.forEach(::insert)
        }
    }

    @Transaction
    fun update(playlistEntity: PlaylistEntity, playlistItem: PlaylistItem) {
        // read fresh: the sync walks every playlist slowly, and a copy from its start wrote over edits made meanwhile
        val current = playlistEntity(playlistEntity.id) ?: playlistEntity
        val updated = current.copy(
            name = playlistItem.title,
            browseId = playlistItem.id,
            // a cover made or picked in mochy stays, youtube's own picture went over it on every sync
            thumbnailUrl = current.thumbnailUrl.takeIf(PlaylistCoverStore::isManaged) ?: playlistItem.thumbnail,
            isEditable = playlistItem.isEditable,
            remoteSongCount = playlistItem.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
            playEndpointParams = playlistItem.playEndpoint?.params,
            shuffleEndpointParams = playlistItem.shuffleEndpoint?.params,
            radioEndpointParams = playlistItem.radioEndpoint?.params
        )
        // skip writing when nothing changed so room doesn't invalidate every playlist observer during sync
        if (updated != current) update(updated)
    }

    @Upsert
    fun upsert(map: SongAlbumMap)

    @Upsert
    fun upsert(lyrics: LyricsEntity)

    @Upsert
    fun upsert(format: FormatEntity)

    @Upsert
    fun upsert(song: SongEntity)

    @Delete
    fun delete(song: SongEntity)

    @Delete
    fun delete(songArtistMap: SongArtistMap)

    @Delete
    fun delete(artist: ArtistEntity)

    @Delete
    fun delete(album: AlbumEntity)

    @Delete
    fun delete(albumArtistMap: AlbumArtistMap)

    @Delete
    fun delete(playlist: PlaylistEntity)

    @Delete
    fun delete(playlistSongMap: PlaylistSongMap)

    @Query("DELETE FROM playlist WHERE browseId = :browseId")
    fun deletePlaylistById(browseId: String)

    @Delete
    fun delete(lyrics: LyricsEntity)

    @Delete
    fun delete(searchHistory: SearchHistory)

    @Delete
    fun delete(event: Event)

    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE songId = :songId")
    fun playlistSongMaps(songId: String): List<PlaylistSongMap>

    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId AND position >= :from ORDER BY position")
    fun playlistSongMaps(
        playlistId: String,
        from: Int,
    ): List<PlaylistSongMap>

    // bare snapshots for sync undo so auto-sync never runs multi-table joins just to record ids and dates
    @Query("SELECT id, likedDate FROM song WHERE liked")
    suspend fun likedSongSnapshots(): List<LikedSongSnapshot>

    @Query("SELECT id, inLibrary FROM song WHERE inLibrary IS NOT NULL")
    suspend fun librarySongSnapshots(): List<LibrarySongSnapshot>

    @Query("SELECT id FROM playlist WHERE browseId IS NOT NULL")
    suspend fun syncedPlaylistIds(): List<String>

    @Query("SELECT playlistId, songId, setVideoId FROM playlist_song_map WHERE playlistId IN (SELECT id FROM playlist WHERE browseId IS NOT NULL) ORDER BY playlistId, position")
    suspend fun syncedPlaylistEntries(): List<SyncedPlaylistEntrySnapshot>

    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun checkpoint() {
        raw("PRAGMA wal_checkpoint(FULL)".toSQLiteQuery())
    }
}

data class LikedSongSnapshot(
    val id: String,
    val likedDate: LocalDateTime?,
)

data class LibrarySongSnapshot(
    val id: String,
    val inLibrary: LocalDateTime,
)

data class SyncedPlaylistEntrySnapshot(
    val playlistId: String,
    val songId: String,
    val setVideoId: String?,
)

private val TIMESTAMP_NAME_REGEX = Regex("""^\d{1,2}:\d{2}(:\d{2})?$""")

private fun String.looksLikeTimestampName(): Boolean = TIMESTAMP_NAME_REGEX.matches(trim())
