package com.mochyy.mochymusic.shared.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingPreloaderTest {
    @Test
    fun playbackStartAndSongChangeForTheSameSongsFetchOnce() = runTest {
        val started = mutableListOf<List<String>>()
        val gate = CompletableDeferred<Unit>()
        val preloader = UpcomingPreloader<String>({ this }, { it }) { started += it; gate.await() }

        preloader.request(listOf("b", "c")) // playback started
        runCurrent()
        preloader.request(listOf("b", "c")) // the song-change listener, same songs coming up
        runCurrent()

        assertEquals(listOf(listOf("b", "c")), started)
        gate.complete(Unit)
    }

    @Test
    fun shuffleChangeWhileFetchingCancelsTheOldOrder() = runTest {
        val finished = mutableListOf<List<String>>()
        val gates = mutableMapOf<List<String>, CompletableDeferred<Unit>>()
        val preloader = UpcomingPreloader<String>({ this }, { it }) { songs ->
            gates.getOrPut(songs) { CompletableDeferred() }.await()
            finished += songs
        }

        preloader.request(listOf("b", "c"))
        runCurrent()
        preloader.request(listOf("d", "b")) // shuffle turned on: different songs next
        runCurrent()
        gates.values.forEach { it.complete(Unit) }
        runCurrent()

        assertEquals(listOf(listOf("d", "b")), finished)
    }

    @Test
    fun theSameSongsFetchAgainOnceTheLastFetchEnded() = runTest {
        var runs = 0
        val preloader = UpcomingPreloader<String>({ this }, { it }) { runs++ }
        preloader.request(listOf("b"))
        runCurrent()
        preloader.request(listOf("b"))
        runCurrent()
        assertEquals(2, runs)
    }

    @Test
    fun nothingComingUpCancelsAndFetchesNothing() = runTest {
        var cancelled = false
        val gate = CompletableDeferred<Unit>()
        val preloader = UpcomingPreloader<String>({ this }, { it }) {
            try { gate.await() } finally { cancelled = true }
        }
        preloader.request(listOf("b"))
        runCurrent()
        preloader.request(emptyList())
        runCurrent()
        assertEquals(true, cancelled)
    }
}
