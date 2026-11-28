// SearchSource.kt

package com.mochyy.mochymusic.shared.media

import com.music.innertube.models.SongItem

/** Where the shared search screen gets songs from; on desktop and Android, YouTube Music. */
fun interface SearchSource {
    suspend fun searchSongs(query: String): Result<List<SongItem>>
}
