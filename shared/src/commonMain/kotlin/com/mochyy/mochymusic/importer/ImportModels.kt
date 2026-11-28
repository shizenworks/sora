// ImportModels.kt

package com.mochyy.mochymusic.importer

import com.mochyy.mochymusic.importer.account.AccountService
import com.music.innertube.models.SongItem

/** one re-sync run: the playlist being read now, and what the run has done so far */
data class ResyncSummary(
    val playlistName: String,
    /** how many playlists the run re-syncs (re-sync all), and how many are finished */
    val playlists: Int = 1,
    val done: Int = 0,
    /** new songs added over every playlist so far, and how many playlists got any */
    val added: Int = 0,
    val playlistsChanged: Int = 0,
    /** playlists that couldn't be read or added to */
    val failed: List<String> = emptyList(),
    /** something to know about how one was read, like a link that only shares part of the playlist */
    val note: String? = null,
)

/** a re-sync's read of a playlist: its songs on the service now, and where it was found */
data class ResyncRead(
    val tracks: List<ImportedTrack>,
    val origin: PlaylistOrigin,
    val note: String? = null,
)

/** one playlist for MusicImportService to re-sync, read runs inside the re-sync so the wait for the service already shows as progress */
class ResyncRequest(
    val playlistId: String,
    val playlistName: String,
    val source: ImportSource,
    val read: suspend () -> ResyncRead,
)

data class ImportOptions(
    /** also create the playlists (and likes) on the signed-in youtube account */
    val mirrorToYouTube: Boolean = false,
)

/** a track that couldn't be matched, with where it came from so the user can find it by hand */
data class UnmatchedTrack(
    val track: ImportedTrack,
    val playlistName: String?,
)

data class ImportProgress(
    val isRunning: Boolean = false,
    val totalTracks: Int = 0,
    val processedTracks: Int = 0,
    val matchedTracks: Int = 0,
    val currentLabel: String = "",
    val isDone: Boolean = false,
    val cancelled: Boolean = false,
    /** the service being imported from, for the "adding 12 of 340 tracks from spotify" banner */
    val source: ImportSource? = null,
    val playlistsCreated: Int = 0,
    val likedAdded: Int = 0,
    val unmatched: List<UnmatchedTrack> = emptyList(),
    /** playlists that imported locally but couldn't be created or filled on youtube */
    val youtubeFailures: List<String> = emptyList(),
    val error: String? = null,
    /** set while this run is a re-sync of one playlist rather than an import */
    val resync: ResyncSummary? = null,
)

/** how a re-sync gets going, for the menu that asked for it */
sealed interface ResyncStart {
    /** reading in the background now; a toast says what changed */
    data object Started : ResyncStart

    /** only the signed-in account can read it: open the service's sign-in page in re-sync mode */
    data class ThroughAccount(val service: AccountService) : ResyncStart

    /** nothing says where it lives, so the user pastes its link */
    data object NeedsLink : ResyncStart

    data class Failed(val message: String) : ResyncStart
}

/** how far re-sync all got on its own. busy: an import was running, so nothing started */
data class ResyncAllStart(
    val started: Int,
    val throughAccount: List<AccountService>,
    val needLink: Int,
    val busy: Boolean = false,
)

/** the imported playlist a Re-sync reads again: its id here, its name, and its id on the service if saved. */
data class ResyncTarget(val playlistId: String, val name: String, val remoteId: String?)

/** what a Re-sync read after sign-in, by playlist id here, and the names it couldn't find. */
data class ResyncReads(val reads: Map<String, ResyncRead>, val missing: List<String>)

/** the manual youtube actions on the sync page; automatic library sync stays in SyncUtils */
data class YtSyncState(
    val running: Boolean = false,
    val label: String = "",
    val message: String? = null,
    val canUndo: Boolean = false,
)

/** a youtube song needs no matching: the id is exact, and the full item is kept */
fun SongItem.toImportedTrack() = ImportedTrack(
    title = title,
    artist = artists.joinToString(", ") { it.name },
    album = album?.name,
    durationMs = duration?.times(1000L),
    videoId = id,
    thumbnailUrl = thumbnail,
    ytItem = this,
)
