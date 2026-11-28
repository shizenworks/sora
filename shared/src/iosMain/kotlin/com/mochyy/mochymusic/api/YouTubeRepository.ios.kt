package com.mochyy.mochymusic.api

import com.music.innertube.models.AlbumItem
import com.music.innertube.models.BrowseEndpoint
import com.music.innertube.models.SearchSuggestions
import com.music.innertube.models.SongItem
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistItemsContinuationPage
import com.music.innertube.pages.ArtistItemsPage
import com.music.innertube.pages.ArtistPage
import com.music.innertube.pages.BrowseResult
import com.music.innertube.pages.ChartsPage
import com.music.innertube.pages.ExplorePage
import com.music.innertube.pages.MoodAndGenres
import com.music.innertube.pages.PlaylistContinuationPage
import com.music.innertube.pages.PlaylistPage
import com.music.innertube.pages.SearchResult
import com.music.innertube.pages.SearchSummaryPage

actual fun platformYouTubeRepository(): YouTubeRepository = UnavailableYouTubeRepository

/**
 * iOS has no YouTube object to delegate to: providers/innertube builds its facade only for the JVM.
 * Every request fails with the same clear reason instead of the screens waiting forever.
 */
private object UnavailableYouTubeRepository : YouTubeRepository {
    private fun <T> unavailable(): Result<T> =
        Result.failure(YouTubeUnavailableOnIosException())

    override suspend fun album(browseId: String): Result<AlbumPage> = unavailable()

    override suspend fun artist(browseId: String): Result<ArtistPage> = unavailable()

    override suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = unavailable()

    override suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> =
        unavailable()

    override suspend fun playlist(playlistId: String): Result<PlaylistPage> = unavailable()

    override suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        unavailable()

    override suspend fun explore(): Result<ExplorePage> = unavailable()

    override suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = unavailable()

    override suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = unavailable()

    override suspend fun browse(browseId: String, params: String?): Result<BrowseResult> = unavailable()

    override suspend fun getChartsPage(continuation: String?): Result<ChartsPage> = unavailable()

    override suspend fun searchSummary(query: String): Result<SearchSummaryPage> = unavailable()

    override suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> = unavailable()

    override suspend fun searchContinuation(continuation: String): Result<SearchResult> = unavailable()

    override suspend fun searchSuggestions(query: String): Result<SearchSuggestions> = unavailable()

    override suspend fun subscribeChannel(channelId: String, subscribe: Boolean): Result<Unit> = unavailable()

    override suspend fun queue(videoIds: List<String>): Result<List<SongItem>> = unavailable()
}
