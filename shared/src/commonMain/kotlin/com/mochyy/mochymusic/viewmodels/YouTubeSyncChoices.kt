package com.mochyy.mochymusic.viewmodels

/** What the "Import & sync" options page sets. */
data class YouTubeSyncChoices(
    val syncOnLaunch: Boolean = true,
    val newPlaylistsToYouTube: Boolean = true,
    val reportPlays: Boolean = true,
    val copyHistory: Boolean = true,
)
