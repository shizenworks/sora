// Queue.kt

package com.mochyy.mochymusic.playback.queues

import androidx.media3.common.MediaItem
import com.mochyy.mochymusic.extensions.metadata
import com.mochyy.mochymusic.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?

    // the song this queue starts on, when that's known before it's read, so a pick can fetch it while the old song still plays
    val firstSongId: String? get() = preloadItem?.id

    // its next pages are youtube's picks after what you chose (an album's radio), so they only play with autoplay on
    val continuesAsRadio: Boolean get() = false

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaItem>

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true) =
            if (enabled) without { it.metadata?.explicit == true } else this

        fun filterVideoSongs(disableVideos: Boolean = false) =
            if (disableVideos) without { it.metadata?.isVideoSong == true } else this

        // the song that was picked always stays (it was asked for by name), and the start index moves with it,
        // so taking songs out before it can't start a different one
        private fun without(hidden: (MediaItem) -> Boolean): Status {
            val picked = items.getOrNull(mediaItemIndex)
            val kept = items.filterIndexed { index, item -> index == mediaItemIndex || !hidden(item) }
            return copy(items = kept, mediaItemIndex = picked?.let { kept.indexOf(it) }?.takeIf { it >= 0 } ?: 0)
        }
    }
}

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else {
        this
    }

fun List<MediaItem>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
        filterNot { it.metadata?.isVideoSong == true }
    } else {
        this
    }
