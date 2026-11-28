package com.mochyy.mochymusic.importer

import com.music.innertube.models.SongItem

data class ImportedTrack(
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long? = null,
    /** exact youtube id when the source already knows it (mochy csv, youtube links) — no search needed */
    val videoId: String? = null,
    val thumbnailUrl: String? = null,
    /** full youtube item when the source is youtube itself, so nothing is lost converting back */
    val ytItem: SongItem? = null,
    /** when it was added to the source's list (liked, saved to the library), so imports keep that order */
    val addedAtMs: Long? = null,
    /** the track's own id on its service ("spotify:track:..."), which a hand-picked version is saved under */
    val sourceId: String? = null,
)

/** where imported music came from, kept on every imported song so the library can list imported from spotify and the youtube sync leaves these songs alone */
enum class ImportSource(val key: String, val label: String) {
    SPOTIFY("spotify", "Spotify"),
    APPLE_MUSIC("apple_music", "Apple Music"),
    TIDAL("tidal", "Tidal"),
    YOUTUBE_MUSIC("youtube_music", "YouTube Music"),
    DEEZER("deezer", "Deezer"),
    SOUNDCLOUD("soundcloud", "SoundCloud"),

    /** a file or a service with no name of its own (a tunemymusic csv, a mochy backup) */
    OTHER("other", "other sources");

    /** the sources the library gives a category of their own */
    val isListed: Boolean get() = this != OTHER

    companion object {
        fun fromKey(key: String?): ImportSource? = entries.firstOrNull { it.key == key }
    }
}

data class ParsedImport(
    val likedSongs: List<ImportedTrack>,
    val playlists: Map<String, List<ImportedTrack>>,
    /** songs that go straight into the library (an account's whole song library), in no playlist */
    val librarySongs: List<ImportedTrack> = emptyList(),
    /** things the user should know before importing, e.g. a source that only shares part of a playlist */
    val warnings: List<String> = emptyList(),
    /** which service this came from; null when that isn't known (a plain csv) */
    val provider: ImportSource? = null,
    /** each playlist's own cover on the source service, by its name in playlists */
    val playlistCovers: Map<String, String> = emptyMap(),
    /** where each playlist lives on the service, by its name in playlists, so it can be re-synced */
    val playlistOrigins: Map<String, PlaylistOrigin> = emptyMap(),
) {
    val totalSongs: Int get() = likedSongs.size + librarySongs.size + playlists.values.sumOf { it.size }
    val totalPlaylists: Int get() = playlists.size
}

/** where a playlist lives on its service. either is enough for re-sync to read it again */
data class PlaylistOrigin(
    /** its id on the service: what the service's account lists it under */
    val remoteId: String? = null,
    /** the share link it was read from */
    val remoteUrl: String? = null,
    /** whoever made it there, so the playlist page can still credit them after the import */
    val author: PlaylistAuthor? = null,
)

/** the person, curator or artist a playlist belongs to on its service */
data class PlaylistAuthor(
    val name: String,
    val avatarUrl: String? = null,
    /** their page on the service, opened when the name on the playlist page is tapped */
    val profileUrl: String? = null,
)
