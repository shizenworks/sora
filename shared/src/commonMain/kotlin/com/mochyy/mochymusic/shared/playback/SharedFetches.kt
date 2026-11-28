package com.mochyy.mochymusic.shared.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * One fetch per key at a time, shared by everyone who asks for that key while it runs, and cancelled
 * as soon as nobody is waiting for it any more. So the current song's lyrics and a preload of the
 * same song share one lookup, and a preload that was called off doesn't keep fetching on its own.
 * Call [fetch] from one thread (the player's); [scope] is asked for on each new fetch, so an owner
 * that replaces its scope is followed.
 */
class SharedFetches<K>(private val scope: () -> CoroutineScope) {
    private class Running(val job: Deferred<Unit>) {
        var waiting = 0
    }

    private val running = HashMap<K, Running>()

    suspend fun fetch(key: K, block: suspend () -> Unit) {
        val shared = running[key]?.takeIf { it.job.isActive }
            ?: Running(scope().async(start = CoroutineStart.LAZY) { block() }).also { started ->
                running[key] = started
                started.job.invokeOnCompletion { if (running[key] === started) running.remove(key) }
            }
        shared.waiting++
        try {
            shared.job.await()
        } finally {
            shared.waiting--
            // the last one waiting left (cancelled, or done): nothing is still interested in a fetch in flight
            if (shared.waiting == 0 && shared.job.isActive) shared.job.cancel()
        }
    }
}
