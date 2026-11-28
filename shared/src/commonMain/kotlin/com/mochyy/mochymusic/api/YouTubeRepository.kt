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

/**
 * The YouTube Music requests shared ViewModels make, in the shape Metrolist gives its shared
 * YouTubeRepository. providers/innertube builds the models and parsers for every platform but its
 * YouTube object only for Android and desktop, so commonMain talks to this interface instead and
 * the two JVM platforms delegate to that same object. Requests, results and failures are unchanged.
 */
interface YouTubeRepository {
    suspend fun album(browseId: String): Result<AlbumPage>

    suspend fun artist(browseId: String): Result<ArtistPage>

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage>

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage>

    suspend fun playlist(playlistId: String): Result<PlaylistPage>

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage>

    suspend fun explore(): Result<ExplorePage>

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>>

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>>

    suspend fun browse(browseId: String, params: String?): Result<BrowseResult>

    suspend fun getChartsPage(continuation: String? = null): Result<ChartsPage>

    suspend fun searchSummary(query: String): Result<SearchSummaryPage>

    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult>

    suspend fun searchContinuation(continuation: String): Result<SearchResult>

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions>

    suspend fun subscribeChannel(channelId: String, subscribe: Boolean): Result<Unit>

    suspend fun queue(videoIds: List<String>): Result<List<SongItem>>
}

/** YouTube's own filters, with the same values, worked in common code. */
data class SearchFilter(val value: String) {
    companion object {
        val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
        val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
        val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
        val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
        val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
        val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
    }
}

class YouTubeUnavailableOnIosException :
    Exception("YouTube browsing is not available on iOS yet")

/** Android and desktop delegate to innertube's YouTube object; iOS fails every request cleanly. */
expect fun platformYouTubeRepository(): YouTubeRepository
