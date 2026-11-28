/*
 * Ported from Natune (github.com/NatuneGroup/Natune, GPL-3.0). Original header:
 * ArchiveTune (2026), © Rukamori — github.com/rukamori, GPL-3.0 License, Portions © vossgraves — github.com/vossgraves
 */
package com.mochyy.mochymusic.shared.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonNames

/**
 * Port of the Android BetterLyrics Portato provider: the QQ Music endpoint of the BetterLyrics lyrics
 * API. The body is returned as the upstream sent it (XML, or the `ttml`/`lyrics` field of a JSON envelope).
 */
class BetterLyricsPortatoProvider(
    private val client: HttpClient,
) : LyricsProvider {
    override val name: String = "BetterLyricsPortato"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        durationSec: Int,
        isrc: String?,
    ): Result<String> =
        suspendRunCatching {
            require(title.isNotBlank() && artist.isNotBlank()) { "Song title and artist are required" }
            fetchLyrics(
                title = title.trim(),
                artist = artist.trim(),
                album = album?.trim().orEmpty(),
                durationSec = durationSec,
            ) ?: throw IllegalStateException("Portato lyrics unavailable")
        }

    /** Any transport or decode failure reads as no lyrics; cancellation still propagates. */
    private suspend fun fetchLyrics(
        title: String,
        artist: String,
        album: String,
        durationSec: Int,
    ): String? =
        suspendRunCatching {
            LyricsHttp
                .getWithStatus(client, BASE_URL + LYRICS_PATH, defaultUserAgent = false) {
                    parameter("s", title)
                    parameter("a", artist)
                    if (album.isNotBlank()) parameter("al", album)
                    if (durationSec > 0) parameter("d", durationSec)
                }.body
                ?.let { decodeLyrics(it) }
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()

    /** An XML body is returned trimmed; anything else must be a JSON envelope, returned untrimmed. */
    private fun decodeLyrics(responseText: String): String {
        val trimmed = responseText.trim()
        if (trimmed.startsWith("<")) return trimmed
        return LyricsJson.decodeFromString<BetterLyricsTtmlResponse>(responseText).ttml
    }

    private companion object {
        const val BASE_URL = "https://lyrics-api.boidu.dev/"
        const val LYRICS_PATH = "qq/getLyrics"
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class BetterLyricsTtmlResponse(
    @JsonNames("ttml", "lyrics")
    val ttml: String = "",
)
