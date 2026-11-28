package com.mochyy.mochymusic.crossmix

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

// the real shape: one analysis thread, kept busy by a song that's decoding
class AnalysisQueueTest {
    private val thread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val queue = AnalysisQueue<String>(CoroutineScope(SupervisorJob() + thread))
    private val ran = Collections.synchronizedList(mutableListOf<String>())

    @Test
    fun skippedSongsDontHoldUpTheCurrentPair() = runBlocking {
        val busy = CountDownLatch(1)
        val playing = queue.submit("playing") { busy.await(5, TimeUnit.SECONDS); ran += "playing"; "playing" }
        // skipped past while the playing song was still decoding
        queue.submit("skipped-1") { ran += "skipped-1"; "skipped-1" }
        queue.submit("skipped-2") { ran += "skipped-2"; "skipped-2" }
        val next = queue.submit("next") { ran += "next"; "next" }

        queue.keepOnly(setOf("playing", "next"))
        busy.countDown()

        assertEquals("playing", playing.await())
        assertEquals("next", next.await())
        assertEquals(listOf("playing", "next"), ran.toList())
    }

    @Test
    fun aSongCalledOffBeforeItStartedCanBeAskedForAgain() = runBlocking {
        val busy = CountDownLatch(1)
        queue.submit("playing") { busy.await(5, TimeUnit.SECONDS); "playing" }
        queue.submit("song") { "first try" }
        queue.keepOnly(setOf("playing"))
        assertTrue("song" !in queue.pending())
        // the called-off job never ran, so it can't have cleaned up after itself: a new request must not get it
        val again = queue.submit("song") { "second try" }
        busy.countDown()
        assertEquals("second try", again.await())
    }

    @Test
    fun clearingTheQueueStopsADecodeAlreadyRunning() = runBlocking {
        // a decode loop that, like StreamPcmDecoder, checks for cancellation as it goes
        val started = CountDownLatch(1)
        val decoding = queue.submit("playing") {
            started.countDown()
            while (true) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                Thread.sleep(5)
            }
            @Suppress("UNREACHABLE_CODE") "never"
        }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        // the playback queue was cleared: nothing playing, nothing next
        queue.keepOnly(emptySet())
        decoding.join()
        assertTrue(decoding.isCancelled)
        assertTrue(queue.pending().isEmpty())
        // the thread is free again
        assertEquals("after", queue.submit("after") { "after" }.await())
    }

    @Test
    fun askingTwiceJoinsOneJob() = runBlocking {
        var runs = 0
        val busy = CountDownLatch(1)
        queue.submit("playing") { busy.await(5, TimeUnit.SECONDS); "playing" }
        val first = queue.submit("song") { runs++; "song" }
        val second = queue.submit("song") { runs++; "song" }
        busy.countDown()
        assertEquals("song", first.await())
        assertEquals("song", second.await())
        assertEquals(1, runs)
    }
}
