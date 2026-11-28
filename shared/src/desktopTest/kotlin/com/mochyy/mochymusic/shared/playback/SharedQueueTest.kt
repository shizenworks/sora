package com.mochyy.mochymusic.shared.playback

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mochyy.mochymusic.shared.library.LibraryDatabase
import com.mochyy.mochymusic.shared.library.LocalLibrary
import com.mochyy.mochymusic.shared.settings.PlaybackPreferences
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toOkioPath
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The queue with no UI at all, the way a CarPlay-only launch runs it: the saved Up next comes
 * back, a song ending starts the next one, and the system Next control works.
 */
class SharedQueueTest {
    private fun song(id: String) = SongItem(id = id, title = id, artists = emptyList(), thumbnail = "")

    private class FakePlayer : PlayerController {
        val mutableState = MutableStateFlow(PlayerState())
        val played = MutableStateFlow<List<String>>(emptyList())
        override val state: StateFlow<PlayerState> = mutableState
        override fun play(song: SongItem) {
            mutableState.value = PlayerState(song = song, isPlaying = true)
            played.value = played.value + song.id
        }
        override fun togglePlayPause() {}
        override fun seekTo(positionMs: Long) {}
        override fun release() {}
    }

    private fun withQueue(
        block: suspend (SharedQueue, FakePlayer, QueueTransport, QueuePlayerConnection, suspend (() -> Unit) -> Unit) -> Unit,
    ) {
        val directory = createTempDirectory("mochy-queue").toFile()
        val executor = Executors.newSingleThreadExecutor()
        val main = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob() + main)
        // the settings store's actor runs on its own scope: the queue only cancels the jobs it started,
        // so a store left on DataStore's default scope keeps reading a directory this test deletes
        val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val database = Room.databaseBuilder<LibraryDatabase>(directory.resolve("library.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            runBlocking {
                val settings = PlaybackPreferences(
                    PreferenceDataStoreFactory.createWithPath(scope = settingsScope) {
                        directory.resolve("settings.preferences_pb").toOkioPath()
                    },
                )
                settings.setUpcomingQueue(listOf(song("b"), song("c")))
                val player = FakePlayer()
                val transport = QueueTransport()
                val queue = SharedQueue(player, settings, LocalLibrary(database.tracks()), transport, scope)
                val connection = QueuePlayerConnection(player, queue, scope)
                // the queue is confined to its one thread, as it is to the main thread in the apps
                val onQueueThread: suspend (() -> Unit) -> Unit = { action -> withContext(main) { action() } }
                onQueueThread { queue.start() }
                withTimeout(10_000) { block(queue, player, transport, connection, onQueueThread) }
                onQueueThread { queue.close() }
            }
        } finally {
            // stop both scopes and wait before the database closes and the directory goes away: a
            // coroutine still running then fails on a thread no test owns, and kotlinx-coroutines-test
            // reports that as UncaughtExceptionsBeforeTest in whatever test runs next
            runBlocking {
                withTimeout(10_000) {
                    scope.coroutineContext[Job]!!.cancelAndJoin()
                    settingsScope.coroutineContext[Job]!!.cancelAndJoin()
                }
            }
            database.close()
            executor.shutdown()
            executor.awaitTermination(10, TimeUnit.SECONDS)
            directory.deleteRecursively()
        }
    }

    @Test
    fun aColdStartRestoresUpNextAndMovesOnWhenASongEnds() = withQueue { queue, player, _, _, onQueueThread ->
        queue.ready.first { it }
        assertEquals(listOf("b", "c"), queue.upcoming.value.map { it.id })

        onQueueThread { queue.play(song("a")) }
        player.mutableState.value = player.mutableState.value.copy(isPlaying = false, hasEnded = true)

        player.played.first { it == listOf("a", "b") }
        queue.upcoming.first { songs -> songs.map { it.id } == listOf("c") }
    }

    @Test
    fun theSystemNextControlPlaysTheHeadOfUpNext() = withQueue { _, player, transport, _, onQueueThread ->
        transport.canGoNext.first { it }
        onQueueThread { transport.next() }
        player.played.first { it == listOf("b") }
    }

    @Test
    fun nextKeepsTheSecondOccurrenceOfARepeatedSong() = withQueue { queue, player, _, _, onQueueThread ->
        queue.ready.first { it }
        onQueueThread {
            queue.playList(listOf(song("a"), song("b"), song("b"), song("c")), startIndex = 0, startPositionMs = 0L)
            queue.playNext()
            assertEquals(listOf("b", "c"), queue.upcoming.value.map { it.id })
            queue.playNext()
            assertEquals(listOf("a", "b", "b"), player.played.value)
            assertEquals(listOf("c"), queue.upcoming.value.map { it.id })
        }
    }

    @Test
    fun aSlowQueueCannotInterruptANewerSongSelection() = withQueue { queue, player, _, connection, onQueueThread ->
        queue.ready.first { it }
        val reading = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val slowQueue = object : Queue {
            override suspend fun getInitialStatus(): Queue.Status {
                reading.complete(Unit)
                return try {
                    awaitCancellation()
                } catch (_: CancellationException) {
                    cancelled.complete(Unit)
                    Queue.Status(null, listOf(song("old")))
                }
            }
        }
        onQueueThread { connection.playQueue(slowQueue) }
        reading.await()

        onQueueThread { connection.play(song("new")) }
        cancelled.await()

        onQueueThread { assertEquals(listOf("new"), player.played.value) }
    }
}
