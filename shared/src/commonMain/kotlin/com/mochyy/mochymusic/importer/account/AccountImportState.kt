package com.mochyy.mochymusic.importer.account

sealed interface AccountImportState {
    /** The service's sign-in page is showing. */
    data object SigningIn : AccountImportState
    /** YouTube Music uses the app's account, which isn't signed in yet. */
    data object NeedsYouTubeSignIn : AccountImportState
    data class Loading(val label: String) : AccountImportState
    data class Choosing(
        val accountName: String?,
        val playlists: List<RemotePlaylist>,
        val selected: Set<String>,
        /** A Re-sync that couldn't find this playlist by name: the user picks which one it is. */
        val pickingFor: String? = null,
    ) : AccountImportState
    data class Reading(val label: String, val done: Int, val total: Int) : AccountImportState
    data class Failed(val message: String, val signedOut: Boolean) : AccountImportState
}
