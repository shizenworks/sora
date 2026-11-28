/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Adapted from github.com/rukamori/lyrics for Mochy's shared Ktor client.
 */
package com.mochyy.mochymusic.shared.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

internal class LrcLibProvider(private val client: HttpClient) : LyricsProvider {
    override val name = "LRCLIB"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        durationSec: Int,
        isrc: String?,
    ): Result<String> = suspendRunCatching {
        val trackName = title.forLyricsSearch()
        val artistName = artist.artistForLyricsSearch()
        require(trackName.isNotBlank() && artistName.isNotBlank()) { "LRCLIB needs a title and artist" }
        val tracks = LyricsHttp.getJson<List<Track>>(client, "https://lrclib.net/api/search") {
            parameter("track_name", trackName)
            parameter("artist_name", artistName)
        }
        val matches = tracks.filter { track ->
            !track.instrumental && track.trackName.forLyricsSearch().equals(trackName, ignoreCase = true) &&
                track.artistName.artistForLyricsSearch().equals(artistName, ignoreCase = true) &&
                (durationSec <= 0 || abs(track.duration - durationSec) <= 2.0)
        }.sortedWith(
            compareByDescending<Track> { !it.syncedLyrics.isNullOrBlank() }
                .thenByDescending { album != null && album.equals(it.albumName, ignoreCase = true) }
                .thenBy { if (durationSec > 0) abs(it.duration - durationSec) else 0.0 },
        )
        matches.firstNotNullOfOrNull { track ->
            track.syncedLyrics?.takeIf(String::isNotBlank) ?: track.plainLyrics?.takeIf(String::isNotBlank)
        } ?: error("No matching LRCLIB lyrics")
    }

    @Serializable
    private data class Track(
        val trackName: String = "",
        val artistName: String = "",
        val albumName: String? = null,
        val duration: Double = 0.0,
        val instrumental: Boolean = false,
        val syncedLyrics: String? = null,
        val plainLyrics: String? = null,
    )
}

internal class SimpMusicLyricsProvider(private val client: HttpClient) : LyricsProvider {
    override val name = "SimpMusic"

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        durationSec: Int,
        isrc: String?,
    ): Result<String> = suspendRunCatching {
        require(VideoId.matches(id)) { "SimpMusic needs a YouTube video ID" }
        val response = LyricsHttp.getJson<Response>(client, "https://api-lyrics.simpmusic.org/v1/$id")
        check(response.type == "success") { "No SimpMusic lyrics" }
        val matches = response.data.filter { track ->
            (track.videoId == null || track.videoId == id) &&
                (durationSec <= 0 || track.duration == null || abs(track.duration.toLong() - durationSec) <= 5L)
        }.sortedWith(
            compareByDescending<Track> { !it.syncedLyrics.isNullOrBlank() }
                .thenBy { if (durationSec > 0 && it.duration != null) abs(it.duration.toLong() - durationSec) else Long.MAX_VALUE },
        )
        matches.firstNotNullOfOrNull { track ->
            track.syncedLyrics?.takeIf(String::isNotBlank) ?: track.plainLyrics?.takeIf(String::isNotBlank)
        } ?: error("No matching SimpMusic lyrics")
    }

    @Serializable
    private data class Response(val type: String? = null, val data: List<Track> = emptyList())

    @Serializable
    private data class Track(
        val videoId: String? = null,
        @SerialName("durationSeconds") val duration: Int? = null,
        val syncedLyrics: String? = null,
        @SerialName("plainLyric") val plainLyrics: String? = null,
    )

    private companion object {
        val VideoId = Regex("[A-Za-z0-9_-]{11}")
    }
}
