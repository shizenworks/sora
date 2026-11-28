// PlayerQueue.kt
// Queues the shared UI can hand to any platform's player. The Android app's own Queue
// (playback/queues) stays where it is for now: its pages are media3 MediaItems and MusicService
// reads them directly, so this is the portable side of the same idea, over the shared SongItem.

package com.mochyy.mochymusic.shared.playback

import com.music.innertube.models.SongItem

/**
 * A queue to start playing: an initial page of songs and, when the source has more (a playlist, a
 * radio), further pages. Implementations that call YouTube belong where YouTube is reachable and
 * can implement this later; a plain list is [SongQueue].
 */
interface Queue {
    /** The song the queue starts on when that is known before it is read, so it can be fetched while the old song plays. */
    val preloadItem: SongItem? get() = null

    val firstSongId: String? get() = preloadItem?.id

    /** Its next pages are YouTube's picks after what was chosen (an album's radio), so they only play with autoplay on. */
    val continuesAsRadio: Boolean get() = false

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean = false

    suspend fun nextPage(): List<SongItem> = emptyList()

    data class Status(
        val title: String?,
        val items: List<SongItem>,
        val mediaItemIndex: Int = 0,
        val position: Long = 0L,
    )
}

/** A queue of songs already in hand, the portable counterpart of the app's ListQueue. */
class SongQueue(
    val title: String? = null,
    override val preloadItem: SongItem? = null,
    val items: List<SongItem>,
    val startIndex: Int = 0,
    val position: Long = 0L,
) : Queue {
    override val firstSongId: String? get() = items.getOrNull(startIndex)?.id

    override suspend fun getInitialStatus(): Queue.Status = Queue.Status(title, items, startIndex, position)

    override fun hasNextPage(): Boolean = false
}

/** Nothing to play; the counterpart of the app's EmptyQueue. */
object EmptyQueue : Queue {
    override suspend fun getInitialStatus(): Queue.Status = Queue.Status(null, emptyList(), -1)

    override fun hasNextPage(): Boolean = false
}
