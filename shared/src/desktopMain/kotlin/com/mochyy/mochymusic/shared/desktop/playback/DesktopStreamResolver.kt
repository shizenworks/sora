// DesktopStreamResolver.kt
// YouTube stream resolution for desktop through InnerTubeX (github.com/MetrolistGroup/innertubex,
// GPL-3.0), built the way Mochy's Android InnerTubeXStreams builds it, and adapted from Natune's desktop
// InnerTubeX tier (github.com/NatuneGroup/Natune, GPL-3.0).

package com.mochyy.mochymusic.shared.desktop.playback

import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.InnerTube as InnerTubeX
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/** A playable audio URL and the headers googlevideo expects with it (it checks the client that minted it). */
data class ResolvedStream(
    val url: String,
    val headers: Map<String, String>,
    val mimeType: String?,
    val codecs: String?,
    val bitrate: Int,
)

class DesktopStreamResolver : AutoCloseable {
    private val clientLock = Any()
    @Volatile private var closed = false
    private var client: HttpClient? = null

    private fun httpClient(): HttpClient = synchronized(clientLock) {
        check(!closed) { "Desktop stream resolver is closed" }
        client ?: HttpClient(OkHttp) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true })
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
        }.also { client = it }
    }

    private val extractor by lazy {
        val http = httpClient()
        val logger = InnerTubeLogger { }
        val innerTube = InnerTubeX(http, logger = logger)
        val configStore = RemotePlayerConfigStore(http, PlayerConfigRepository.disabled(), logger)
        val cipher = YouTubeCipherService(http, configStore, logger)
        // no token provider: the desktop asks as a visitor, so InnerTubeX picks clients that need no PO token
        InnerTubeExtractor(
            configParser = YtConfigParserImpl(http, innerTube, configStore, logger, cipher),
            cipherService = cipher,
            innerTube = innerTube,
            tokenProvider = null,
            logger = logger,
        )
    }

    /** Closes the HTTP connection pool when the desktop player is released. */
    override fun close() {
        val activeClient = synchronized(clientLock) {
            closed = true
            client.also { client = null }
        }
        activeClient?.close()
    }

    private val prewarmMutex = Mutex()
    private var prewarmed = false

    suspend fun resolve(videoId: String): ResolvedStream? {
        prewarmOnce()
        // FFmpeg reads one progressive URL: no HLS manifests, no SABR segments, no client-side byte ranges
        val hints = ContentHints(wantVideo = false)
            .withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = false)
        val stream = withTimeoutOrNull(GiveUpMs) {
            extractor.extract(
                videoId = videoId,
                hints = hints,
                excludedClients = emptySet(),
                audioQuality = AudioQuality.HIGH,
                clientPlaybackNonce = generateClientPlaybackNonce(),
            )
        } ?: return null
        if (stream.sabrBootstrap != null || stream.audioUrl.isBlank()) return null
        return ResolvedStream(
            url = stream.audioUrl,
            // header values reach FFmpeg as one CRLF-joined option, so a value with a line break is dropped
            headers = stream.headers.filter { (name, value) ->
                name.isNotBlank() && value.isNotBlank() && value.none { it == '\r' || it == '\n' }
            },
            mimeType = stream.mimeType,
            codecs = stream.codecs,
            bitrate = stream.bitrate ?: 0,
        )
    }

    /** Warming is an optimisation: a failed warm never fails playback and the next resolve tries again. */
    private suspend fun prewarmOnce() = prewarmMutex.withLock {
        if (prewarmed) return@withLock
        try {
            extractor.prewarm()
            prewarmed = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
        }
    }

    private companion object {
        const val GiveUpMs = 25_000L
    }
}
