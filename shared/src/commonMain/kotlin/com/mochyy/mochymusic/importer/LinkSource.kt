package com.mochyy.mochymusic.importer

enum class LinkSource(val label: String, val example: String) {
    SPOTIFY("Spotify", "https://open.spotify.com/playlist/…"),
    APPLE_MUSIC("Apple Music", "https://music.apple.com/us/playlist/…"),
    DEEZER("Deezer", "https://www.deezer.com/playlist/…"),
    YOUTUBE("YouTube Music", "https://music.youtube.com/playlist?list=…"),
    SOUNDCLOUD("SoundCloud", "https://soundcloud.com/…/sets/…"),
}

/** works out which service a pasted link belongs to, or null if it isn't one we read */
fun detectLinkSource(link: String): LinkSource? {
    val text = link.trim()
    return when {
        text.contains("spotify") -> LinkSource.SPOTIFY
        text.contains("music.apple.com") -> LinkSource.APPLE_MUSIC
        text.contains("deezer") -> LinkSource.DEEZER
        text.contains("soundcloud.com") -> LinkSource.SOUNDCLOUD
        text.contains("youtube.com") || text.contains("youtu.be") -> LinkSource.YOUTUBE
        else -> null
    }
}
