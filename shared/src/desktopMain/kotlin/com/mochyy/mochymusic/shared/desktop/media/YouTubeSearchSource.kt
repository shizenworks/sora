// YouTubeSearchSource.kt

package com.mochyy.mochymusic.shared.desktop.media

import com.mochyy.mochymusic.shared.media.SearchSource
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem

/** Song search on YouTube Music through the same facade the Android app uses. */
class YouTubeSearchSource : SearchSource {
    @Volatile
    private var visitorReady = false

    override suspend fun searchSongs(query: String): Result<List<SongItem>> {
        // a guest session needs visitor data before YouTube answers searches consistently
        if (!visitorReady && YouTube.visitorData == null) {
            YouTube.refreshVisitorData().onSuccess { visitorReady = true }
        }
        return YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).map { result ->
            result.items.filterIsInstance<SongItem>()
        }
    }
}
