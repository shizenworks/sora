package com.mochyy.mochymusic.importer.account

enum class AccountService(
    val route: String,
    val label: String,
    /** Where sign-in starts; null for YouTube Music, which uses the app's own account. */
    val loginUrl: String?,
    /** Sites whose cookies and storage belong to this service, cleared by "Use a different account". */
    val webOrigins: List<String>,
    /** The web player that sends the session after sign-in; only it is watched, never the login pages. */
    val playerOrigins: Set<String> = emptySet(),
    /** Tidal's web player turns phones away, so it's opened as a desktop site. */
    val desktopSite: Boolean = false,
) {
    SPOTIFY(
        route = "spotify",
        label = "Spotify",
        loginUrl = "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F",
        webOrigins = listOf("https://accounts.spotify.com", "https://open.spotify.com", "https://www.spotify.com"),
        playerOrigins = setOf("https://open.spotify.com"),
    ),
    APPLE_MUSIC(
        route = "apple_music",
        label = "Apple Music",
        loginUrl = "https://music.apple.com/us/library/playlists",
        webOrigins = listOf("https://music.apple.com", "https://idmsa.apple.com", "https://authorize.music.apple.com", "https://www.apple.com"),
        playerOrigins = setOf("https://music.apple.com"),
    ),
    TIDAL(
        route = "tidal",
        label = "Tidal",
        loginUrl = "https://listen.tidal.com/login",
        webOrigins = listOf("https://listen.tidal.com", "https://login.tidal.com", "https://tidal.com"),
        playerOrigins = setOf("https://listen.tidal.com"),
        desktopSite = true,
    ),
    YOUTUBE_MUSIC(
        route = "youtube_music",
        label = "YouTube Music",
        loginUrl = null,
        webOrigins = emptyList(),
    );

    companion object {
        fun fromRoute(route: String?): AccountService? = entries.firstOrNull { it.route == route }
    }
}

data class RemotePlaylist(
    val id: String,
    val name: String,
    val trackCount: Int?,
    val subtitle: String? = null,
    /** Goes into Liked songs instead of becoming a playlist. */
    val isLiked: Boolean = false,
    /**
     * The account's whole song library rather than a playlist: goes into the Library's Songs (and
     * Artists, Albums) as songs, not into a playlist named after the service.
     */
    val isLibrary: Boolean = false,
    val coverUrl: String? = null,
)
