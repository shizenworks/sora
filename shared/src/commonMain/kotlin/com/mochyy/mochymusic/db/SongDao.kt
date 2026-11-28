package com.mochyy.mochymusic.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import com.mochyy.mochymusic.constants.AlbumSortType
import com.mochyy.mochymusic.constants.ArtistSortType
import com.mochyy.mochymusic.constants.SongSortType
import com.mochyy.mochymusic.db.entities.Album
import com.mochyy.mochymusic.db.entities.AlbumSongRow
import com.mochyy.mochymusic.db.entities.AlbumWithSongs
import com.mochyy.mochymusic.db.entities.Artist
import com.mochyy.mochymusic.db.entities.FormatEntity
import com.mochyy.mochymusic.db.entities.LyricsEntity
import com.mochyy.mochymusic.db.entities.SetVideoIdEntity
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.db.entities.SongEdit
import com.mochyy.mochymusic.db.entities.SongEntity
import com.mochyy.mochymusic.extensions.reversed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The songs, albums and artists queries every platform can run: each one returns a Flow or suspends.
// The blocking ones and the ones that need java or app-only code stay in the app's DatabaseDao.
@Dao
interface SongDao {
    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY rowId")
    fun songsByRowIdAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY inLibrary")
    fun songsByCreateDateAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY title")
    fun songsByNameAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY totalPlayTime")
    fun songsByPlayTimeAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isLocal = 1 ORDER BY rowId")
    fun localSongsByRowIdAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isLocal = 1 ORDER BY inLibrary")
    fun localSongsByCreateDateAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isLocal = 1 ORDER BY title")
    fun localSongsByNameAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isLocal = 1 ORDER BY totalPlayTime")
    fun localSongsByPlayTimeAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE liked ORDER BY rowId")
    fun likedSongsByRowIdAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE liked ORDER BY likedDate")
    fun likedSongsByCreateDateAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE liked ORDER BY title")
    fun likedSongsByNameAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE liked ORDER BY totalPlayTime")
    fun likedSongsByPlayTimeAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT COUNT(1) FROM song WHERE liked")
    fun likedSongsCount(): Flow<Int>

