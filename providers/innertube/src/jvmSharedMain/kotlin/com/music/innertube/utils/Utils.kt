package com.music.innertube.utils

import com.music.innertube.YouTube
import com.music.innertube.pages.LibraryPage
import com.music.innertube.pages.PlaylistPage
import java.security.MessageDigest

// a page that fails, repeats or comes back empty stops here and leaves its token in songsContinuation, so a sync knows the list is short
@JvmName("completedLibrary")
suspend fun Result<PlaylistPage>.completed(): Result<PlaylistPage> = runCatching {
    val page = getOrThrow()
    val songs = page.songs.toMutableList()
    var continuation = page.songsContinuation
    val seenContinuations = mutableSetOf<String>()
    var requestCount = 0
    val maxRequests = 50

    while (continuation != null && requestCount < maxRequests) {
        if (continuation in seenContinuations) {
            break
        }
        seenContinuations.add(continuation)
        requestCount++

        val continuationPage = YouTube.playlistContinuation(continuation).getOrNull() ?: break
        // an empty page never carries a next token, and youtube only hands out a token when more songs follow
        if (continuationPage.songs.isEmpty()) break

        songs += continuationPage.songs
        continuation = continuationPage.continuation
    }
    PlaylistPage(
        playlist = page.playlist,
        songs = songs,
        songsContinuation = continuation,
        continuation = page.continuation
    )
}

// when paging stops early, continuation keeps the token it stopped on, so a sync knows the list is short
@JvmName("completedPlaylist")
suspend fun Result<LibraryPage>.completed(): Result<LibraryPage> = runCatching {
    val page = getOrThrow()
    val items = page.items.toMutableList()
    var continuation = page.continuation
    val seenContinuations = mutableSetOf<String>()
    var requestCount = 0
    val maxRequests = 50
    var consecutiveEmptyResponses = 0

    while (continuation != null && requestCount < maxRequests) {
        if (continuation in seenContinuations) {
            break
        }
        seenContinuations.add(continuation)
        requestCount++

        val continuationPage = YouTube.libraryContinuation(continuation).getOrNull() ?: break

        if (continuationPage.items.isEmpty()) {
            consecutiveEmptyResponses++
            if (consecutiveEmptyResponses >= 2) break
        } else {
            consecutiveEmptyResponses = 0
            items += continuationPage.items
        }

        continuation = continuationPage.continuation
    }
    LibraryPage(
        items = items,
        continuation = continuation
    )
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun sha1(str: String): String = MessageDigest.getInstance("SHA-1").digest(str.toByteArray()).toHex()
