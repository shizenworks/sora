package com.mochyy.mochymusic.shared.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchiveTuneLyricsProvidersTest {
    @Test
    fun lrclibPrefersSyncedLyricsFromTheMatchingRecording() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals("Track", request.url.parameters["track_name"])
            assertEquals("Artist", request.url.parameters["artist_name"])
            jsonReply(
                """
                [
                  {"trackName":"Track","artistName":"Artist","duration":210,"plainLyrics":"Plain fallback"},
                  {"trackName":"Track","artistName":"Artist","duration":232,"syncedLyrics":"[00:03.00]Live recording"},
                  {"trackName":"Track","artistName":"Artist","duration":209.7,"syncedLyrics":"[00:02.00]Studio recording"}
                ]
                """.trimIndent(),
            )
        })
        try {
            val lyrics = LrcLibProvider(client)
                .getLyrics("a1b2c3d4e5f", "Track (Official Music Video)", "Artist - Topic", null, 210)
                .getOrThrow()
            assertEquals("[00:02.00]Studio recording", lyrics)
        } finally {
            client.close()
        }
    }

    @Test
    fun simpMusicAcceptsPlainLyricsAndRejectsOtherVideos() = runTest {
        val client = HttpClient(MockEngine { request ->
            assertEquals("https://api-lyrics.simpmusic.org/v1/a1b2c3d4e5f", request.url.toString())
            jsonReply(
                """
                {"type":"success","data":[
                  {"videoId":"a1b2c3d4e5g","durationSeconds":210,"syncedLyrics":"[00:02.00]Wrong song"},
                  {"videoId":"a1b2c3d4e5f","durationSeconds":210,"syncedLyrics":"","plainLyric":"Right song"}
                ]}
                """.trimIndent(),
            )
        })
        try {
            val provider = SimpMusicLyricsProvider(client)
            assertEquals("Right song", provider.getLyrics("a1b2c3d4e5f", "Track", "Artist", null, 210).getOrThrow())
            assertTrue(provider.getLyrics("a1b2c3d4e5f", "Track", "Artist", null, 240).isFailure)
        } finally {
            client.close()
        }
    }
}
