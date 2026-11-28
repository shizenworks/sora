package com.mochyy.mochymusic.shared.library

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.music.innertube.models.Artist
import com.music.innertube.models.SongItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalLibraryTest {
    @Test
    fun savedAndRecentTracksPersistIndependently() = runTest {
        val directory = createTempDirectory("mochy-library").toFile()
        fun openDatabase() = Room.databaseBuilder<LibraryDatabase>(directory.resolve("library.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()

        val song = SongItem(
            id = "test-song",
            title = "Test song",
            artists = listOf(Artist(name = "Test artist", id = null)),
            thumbnail = "",
        )

        try {
            val first = openDatabase()
            try {
                val library = LocalLibrary(first.tracks())
                library.recordPlay(song)
                library.setSaved(song, true)
            } finally {
                first.close()
            }

            val reopened = openDatabase()
            try {
                val library = LocalLibrary(reopened.tracks())
                assertEquals(listOf(song), library.saved.first())
                assertEquals(listOf(song), library.recent.first())
                library.setSaved(song, false)
                assertEquals(emptyList(), library.saved.first())
                assertEquals(listOf(song), library.recent.first())
            } finally {
                reopened.close()
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
