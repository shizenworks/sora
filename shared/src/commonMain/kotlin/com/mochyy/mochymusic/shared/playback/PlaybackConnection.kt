// PlaybackConnection.kt
// The player shared Compose code can ask for on every platform: what is playing, where it is, what
// comes around it, and the transport. Android's PlayerConnection, and the desktop and iOS players,
// implement it; each platform root provides LocalPlayerConnection.

package com.mochyy.mochymusic.shared.playback

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import com.music.innertube.models.SongItem
import kotlinx.coroutines.flow.StateFlow

/** The songs either side of the one playing, for the player's compact queue. */
data class PlayerQueueWindow(
    val previous: SongItem? = null,
    val current: SongItem? = null,
    val next: SongItem? = null,
)

/**
 * What the UI reads and does with the player, with nothing of media3 or AVFoundation in it. The
 * queue itself is the portable [Queue] below, so a screen can start one without knowing which
 * engine plays it.
 */
@Stable
interface PlaybackConnection : PlayerController {
    /** The song now playing, as the shared models describe it. */
    val nowPlaying: StateFlow<SongItem?>

    /** True while the player means to be playing; a paused fade still reports the press at once. */
    val isPlaying: StateFlow<Boolean>

    /** Current position in ms. */
    val positionMs: Long get() = state.value.positionMs

    /** Current track length in ms, 0 while it isn't known yet. */
    val durationMs: Long get() = state.value.durationMs

    val queueWindow: StateFlow<PlayerQueueWindow>

    fun play() {
        if (!state.value.isPlaying) togglePlayPause()
    }

    fun pause() {
        if (state.value.isPlaying) togglePlayPause()
    }

    /** Replaces what is playing with [queue] and starts at its own start index. */
    fun playQueue(queue: Queue)

    /** Adds [song] to the end of what plays next, as "Add to Queue" does. */
    fun addToQueue(song: SongItem)

    /** Puts [song] straight after the one playing. */
    fun playNext(song: SongItem)
}

/**
 * `null` where nothing provides it, so a screen can `?: return` as the Android app's callers do;
 * every platform root below the app provides the real one.
 */
val LocalPlayerConnection = staticCompositionLocalOf<PlaybackConnection?> {
    error("No PlayerConnection provided")
}
