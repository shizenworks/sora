package com.mochyy.mochymusic.ui.player.styles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mochyy.mochymusic.playback.PlayerConnection

/**
 * A seek-bar drag bound to the queue item it started on.
 *
 * The player sheet stays open across a track change (headset Next, the song ending), so a drag that
 * began on one song could otherwise be let go on the next one and seek it to the old song's fraction.
 * The item is the media id together with its queue index, so the same song queued twice still counts
 * as a change. Once the item changes the drag is dropped for good: the bar goes back to the live
 * position and lifting the finger seeks nothing. The next drag starts fresh and commits once.
 */
@Stable
class StyleScrub internal constructor(private val connection: PlayerConnection) {
    /** The fraction being dragged to, or null when the bar shows the live position. */
    var fraction by mutableStateOf<Float?>(null)
        private set

    private var startedOn: Pair<String?, Int>? = null
    private var dropped = false

    fun update(value: Float) {
        if (dropped) return
        val item = currentItem()
        if (startedOn == null) {
            startedOn = item
        } else if (startedOn != item) {
            dropped = true
            fraction = null
            return
        }
        fraction = value
    }

    /** Ends the drag; seeks only when it is still on the item it started on and [commit] allows it. */
    fun finish(durationMs: Long, commit: Boolean) {
        val value = fraction
        val sameItem = !dropped && startedOn != null && startedOn == currentItem()
        fraction = null
        startedOn = null
        dropped = false
        if (value != null && sameItem && commit) connection.seekTo((durationMs * value).toLong())
    }

    private fun currentItem(): Pair<String?, Int> =
        connection.player.currentMediaItem?.mediaId to connection.player.currentMediaItemIndex
}

@Composable
fun rememberStyleScrub(connection: PlayerConnection): StyleScrub = remember(connection) { StyleScrub(connection) }
