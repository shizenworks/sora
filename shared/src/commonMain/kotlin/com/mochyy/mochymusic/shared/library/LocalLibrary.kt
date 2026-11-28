package com.mochyy.mochymusic.shared.library

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Upsert
import com.music.innertube.models.SongItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Entity(tableName = "library_tracks", primaryKeys = ["collection", "id"])
internal data class LibraryTrack(
    val collection: String,
    val id: String,
    val payload: String,
    val updatedAt: Long,
)

@Dao
internal interface LibraryDao {
    @Query("SELECT payload FROM library_tracks WHERE collection = :collection ORDER BY updatedAt DESC, id LIMIT :limit")
    fun observe(collection: String, limit: Int): Flow<List<String>>

    @Upsert
    suspend fun upsert(track: LibraryTrack)

    @Query("DELETE FROM library_tracks WHERE collection = :collection AND id = :id")
    suspend fun remove(collection: String, id: String)
}

@Database(entities = [LibraryTrack::class], version = 1, exportSchema = false)
@ConstructedBy(LibraryDatabaseConstructor::class)
internal abstract class LibraryDatabase : RoomDatabase() {
    abstract fun tracks(): LibraryDao
}

@Suppress("KotlinNoActualForExpect") // Room generates each platform's constructor.
internal expect object LibraryDatabaseConstructor : RoomDatabaseConstructor<LibraryDatabase> {
    override fun initialize(): LibraryDatabase
}

internal class LocalLibrary(private val tracks: LibraryDao) {
    private val json = Json { ignoreUnknownKeys = true }

    val saved = observe(SAVED, -1)
    val recent = observe(RECENT, 50)

    suspend fun setSaved(song: SongItem, saved: Boolean) {
        if (saved) write(SAVED, song) else tracks.remove(SAVED, song.id)
    }

    suspend fun recordPlay(song: SongItem) {
        write(RECENT, song)
    }

    private fun observe(collection: String, limit: Int): Flow<List<SongItem>> =
        tracks.observe(collection, limit).map { rows ->
            rows.mapNotNull { payload ->
                try {
                    json.decodeFromString<SongItem>(payload)
                } catch (_: SerializationException) {
                    null
                }
            }
        }

    private suspend fun write(collection: String, song: SongItem) {
        val stableSong = song.copy(
            endpoint = null,
            libraryAddToken = null,
            libraryRemoveToken = null,
            historyRemoveToken = null,
            setVideoId = null,
        )
        tracks.upsert(LibraryTrack(
            collection = collection,
            id = song.id,
            payload = json.encodeToString(stableSong),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        ))
    }

    private companion object {
        const val SAVED = "saved"
        const val RECENT = "recent"
    }
}