    @Transaction
    @Query("SELECT song.* FROM song JOIN song_album_map ON song.id = song_album_map.songId WHERE song_album_map.albumId = :albumId ORDER BY song_album_map.`index`, song_album_map.rowid")
    fun albumSongs(albumId: String): Flow<List<Song>>

    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY inLibrary",
    )
    fun artistSongsByCreateDateAsc(artistId: String): Flow<List<Song>>

    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY title",
    )
    fun artistSongsByNameAsc(artistId: String): Flow<List<Song>>

    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY totalPlayTime",
    )
    fun artistSongsByPlayTimeAsc(artistId: String): Flow<List<Song>>

    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL LIMIT :previewSize",
    )
    fun artistSongsPreview(
        artistId: String,
        previewSize: Int = 3,
    ): Flow<List<Song>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT album.*, count(song.dateDownload) downloadCount
        FROM album_artist_map
            JOIN album ON album_artist_map.albumId = album.id
            JOIN song ON album_artist_map.albumId = song.albumId
        WHERE artistId = :artistId
        GROUP BY album.id
        LIMIT :previewSize
    """
    )
    fun artistAlbumsPreview(artistId: String, previewSize: Int = 6): Flow<List<Album>>

    @Transaction
    @Query("SELECT * FROM song WHERE id = :songId")
    fun song(songId: String?): Flow<Song?>

    @Transaction
    @Query("SELECT * FROM song WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): Song?

    @Transaction
    @Query("SELECT * FROM song WHERE id IN (:songIds)")
    suspend fun getSongsByIds(songIds: List<String>): List<Song>

    /** [getSongsByIds] that keeps up: a like or a new artist picture shows without asking again. */
    @Transaction
    @Query("SELECT * FROM song WHERE id IN (:songIds)")
    fun songsByIds(songIds: List<String>): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song")
    fun allSongs(): Flow<List<Song>>

    @Query("SELECT * FROM set_video_id WHERE videoId = :videoId")
    suspend fun getSetVideoId(videoId: String): SetVideoIdEntity?

    @Transaction
    @Query("SELECT * FROM format WHERE id = :id")
    fun format(id: String?): Flow<FormatEntity?>

    @Transaction
    @Query("SELECT * FROM lyrics WHERE id = :id")
    fun lyrics(id: String?): Flow<LyricsEntity?>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 ORDER BY rowId")
    fun artistsByCreateDateAsc(): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 ORDER BY name")
    fun artistsByNameAsc(): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 ORDER BY songCount")
    fun artistsBySongCountAsc(): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
                 JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN song
                                    ON song_artist_map.songId = song.id
                      GROUP BY artistId
                      ORDER BY totalPlayTime)
                     ON artist.id = artistId
        WHERE songCount > 0
    """
    )
    fun artistsByPlayTimeAsc(): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt")
    fun artistsBookmarkedByCreateDateAsc(): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY name")
    fun artistsBookmarkedByNameAsc(): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun artistsBookmarkedBySongCountAsc(): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
                 JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN song
                                    ON song_artist_map.songId = song.id
                      GROUP BY artistId
                      ORDER BY totalPlayTime)
                     ON artist.id = artistId
        WHERE bookmarkedAt IS NOT NULL
    """
    )
    fun artistsBookmarkedByPlayTimeAsc(): Flow<List<Artist>>

    fun artists(sortType: ArtistSortType, descending: Boolean) =
        when (sortType) {
            ArtistSortType.CREATE_DATE -> artistsByCreateDateAsc()
            ArtistSortType.NAME -> artistsByNameAsc()
            ArtistSortType.SONG_COUNT -> artistsBySongCountAsc()
            ArtistSortType.PLAY_TIME -> artistsByPlayTimeAsc()
        }.map { artists ->
            artists
                .filter { it.artist.isYouTubeArtist || it.artist.isLocal }
                .reversed(descending)
        }

    fun artistsBookmarked(sortType: ArtistSortType, descending: Boolean) =
        when (sortType) {
            ArtistSortType.CREATE_DATE -> artistsBookmarkedByCreateDateAsc()
            ArtistSortType.NAME -> artistsBookmarkedByNameAsc()
            ArtistSortType.SONG_COUNT -> artistsBookmarkedBySongCountAsc()
            ArtistSortType.PLAY_TIME -> artistsBookmarkedByPlayTimeAsc()
        }.map { artists ->
            artists
                .filter { it.artist.isYouTubeArtist || it.artist.isLocal }
                .reversed(descending)
        }

    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE id = :id")
    fun artist(id: String): Flow<Artist?>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY rowId")
    fun albumsByCreateDateAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY title")
    fun albumsByNameAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY year")
    fun albumsByYearAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY songCount")
    fun albumsBySongCountAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) ORDER BY duration")
    fun albumsByLengthAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT album.*
        FROM album
                 JOIN song
                      ON song.albumId = album.id
        WHERE EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL)
        GROUP BY album.id
        ORDER BY SUM(song.totalPlayTime)
    """,
    )
    fun albumsByPlayTimeAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY rowId")
    fun albumsLikedByCreateDateAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY title")
    fun albumsLikedByNameAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY year")
    fun albumsLikedByYearAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun albumsLikedBySongCountAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY duration")
    fun albumsLikedByLengthAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT album.*
        FROM album
                 JOIN song
                      ON song.albumId = album.id
        WHERE bookmarkedAt IS NOT NULL
        GROUP BY album.id
        ORDER BY SUM(song.totalPlayTime)
    """
    )
    fun albumsLikedByPlayTimeAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE isUploaded = 1 ORDER BY rowId")
    fun albumsUploadedByCreateDateAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY title")
    fun albumsUploadedByNameAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY year")
    fun albumsUploadedByYearAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun albumsUploadedBySongCountAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE bookmarkedAt IS NOT NULL ORDER BY duration")
    fun albumsUploadedByLengthAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT album.*
        FROM album
                 JOIN song
                      ON song.albumId = album.id
        WHERE bookmarkedAt IS NOT NULL
        GROUP BY album.id
        ORDER BY SUM(song.totalPlayTime)
    """
    )
    fun albumsUploadedByPlayTimeAsc(): Flow<List<Album>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM album WHERE id = :id")
    fun album(id: String): Flow<Album?>

    // The outer ORDER BY is needed because SQLite can flatten the sorted view's join.
    @Transaction
    @Query("""
        SELECT album.*, song_album_map.songId AS mappedSongId
        FROM album LEFT JOIN song_album_map ON album.id = song_album_map.albumId
        WHERE album.id = :albumId
        ORDER BY song_album_map.`index`, song_album_map.rowid
    """)
    fun albumSongRows(albumId: String): Flow<List<AlbumSongRow>>

    fun albumWithSongs(albumId: String): Flow<AlbumWithSongs?> =
        albumSongRows(albumId).map { rows ->
            rows.firstOrNull()?.let { first ->
                AlbumWithSongs(first.album, first.artists, rows.mapNotNull { it.song })
            }
        }

    // the albums your library songs come from, a song that's only in through a like counts when withLiked is on (the old way)
    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        "SELECT * FROM album WHERE EXISTS(SELECT 1 FROM song WHERE song.albumId = album.id " +
            "AND song.inLibrary IS NOT NULL AND (:withLiked OR NOT song.addedByLike)) ORDER BY rowId DESC"
    )
    fun albumsFromLibrarySongs(withLiked: Boolean): Flow<List<Album>>

    @Query("SELECT COUNT(*) FROM song WHERE inLibrary IS NOT NULL OR liked")
    fun librarySongCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM song WHERE isLocal AND albumId IS NULL AND albumName IS NOT NULL AND albumName != ''")
    suspend fun localSongsWithoutAlbum(): List<Song>

    // local files whose genre tag hasn't been read yet
    @Query("SELECT * FROM song WHERE isLocal AND genre IS NULL AND localUri IS NOT NULL")
    suspend fun localSongsWithoutGenre(): List<SongEntity>

    // songs the genres tab can sort: tagged files, plus anything saved, liked or played for a minute, which goes by its artist's genre
    @Transaction
    @Query(
        "SELECT * FROM song WHERE (isLocal AND genre IS NOT NULL AND genre != '') OR inLibrary IS NOT NULL OR liked OR totalPlayTime >= 60000 " +
            "ORDER BY title COLLATE NOCASE",
    )
    fun songsForGenres(): Flow<List<Song>>

    // songs liked after [since], in the same stored form as likedDate
    @Query("SELECT id FROM song WHERE liked = 1 AND likedDate > :since")
    fun likedSongIdsSince(since: Long): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM song WHERE isDownloaded = 1 ORDER BY dateDownload")
    fun downloadedSongsByCreateDateAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isDownloaded = 1 ORDER BY title")
    fun downloadedSongsByNameAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isDownloaded = 1 ORDER BY totalPlayTime")
    fun downloadedSongsByPlayTimeAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isUploaded = 1 ORDER BY dateDownload")
    fun uploadedSongsByCreateDateAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isUploaded = 1 ORDER BY title")
    fun uploadedSongsByNameAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isUploaded = 1 ORDER BY totalPlayTime")
    fun uploadedSongsByPlayTimeAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE isUploaded = 1 ORDER BY rowId")
    fun uploadedSongsByRowIdAsc(): Flow<List<Song>>

    @Transaction
    @Query("SELECT * FROM song WHERE title LIKE '%' || :query || '%' AND inLibrary IS NOT NULL LIMIT :previewSize")
    fun searchSongs(
        query: String,
        previewSize: Int = Int.MAX_VALUE,
    ): Flow<List<Song>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE name LIKE '%' || :query || '%' AND songCount > 0 LIMIT :previewSize",
    )
    fun searchArtists(
        query: String,
        previewSize: Int = Int.MAX_VALUE,
    ): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        "SELECT * FROM album WHERE title LIKE '%' || :query || '%' AND EXISTS(SELECT * FROM song WHERE song.albumId = album.id AND song.inLibrary IS NOT NULL) LIMIT :previewSize",
    )
    fun searchAlbums(
        query: String,
        previewSize: Int = Int.MAX_VALUE,
    ): Flow<List<Album>>

    @Transaction
    @Query(
        "SELECT song.* FROM (SELECT * from related_song_map GROUP BY relatedSongId) map JOIN song ON song.id = map.relatedSongId where songId = :songId",
    )
    fun getRelatedSongs(songId: String): Flow<List<Song>>

    // ---- hand edits from the song edit sheet ----

    @Query("SELECT * FROM song_edit WHERE songId = :songId")
    fun songEdit(songId: String?): Flow<SongEdit?>

    @Query("SELECT * FROM song_edit")
    fun songEdits(): Flow<List<SongEdit>>

    /** The covers of the songs liked last, which Liked songs' own cover is coloured from. */
    @Query("SELECT thumbnailUrl FROM song WHERE liked AND thumbnailUrl IS NOT NULL ORDER BY likedDate DESC LIMIT 4")
    fun likedCovers(): Flow<List<String>>

    // ---- sorted lists: the Name and Artist sorts compare in the device language (LibrarySort.kt) ----

    fun songs(
        sortType: SongSortType,
        descending: Boolean,
    ) = when (sortType) {
        SongSortType.CREATE_DATE -> songsByCreateDateAsc()
        SongSortType.NAME -> songsByNameAsc().map { songs -> songs.sortedBySongTitle() }
        SongSortType.ARTIST -> songsByRowIdAsc().map { songs -> songs.sortedBySongArtistsThenAlbum() }
        SongSortType.PLAY_TIME -> songsByPlayTimeAsc()
    }.map { it.reversed(descending) }

    fun localSongs(
        sortType: SongSortType,
        descending: Boolean,
    ) = when (sortType) {
        SongSortType.CREATE_DATE -> localSongsByCreateDateAsc()
        SongSortType.NAME -> localSongsByNameAsc().map { songs -> songs.sortedBySongTitle() }
        SongSortType.ARTIST -> localSongsByRowIdAsc().map { songs -> songs.sortedBySongArtistsThenAlbum() }
        SongSortType.PLAY_TIME -> localSongsByPlayTimeAsc()
    }.map { it.reversed(descending) }

    fun likedSongs(
        sortType: SongSortType,
        descending: Boolean,
    ) = when (sortType) {
        SongSortType.CREATE_DATE -> likedSongsByCreateDateAsc()
        SongSortType.NAME -> likedSongsByNameAsc().map { songs -> songs.sortedBySongTitle() }
        SongSortType.ARTIST -> likedSongsByRowIdAsc().map { songs -> songs.sortedBySongArtistsThenAlbum() }
        SongSortType.PLAY_TIME -> likedSongsByPlayTimeAsc()
    }.map { it.reversed(descending) }

    fun downloadedSongs(
        sortType: SongSortType,
        descending: Boolean
    ): Flow<List<Song>> = when (sortType) {
        SongSortType.CREATE_DATE -> downloadedSongsByCreateDateAsc()
        SongSortType.NAME -> downloadedSongsByNameAsc().map { songs -> songs.sortedBySongTitle() }
        // artist order only, no album grouping, as it always was here
        SongSortType.ARTIST -> downloadedSongsByNameAsc().map { songs -> songs.sortedBySongArtists() }
        SongSortType.PLAY_TIME -> downloadedSongsByPlayTimeAsc()
    }.map { it.reversed(descending) }

    fun uploadedSongs(
        sortType: SongSortType,
        descending: Boolean,
    ) = when (sortType) {
        SongSortType.CREATE_DATE -> uploadedSongsByCreateDateAsc()
        SongSortType.NAME -> uploadedSongsByNameAsc().map { songs -> songs.sortedBySongTitle() }
        SongSortType.ARTIST -> uploadedSongsByRowIdAsc().map { songs -> songs.sortedBySongArtistsThenAlbum() }
        SongSortType.PLAY_TIME -> uploadedSongsByPlayTimeAsc()
    }.map { it.reversed(descending) }

    fun albums(
        sortType: AlbumSortType,
        descending: Boolean,
    ) = when (sortType) {
        AlbumSortType.CREATE_DATE -> albumsByCreateDateAsc()
        AlbumSortType.NAME -> albumsByNameAsc().map { albums -> albums.sortedByAlbumTitle() }
        AlbumSortType.ARTIST -> albumsByCreateDateAsc().map { albums -> albums.sortedByAlbumArtists() }
        AlbumSortType.YEAR -> albumsByYearAsc()
        AlbumSortType.SONG_COUNT -> albumsBySongCountAsc()
        AlbumSortType.LENGTH -> albumsByLengthAsc()
        AlbumSortType.PLAY_TIME -> albumsByPlayTimeAsc()
    }.map { it.reversed(descending) }

    fun albumsLiked(
        sortType: AlbumSortType,
        descending: Boolean,
    ) = when (sortType) {
        AlbumSortType.CREATE_DATE -> albumsLikedByCreateDateAsc()
        AlbumSortType.NAME -> albumsLikedByNameAsc().map { albums -> albums.sortedByAlbumTitle() }
        AlbumSortType.ARTIST -> albumsLikedByCreateDateAsc().map { albums -> albums.sortedByAlbumArtists() }
        AlbumSortType.YEAR -> albumsLikedByYearAsc()
        AlbumSortType.SONG_COUNT -> albumsLikedBySongCountAsc()
        AlbumSortType.LENGTH -> albumsLikedByLengthAsc()
        AlbumSortType.PLAY_TIME -> albumsLikedByPlayTimeAsc()
    }.map { it.reversed(descending) }

    fun albumsUploaded(
        sortType: AlbumSortType,
        descending: Boolean,
    ) = when (sortType) {
        AlbumSortType.CREATE_DATE -> albumsUploadedByCreateDateAsc()
        AlbumSortType.NAME -> albumsUploadedByNameAsc().map { albums -> albums.sortedByAlbumTitle() }
        AlbumSortType.ARTIST -> albumsUploadedByCreateDateAsc().map { albums -> albums.sortedByAlbumArtists() }
        AlbumSortType.YEAR -> albumsUploadedByYearAsc()
        AlbumSortType.SONG_COUNT -> albumsUploadedBySongCountAsc()
        AlbumSortType.LENGTH -> albumsUploadedByLengthAsc()
        AlbumSortType.PLAY_TIME -> albumsUploadedByPlayTimeAsc()
    }.map { it.reversed(descending) }
}
