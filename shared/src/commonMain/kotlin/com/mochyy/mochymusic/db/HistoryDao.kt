package com.mochyy.mochymusic.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomWarnings
import androidx.room.Transaction
import com.mochyy.mochymusic.db.entities.Album
import com.mochyy.mochymusic.db.entities.Artist
import com.mochyy.mochymusic.db.entities.EventWithSong
import com.mochyy.mochymusic.db.entities.Playlist
import com.mochyy.mochymusic.db.entities.RecognitionHistory
import com.mochyy.mochymusic.db.entities.SearchHistory
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.db.entities.SongWithStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// The play history, stats and search history queries every platform can run: each one returns a
// Flow or suspends. The blocking ones stay in the app's DatabaseDao.
@Dao
interface HistoryDao {
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    /** Total time spent listening, in milliseconds, across every recorded play event. */
    @Query("SELECT COALESCE(SUM(playTime), 0) FROM event")
    fun totalListeningTimeMs(): Flow<Long>

    @Query(
        """
        SELECT s.*
        FROM song s
        JOIN (
            SELECT e.songId, SUM(e.playTime) as totalPlayTime
            FROM event e
            JOIN song_artist_map sam ON e.songId = sam.songId
            WHERE sam.artistId = :artistId AND e.timestamp >= :fromTimeStamp AND e.timestamp <= :toTimeStamp
            GROUP BY e.songId
        ) AS play_times ON s.id = play_times.songId
        ORDER BY play_times.totalPlayTime DESC
        """
    )
    fun mostPlayedSongsByArtist(artistId: String, fromTimeStamp: Long, toTimeStamp: Long): Flow<List<Song>>

    @Transaction
    @Query(
        """
        SELECT song.*
        FROM (SELECT *, COUNT(1) AS referredCount
              FROM related_song_map
              GROUP BY relatedSongId) map
                 JOIN song ON song.id = map.relatedSongId
        WHERE songId IN (SELECT songId
                         FROM (SELECT songId
                               FROM event
                               ORDER BY ROWID DESC
                               LIMIT 5)
                         UNION
                         SELECT songId
                         FROM (SELECT songId
                               FROM event
                               WHERE timestamp > :now - 86400000 * 7
                               GROUP BY songId
                               ORDER BY SUM(playTime) DESC
                               LIMIT 5)
                         UNION
                         SELECT id
                         FROM (SELECT id
                               FROM song
                               ORDER BY totalPlayTime DESC
                               LIMIT 10))
        ORDER BY referredCount DESC
        LIMIT 100
    """,
    )
    fun quickPicks(now: Long = nowEpochMillis()): Flow<List<Song>>

    @Transaction
    @Query(
        """
        SELECT
            song.*
        FROM
            event
        JOIN
            song ON event.songId = song.id
        WHERE
            event.timestamp > (:now - 86400000 * 7 * 2)
        GROUP BY
            song.albumId
        HAVING
            song.albumId IS NOT NULL
        ORDER BY
            sum(event.playTime) DESC
        LIMIT :limit
        OFFSET :offset

        """,
    )
    fun getRecommendationAlbum(
        now: Long = nowEpochMillis(),
        limit: Int = 5,
        offset: Int = 0,
    ): Flow<List<Song>>

