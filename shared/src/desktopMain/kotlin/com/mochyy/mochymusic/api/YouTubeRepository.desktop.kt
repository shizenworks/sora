package com.mochyy.mochymusic.api

import com.music.innertube.YouTube
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

actual fun platformYouTubeRepository(): YouTubeRepository = DesktopYouTubeRepository

/** Desktop's facade: the same YouTube object calls, since desktop also runs innertube's JVM client. */
private object DesktopYouTubeRepository : YouTubeRepository {
    override suspend fun album(browseId: String): Result<AlbumPage> = YouTube.album(browseId)

    override suspend fun artist(browseId: String): Result<ArtistPage> = YouTube.artist(browseId)

    override suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> =
        YouTube.artistItems(endpoint)

    override suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> =
        YouTube.artistItemsContinuation(continuation)

    override suspend fun playlist(playlistId: String): Result<PlaylistPage> = YouTube.playlist(playlistId)

    override suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        YouTube.playlistContinuation(continuation)

    override suspend fun explore(): Result<ExplorePage> = YouTube.explore()

    override suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = YouTube.newReleaseAlbums()

    override suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = YouTube.moodAndGenres()

    override suspend fun browse(browseId: String, params: String?): Result<BrowseResult> =
        YouTube.browse(browseId, params)

    override suspend fun getChartsPage(continuation: String?): Result<ChartsPage> =
        YouTube.getChartsPage(continuation)

    override suspend fun searchSummary(query: String): Result<SearchSummaryPage> =
        YouTube.searchSummary(query)

    override suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> =
        YouTube.search(query, YouTube.SearchFilter(filter.value))

    override suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        YouTube.searchContinuation(continuation)

    override suspend fun searchSuggestions(query: String): Result<SearchSuggestions> =
        YouTube.searchSuggestions(query)

    override suspend fun subscribeChannel(channelId: String, subscribe: Boolean): Result<Unit> =
        YouTube.subscribeChannel(channelId, subscribe).map { _ -> }

    override suspend fun queue(videoIds: List<String>): Result<List<SongItem>> = YouTube.queue(videoIds)
}
