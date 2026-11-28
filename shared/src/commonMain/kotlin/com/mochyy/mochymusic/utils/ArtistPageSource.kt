package com.mochyy.mochymusic.utils

import com.mochyy.mochymusic.models.ArtistGroup
import com.music.innertube.pages.ArtistPage

/**
 * The artist pages this run already has, and a way to fetch one it doesn't. Android provides it
 * from ArtistPageCache and the YouTube facade; the facade lives in innertube's jvmSharedMain, which
 * commonMain can't see, so this stays a platform contract until a shared artist source exists.
 * Platforms that don't load a module providing it never resolve ArtistSimilarViewModel.
 */
interface ArtistPageSource {
    fun page(artistId: String): ArtistPage?

    /** what a Home "Similar to" box showed for this artist, when the page was opened from one */
    fun picks(artistId: String): ArtistGroup?

    fun putPage(artistId: String, page: ArtistPage)

    /** The page, or the failure; the caller decides which failures are worth reporting. */
    suspend fun fetch(artistId: String): Result<ArtistPage>

    /** Writes the failure to the platform's log, as the Android implementation's reportException does. */
    fun reportFailure(throwable: Throwable)
}
