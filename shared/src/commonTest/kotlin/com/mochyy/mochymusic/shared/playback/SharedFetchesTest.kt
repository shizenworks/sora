package com.mochyy.mochymusic.shared.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// a provider that answers only when the test says so stands in for a slow lyrics lookup
@OptIn(ExperimentalCoroutinesApi::class)
class SharedFetchesTest {
    @Test
    fun twoCallersForTheSameSongShareOneFetch() = runTest {
        var started = 0
        val answer = CompletableDeferred<Unit>()
        val fetches = SharedFetches<String>({ backgroundScope })
        val preload = launch { fetches.fetch("b") { started++; answer.await() } }
        runCurrent()
        val current = launch { fetches.fetch("b") { started++; answer.await() } }
        runCurrent()
        answer.complete(Unit)
        preload.join()
        current.join()
        assertEquals(1, started)
    }

    @Test
    fun aCalledOffPreloadStopsItsFetch() = runTest {
        var finished = false
        var cancelled = false
        val answer = CompletableDeferred<Unit>()
        val fetches = SharedFetches<String>({ backgroundScope })
        val preload = launch {
            fetches.fetch("b") {
                try { answer.await(); finished = true } finally { if (!finished) cancelled = true }
            }
        }
        runCurrent()
        preload.cancel() // paused: the only one waiting leaves
        runCurrent()
        answer.complete(Unit)
        runCurrent()
        assertTrue(cancelled)
        assertEquals(false, finished)
    }

    @Test
    fun theFetchGoesOnWhileTheCurrentSongStillNeedsIt() = runTest {
        var finished = false
        val answer = CompletableDeferred<Unit>()
        val fetches = SharedFetches<String>({ backgroundScope })
        val preload = launch { fetches.fetch("b") { answer.await(); finished = true } }
        runCurrent()
        val current = launch { fetches.fetch("b") { answer.await(); finished = true } }
        runCurrent()
        preload.cancel()
        runCurrent()
        answer.complete(Unit)
        current.join()
        assertTrue(finished)
    }
}
