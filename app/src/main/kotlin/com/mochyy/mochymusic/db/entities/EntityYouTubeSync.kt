package com.mochyy.mochymusic.db.entities

import com.mochyy.mochymusic.db.nowLocal
import com.music.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// The entities live in shared/; the likes and library changes that also go to YouTube stay here,
// because the YouTube client is JVM-only.

// Pure local state update: callers sync to YouTube through SyncUtils.likeSong so retries,
// offline queueing, YouTubeLikesMemory and import-exclusion rules stay consistent (no double-firing).
fun SongEntity.toggleLike() = copy(
    liked = !liked,
    likedDate = if (!liked) nowLocal() else null,
    inLibrary = if (!liked) inLibrary ?: nowLocal() else inLibrary,
    addedByLike = if (!liked && inLibrary == null) true else addedByLike,
)

// byLike is for the youtube sync, where a liked song lands in the library on its own
fun SongEntity.toggleLibrary(syncToYouTube: Boolean = true, byLike: Boolean = false) = copy(
    liked = if (inLibrary == null) liked else false,
    inLibrary = if (inLibrary == null) nowLocal() else null,
    likedDate = if (inLibrary == null) likedDate else null,
    addedByLike = inLibrary == null && byLike,
).also {
    if (syncToYouTube) {
        CoroutineScope(Dispatchers.IO).launch {

            val addToLibrary = inLibrary == null
            YouTube.toggleSongLibrary(id, addToLibrary)
        }
    }
}

fun AlbumEntity.toggleLike() = localToggleLike().also {
    CoroutineScope(Dispatchers.IO).launch {
        // a local copy: smart casts don't reach a property declared in another module
        val playlistId = playlistId
        if (playlistId != null)
            YouTube.likePlaylist(playlistId, bookmarkedAt == null)
    }
}

fun PlaylistEntity.toggleLike() = localToggleLike().also {
    CoroutineScope(Dispatchers.IO).launch {
        val browseId = browseId
        if (browseId != null)
            YouTube.likePlaylist(browseId, bookmarkedAt == null)
    }
}

fun ArtistEntity.toggleLike() = localToggleLike().also {
    CoroutineScope(Dispatchers.IO).launch {
        val targetChannelId = channelId ?: YouTube.getChannelId(id)
        if (targetChannelId.isNotEmpty()) {
            YouTube.subscribeChannel(targetChannelId, bookmarkedAt == null)
        }
    }
}