    @Transaction
    @Query(
        """
        SELECT s.id, s.title, s.thumbnailUrl, s.isVideo,
               (SELECT name FROM artist WHERE id = sam.artistId) as artistName,
               (SELECT COUNT(1)
                FROM event
                WHERE songId = s.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS songCountListened,
               (SELECT SUM(event.playTime)
                FROM event
                WHERE songId = s.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS timeListened
        FROM song s
        LEFT JOIN song_artist_map sam ON s.id = sam.songId
        JOIN (SELECT songId
              FROM event
              WHERE timestamp > :fromTimeStamp
                AND timestamp <= :toTimeStamp
              GROUP BY songId
              ORDER BY SUM(playTime) DESC
              LIMIT :limit) AS top_songs ON s.id = top_songs.songId
        GROUP BY s.id
        ORDER BY timeListened DESC
        LIMIT :limit OFFSET :offset
        """,
    )
    fun mostPlayedSongsStats(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = nowLocal().toEpochMillisUtc(),
    ): Flow<List<SongWithStats>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT song.*,
               (SELECT COUNT(1)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS songCountListened,
               (SELECT SUM(event.playTime)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS timeListened
        FROM song
        JOIN (SELECT songId
                     FROM event
                     WHERE timestamp > :fromTimeStamp
                     AND timestamp <= :toTimeStamp
                     GROUP BY songId
                     ORDER BY SUM(playTime) DESC
                     LIMIT :limit)
        ON song.id = songId
        LIMIT :limit
        OFFSET :offset
    """,
    )
    fun mostPlayedSongs(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = nowLocal().toEpochMillisUtc(),
    ): Flow<List<Song>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS songCount,
               (SELECT SUM(event.playTime)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS timeListened
        FROM artist
                 JOIN(SELECT artistId, SUM(songTotalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN (SELECT songId, SUM(playTime) AS songTotalPlayTime
                                     FROM event
                                     WHERE timestamp > :fromTimeStamp
                                     AND timestamp <= :toTimeStamp
                                     GROUP BY songId) AS e
                                    ON song_artist_map.songId = e.songId
                      GROUP BY artistId
                      ORDER BY totalPlayTime DESC
                      LIMIT :limit
                      OFFSET :offset)
                     ON artist.id = artistId
    """,
    )
    fun mostPlayedArtists(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = nowLocal().toEpochMillisUtc(),
    ): Flow<List<Artist>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
    SELECT album.*,
           COUNT(DISTINCT song_album_map.songId) as downloadCount,
           (SELECT COUNT(1)
            FROM song_album_map
                     JOIN event e ON song_album_map.songId = e.songId
            WHERE albumId = album.id
              AND e.timestamp > :fromTimeStamp
              AND e.timestamp <= :toTimeStamp) AS songCountListened,
           (SELECT SUM(e.playTime)
            FROM song_album_map
                     JOIN event e ON song_album_map.songId = e.songId
            WHERE albumId = album.id
              AND e.timestamp > :fromTimeStamp
              AND e.timestamp <= :toTimeStamp) AS timeListened
    FROM album
    JOIN song_album_map ON album.id = song_album_map.albumId
    WHERE album.id IN (
        SELECT sam.albumId
        FROM event
                 JOIN song_album_map sam ON event.songId = sam.songId
        WHERE event.timestamp > :fromTimeStamp
          AND event.timestamp <= :toTimeStamp
        GROUP BY sam.albumId
        HAVING sam.albumId IS NOT NULL
    )
    GROUP BY album.id
    ORDER BY timeListened DESC
    LIMIT :limit OFFSET :offset
    """
    )
    fun mostPlayedAlbums(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = nowLocal().toEpochMillisUtc(),
    ): Flow<List<Album>>

    @Transaction
    @Query(
        """
        SELECT playlist.*, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist_event
        JOIN playlist ON playlist_event.playlistId = playlist.id
        WHERE playlist_event.timestamp > :fromTimeStamp AND playlist_event.timestamp <= :toTimeStamp
        GROUP BY playlist_event.playlistId
        ORDER BY count(playlist_event.id) DESC
        LIMIT :limit
        OFFSET :offset
        """
    )
    fun mostPlayedPlaylists(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = nowLocal().toEpochMillisUtc(),
    ): Flow<List<Playlist>>

    @Transaction
    @Query(
        """
        SELECT song.* FROM song
        JOIN (SELECT songId, MAX(timestamp) AS lastPlayed FROM event GROUP BY songId) e
        ON song.id = e.songId
        ORDER BY e.lastPlayed DESC
        LIMIT :limit
        """
    )
    fun recentlyPlayedSongs(limit: Int = 15): Flow<List<Song>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT album.*,
               (SELECT COUNT(1) FROM song_album_map sam2 JOIN event e2 ON sam2.songId = e2.songId WHERE sam2.albumId = album.id) AS songCountListened,
               (SELECT SUM(e2.playTime) FROM song_album_map sam2 JOIN event e2 ON sam2.songId = e2.songId WHERE sam2.albumId = album.id) AS timeListened
        FROM album
        JOIN (
            SELECT sam.albumId AS albumId, MAX(e.timestamp) AS lastPlayed
            FROM song_album_map sam
            JOIN event e ON sam.songId = e.songId
            GROUP BY sam.albumId
        ) recency ON album.id = recency.albumId
        ORDER BY recency.lastPlayed DESC
        LIMIT :limit
        """
    )
    fun recentlyPlayedAlbums(limit: Int = 15): Flow<List<Album>>

