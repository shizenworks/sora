package com.mochyy.mochymusic.shared.ios

import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.ExtractedStream
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.models.YouTubeClient
import com.mochyy.mochymusic.shared.lyrics.suspendRunCatching
import com.mochyy.mochymusic.shared.media.SearchSource
import com.music.innertube.models.SongItem
import com.music.innertube.models.response.SearchResponse
import com.music.innertube.pages.SearchPage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

internal class IosYouTubeClient : SearchSource {
    val http = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }
    private val logger = InnerTubeLogger { }
    private val innerTube = InnerTube(http, logger = logger)
    private val configStore = RemotePlayerConfigStore(http, PlayerConfigRepository.disabled(), logger)
    private val cipher = YouTubeCipherService(http, configStore, logger)
    private val extractor = InnerTubeExtractor(
        configParser = YtConfigParserImpl(http, innerTube, configStore, logger, cipher),
        cipherService = cipher,
        innerTube = innerTube,
        tokenProvider = null,
        logger = logger,
    )

    override suspend fun searchSongs(query: String): Result<List<SongItem>> = suspendRunCatching {
        innerTube.fetchFreshVisitorData(innerTube.sessionSnapshot(), forceRefresh = false)
        val response = innerTube.search(
            client = YouTubeClient.WEB_REMIX,
            query = query,
            params = SearchPage.SONG_FILTER,
            setLogin = false,
        ).body<SearchResponse>()
        SearchPage.parse(response).items.filterIsInstance<SongItem>()
    }

    suspend fun resolve(song: SongItem): ExtractedStream? = withTimeoutOrNull(25_000) {
        extractor.extract(
            videoId = song.id,
            hints = ContentHints(isExplicit = song.explicit)
                .withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = false),
            excludedClients = emptySet(),
            // AVPlayer needs an MP4 audio stream for this path. WebM is handled by FFmpeg on desktop.
            audioQuality = AudioQuality.MP4,
            clientPlaybackNonce = generateClientPlaybackNonce(),
        )?.takeIf { it.audioUrl.isNotBlank() && it.sabrBootstrap == null }
    }

    suspend fun close() {
        innerTube.close()
        http.close()
        cipher.dispose()
    }
}
