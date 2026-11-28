package com.mochyy.mochymusic.db

import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mochyy.mochymusic.db.entities.SongEntity
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InternalDatabaseTest {
    @Test
    fun opensAndReadsASongBackThroughSongDao() = runTest {
        val directory = createTempDirectory("mochy-db").toFile()
        val database = Room.databaseBuilder<InternalDatabase>(directory.resolve("mochy.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            database.suspendingDao.insertSongWithArtists(
                SongEntity(id = "test-song", title = "Test song", duration = 180),
                listOf(SongArtistInput(id = null, name = "Test artist")),
            )

            val song = assertNotNull(database.songDao.getSongById("test-song"))
            assertEquals("Test song", song.song.title)
            assertEquals(180, song.song.duration)
            assertEquals(listOf("Test artist"), song.artists.map { it.name })
            assertEquals(MusicDatabaseVersion, database.useReaderConnection { connection ->
                connection.usePrepared("PRAGMA user_version") { it.step(); it.getLong(0).toInt() }
            })
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }

    @Test
    fun checkpointsTheWriteAheadLog() = runTest {
        val directory = createTempDirectory("mochy-db").toFile()
        val database = Room.databaseBuilder<InternalDatabase>(directory.resolve("mochy.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            // the app's checkpoint() is raw(SupportSQLiteQuery) on Android; shared code runs the same PRAGMA
            database.suspendingDao.checkpoint()
        } finally {
            database.close()
            directory.deleteRecursively()
        }
    }
}
