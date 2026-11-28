// PlayerController.kt
// What the shared UI knows about playback, on any platform. Each platform's engine implements it:
// desktop with FFmpeg and Java Sound, iOS with AVFoundation; Android keeps its MusicService.

package com.mochyy.mochymusic.shared.playback

import com.music.innertube.models.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackRepeatMode {
    OFF,
    ONE,
    ALL,
}

/** One entry in the order the active player will visit its queue. [queueId] distinguishes repeated songs. */
data class PlaybackQueueItem(
    val queueId: String,
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String? = null,
    val durationMs: Long = 0L,
    val isAutoplay: Boolean = false,
    val isManual: Boolean = false,
)

/** A small, platform-neutral view of the active playback queue and its transport modes. */
data class PlaybackQueueState(
    val title: String? = null,
    val items: List<PlaybackQueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: PlaybackRepeatMode = PlaybackRepeatMode.OFF,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
)

private val EmptyPlaybackQueueState = MutableStateFlow(PlaybackQueueState())

/** The codec and rate of what is actually playing, for Mochy's format badge. */
data class StreamFormat(
    /** "OPUS", "AAC", "FLAC"... as the decoder names it */
    val codec: String,
    /** samples per second the decoder is given, 0 when not known yet */
    val sampleRate: Int = 0,
    /** bits per second of the source stream, 0 when the source doesn't say */
    val bitrate: Int = 0,
)

data class PlayerState(
    val song: SongItem? = null,
    val isPlaying: Boolean = false,
    /** a stream is being resolved or opened: the play button shows progress */
    val isLoading: Boolean = false,
    val hasEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val format: StreamFormat? = null,
    /** why the last song couldn't play, shown under the title until the next one starts */
    val error: String? = null,
)

interface PlayerController {
    val state: StateFlow<PlayerState>

    /** Implementations expose their native queue when one exists; simple single-track players stay empty. */
    val queueState: StateFlow<PlaybackQueueState>
        get() = EmptyPlaybackQueueState

    /** Resolves [song]'s stream and plays it, replacing whatever was playing. */
    fun play(song: SongItem)

    /** Replaces the native queue and starts at [startIndex]. */
    fun playQueue(songs: List<SongItem>, startIndex: Int = 0, title: String? = null) {
        songs.getOrNull(startIndex)?.let(::play)
    }

    /** Adds songs to the end of a platform queue when the implementation supports one. */
    fun queueSongs(songs: List<SongItem>) {}

    /** Places songs immediately after the current item when the implementation supports it. */
    fun queueNext(songs: List<SongItem>) {}

    /** Skip commands default safely for single-track implementations. */
    fun skipToNext() {}

    fun skipToPrevious() {
        seekTo(0L)
    }

    fun playQueueItem(queueId: String) {}

    fun playQueueItemNext(queueId: String) {}

    fun removeQueueItem(queueId: String) {}

    fun moveQueueItem(queueId: String, afterQueueId: String) {}

    fun setShuffleEnabled(enabled: Boolean) {}

    fun setRepeatMode(mode: PlaybackRepeatMode) {}

    /** Warm at most one upcoming song without playback; repeat calls for the same ID should be idempotent.
     * Passing null cancels and drops an unclaimed prefetch.
     */
    fun preloadNext(song: SongItem?) {}

    fun togglePlayPause()

    fun seekTo(positionMs: Long)

    fun release()
}
