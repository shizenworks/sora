package com.mochyy.mochymusic.utils

import com.mochyy.mochymusic.models.ArtistGroup
import com.music.innertube.YouTube
import com.music.innertube.pages.ArtistPage

/** Android's ArtistPageSource: this run's ArtistPageCache in front of the YouTube facade. */
object AndroidArtistPageSource : ArtistPageSource {
    override fun page(artistId: String): ArtistPage? = ArtistPageCache.page(artistId)

    override fun picks(artistId: String): ArtistGroup? = ArtistPageCache.picks(artistId)

    override fun putPage(artistId: String, page: ArtistPage) = ArtistPageCache.putPage(artistId, page)

    override suspend fun fetch(artistId: String): Result<ArtistPage> = YouTube.artist(artistId)

    override fun reportFailure(throwable: Throwable) = reportException(throwable)
}
