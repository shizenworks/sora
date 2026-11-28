package com.mochyy.mochymusic.playback

import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.mochyy.mochymusic.constants.QueuePreventDuplicatesKey
import com.mochyy.mochymusic.utils.dataStore
import com.mochyy.mochymusic.utils.get

/**
 * With "prevent duplicates" on, takes out the songs still to come that match [items], so the
 * re-added song moves to its new place instead of showing up twice. Songs already played and the
 * song playing now are never touched.
 */
fun MusicService.removeUpcomingDuplicates(items: List<MediaItem>) {
    if (!dataStore.get(QueuePreventDuplicatesKey, false)) return
    val ids = items.mapTo(HashSet()) { it.mediaId }
    val timeline = player.currentTimeline
    if (timeline.isEmpty) return
    // the songs still to come in the order they will play (shuffle included), without wrapping
    // round to the start, so with repeat all the already played ones stay
    val matches = ArrayList<Int>()
    var index = player.currentMediaItemIndex
    while (true) {
        index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
        if (index == C.INDEX_UNSET) break
        if (player.getMediaItemAt(index).mediaId in ids) matches += index
    }
    // highest position first, so the ones still to remove don't move
    matches.sortedDescending().forEach(player::removeMediaItem)
}