    @Transaction
    @Query(
        """
        SELECT playlist.*, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount
        FROM playlist_event
        JOIN playlist ON playlist_event.playlistId = playlist.id
        GROUP BY playlist_event.playlistId
        ORDER BY MAX(playlist_event.timestamp) DESC
        LIMIT :limit
        """
    )
    fun recentlyPlayedPlaylists(limit: Int = 15): Flow<List<Playlist>>

    @Query("SELECT SUM(playTime) FROM event WHERE timestamp >= :fromTimeStamp AND timestamp <= :toTimeStamp")
    fun getTotalPlayTimeInRange(fromTimeStamp: Long, toTimeStamp: Long): Flow<Long?>

    @Query("SELECT COUNT(DISTINCT songId) FROM event WHERE timestamp >= :fromTimeStamp AND timestamp <= :toTimeStamp")
    fun getUniqueSongCountInRange(fromTimeStamp: Long, toTimeStamp: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT artistId)
        FROM event
        JOIN song_artist_map ON event.songId = song_artist_map.songId
        WHERE timestamp >= :fromTimeStamp AND timestamp <= :toTimeStamp
    """
    )
    fun getUniqueArtistCountInRange(fromTimeStamp: Long, toTimeStamp: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(DISTINCT albumId)
        FROM event
        JOIN song ON event.songId = song.id
        WHERE timestamp >= :fromTimeStamp AND timestamp <= :toTimeStamp
    """
    )
    fun getUniqueAlbumCountInRange(fromTimeStamp: Long, toTimeStamp: Long): Flow<Int>

    @Query("SELECT sum(count) from playCount WHERE song = :songId")
    fun getLifetimePlayCount(songId: String?): Flow<Int>

    @Query("SELECT sum(count) from playCount WHERE song = :songId AND year = :year")
    fun getPlayCountByYear(songId: String?, year: Int): Flow<Int>

    @Query("SELECT count from playCount WHERE song = :songId AND year = :year AND month = :month")
    fun getPlayCountByMonth(songId: String?, year: Int, month: Int): Flow<Int>

    @Transaction
    @Query(
        """
        SELECT song.*
        FROM (SELECT n.songId      AS eid,
                     SUM(playTime) AS oldPlayTime,
                     newPlayTime
              FROM event
                       JOIN
                   (SELECT songId, SUM(playTime) AS newPlayTime
                    FROM event
                    WHERE timestamp > (:now - 86400000 * 30 * 1)
                    GROUP BY songId
                    ORDER BY newPlayTime) as n
                   ON event.songId = n.songId
              WHERE timestamp < (:now - 86400000 * 30 * 1)
              GROUP BY n.songId
              ORDER BY oldPlayTime) AS t
                 JOIN song on song.id = t.eid
        WHERE 0.2 * t.oldPlayTime > t.newPlayTime
        LIMIT 100
    """
    )
    fun forgottenFavorites(now: Long = nowEpochMillis()): Flow<List<Song>>

    @Transaction
    @Query(
        """
        SELECT song.*
        FROM event
                 JOIN
             song ON event.songId = song.id
        WHERE event.timestamp > (:now - 86400000 * 7 * 2)
        GROUP BY song.albumId
        HAVING song.albumId IS NOT NULL
        ORDER BY sum(event.playTime) DESC
        LIMIT :limit
        OFFSET :offset
        """,
    )
    fun recommendedAlbum(
        now: Long = nowEpochMillis(),
        limit: Int = 5,
        offset: Int = 0,
    ): Flow<List<Song>>

    @Transaction
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT DISTINCT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id) AS songCount
        FROM artist
                 LEFT JOIN(SELECT artistId, SUM(songTotalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN (SELECT songId, SUM(playTime) AS songTotalPlayTime
                                     FROM event
                                     GROUP BY songId) AS e
                                    ON song_artist_map.songId = e.songId
                      GROUP BY artistId
                      ORDER BY totalPlayTime DESC) AS artistTotalPlayTime
                     ON artist.id = artistId
                     OR artist.bookmarkedAt IS NOT NULL
                     ORDER BY
                      CASE
                        WHEN artistTotalPlayTime.artistId IS NULL THEN 1
                        ELSE 0
                      END,
                      artistTotalPlayTime.totalPlayTime DESC
    """,
    )
    fun allArtistsByPlayTime(): Flow<List<Artist>>

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId DESC")
    fun events(): Flow<List<EventWithSong>>

    /** The newest [limit] plays, for callers that only need the head of [events]. */
    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId DESC LIMIT :limit")
    fun recentEvents(limit: Int): Flow<List<EventWithSong>>

    @Transaction
    @Query("SELECT * FROM event ORDER BY rowId ASC LIMIT 1")
    fun firstEvent(): Flow<EventWithSong?>

    @Query("SELECT COUNT(*) FROM event")
    fun eventCount(): Flow<Int>

    // lightweight distinct song ids for discovery filtering without loading every joined event row
    @Query("SELECT DISTINCT songId FROM event")
    suspend fun allPlayedSongIds(): List<String>

    /** Every play in [fromTimeStamp, toTimeStamp), bare - the monthly recap and Stats aggregate these themselves. */
    @Query("SELECT songId, timestamp, playTime FROM event WHERE timestamp >= :fromTimeStamp AND timestamp < :toTimeStamp")
    suspend fun recapEvents(fromTimeStamp: Long, toTimeStamp: Long): List<RecapEventRow>

    // which of these songs were already played before a period started, so stats can tell new finds from old favourites
    @Query("SELECT DISTINCT songId FROM event WHERE timestamp < :beforeTimeStamp AND songId IN (:songIds)")
    suspend fun songsPlayedBefore(songIds: List<String>, beforeTimeStamp: Long): List<String>

    // all the listening in a period added up, for stats' "more than last month"
    @Query("SELECT COALESCE(SUM(playTime), 0) FROM event WHERE timestamp >= :fromTimeStamp AND timestamp < :toTimeStamp")
    suspend fun listeningTotal(fromTimeStamp: Long, toTimeStamp: Long): Long

    @Query("SELECT MIN(timestamp) FROM event")
    suspend fun firstEventTimestamp(): Long?

    @Transaction
    @Query("SELECT * FROM search_history WHERE `query` LIKE :query || '%' ORDER BY id DESC")
    fun searchHistory(query: String = ""): Flow<List<SearchHistory>>

    @Transaction
    @Query("SELECT * FROM recognition_history ORDER BY recognizedAt DESC")
    fun recognitionHistory(): Flow<List<RecognitionHistory>>

    @Transaction
    @Query("SELECT * FROM recognition_history WHERE id = :id")
    fun recognitionHistoryById(id: Long): Flow<RecognitionHistory?>

    @Transaction
    @Query("SELECT * FROM recognition_history WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY recognizedAt DESC")
    fun searchRecognitionHistory(query: String): Flow<List<RecognitionHistory>>
}
