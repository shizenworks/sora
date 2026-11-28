package com.mochyy.mochymusic.shared.lyrics

import com.music.innertube.models.SongItem
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal data class PlayerLyrics(val lines: List<LyricsLine>, val synced: Boolean, val provider: String)

internal fun playerLyricsProviders(http: HttpClient): List<LyricsProvider> = listOf(
    LrcLibProvider(http),
    SimpMusicLyricsProvider(http),
    BiniLyricsProvider(http),
    MegalobizLyricsProvider(http),
)

internal suspend fun findPlayerLyrics(song: SongItem, providers: List<LyricsProvider>): PlayerLyrics? =
    withContext(Dispatchers.Default) {
        var plainLyrics: PlayerLyrics? = null
        for (provider in providers) {
            val response = withTimeoutOrNull(8_000) {
                provider.getLyrics(song.id, song.title, song.artists.firstOrNull()?.name.orEmpty(), song.album?.name, song.duration ?: 0)
            }?.getOrElse { error ->
                if (error is CancellationException) throw error
                null
            } ?: continue
            val normalized = LyricsNormalizer.normalize(response)
            val isTtml = LyricsPayload.isTtmlDocument(normalized)
            val lines = if (isTtml) LyricsParser.parseTtml(normalized) else LyricsParser.parse(normalized)
            if (lines.isNotEmpty()) return@withContext PlayerLyrics(lines, synced = true, provider = provider.name)
            if (plainLyrics == null && !isTtml && !normalized.startsWith("<")) {
                val plainLines = normalized.lineSequence().map(String::trim).filter(String::isNotEmpty)
                    .map { SyncedLine(time = 0L, text = it) }.toList()
                if (plainLines.isNotEmpty()) plainLyrics = PlayerLyrics(plainLines, synced = false, provider = provider.name)
            }
        }
        plainLyrics
    }
