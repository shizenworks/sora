package com.mochyy.mochymusic.shared.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The one owner of fetching ahead for the songs coming up next. Every trigger (a song change,
 * playback starting, the queue or shuffle changing) calls [request] with the upcoming songs in play
 * order. The same songs while a fetch is still running are left to it; different songs cancel it and
 * start again. So two triggers never fetch for the same songs at once, and an old order's fetch
 * doesn't keep running after the order changed. Call it from one thread (the player's). [scope] is
 * asked for on each fetch, so an owner that replaces its scope (a restarted service) is followed.
 */
class UpcomingPreloader<T>(
    private val scope: () -> CoroutineScope,
    private val key: (T) -> String,
    private val fetch: suspend (List<T>) -> Unit,
) {
    private var job: Job? = null
    private var keys: List<String>? = null

    fun request(upcoming: List<T>) {
        val next = upcoming.map(key)
        if (next == keys && job?.isActive == true) return
        job?.cancel()
        keys = next
        job = if (upcoming.isEmpty()) null else scope().launch { fetch(upcoming) }
    }

    fun cancel() {
        job?.cancel()
        job = null
        keys = null
    }
}
