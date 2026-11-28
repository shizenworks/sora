package com.mochyy.mochymusic.shared.playback

import com.music.innertube.models.SongItem

/**
 * The songs this session played, in the order they started. Previous walks back through it;
 * Recently Played can't be used for that, because a song moves to its top every time it plays
 * again, so stepping back twice from C in A → B → C could land on C instead of A.
 */
class PlaybackHistory(private val limit: Int = 200) {
    private val played = ArrayDeque<SongItem>()
    private val seen = HashSet<String>()

    val isEmpty: Boolean get() = played.isEmpty()

    /** Whether [id] started at any point this session, even if Previous has walked back past it. */
    fun playedThisSession(id: String): Boolean = id in seen

    fun started(song: SongItem) {
        seen += song.id
        if (played.lastOrNull()?.id == song.id) return
        played.addLast(song)
        if (played.size > limit) played.removeFirst()
    }

    /**
     * [song] is from before this session (Previous went past its start), so the history starts over
     * from it: a further Previous keeps walking back in time instead of returning to this session.
     */
    fun startedBefore(song: SongItem) {
        seen += song.id
        played.clear()
        played.addLast(song)
    }

    /**
     * The song before [current], taken off the history along with [current] itself; starting it
     * puts it back on top through [started]. Null when nothing played before [current], and then
     * the history is left as it was, so a Previous that only restarts the first song loses nothing.
     */
    fun stepBack(current: SongItem): SongItem? {
        val currentOnTop = played.lastOrNull()?.id == current.id
        val previousIndex = if (currentOnTop) played.size - 2 else played.size - 1
        if (previousIndex < 0) return null
        if (currentOnTop) played.removeLast()
        return played.removeLast()
    }
}
