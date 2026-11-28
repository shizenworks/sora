package com.mochyy.mochymusic.db.entities

import com.mochyy.mochymusic.models.MediaMetadata

// SongEdit lives in shared/; MediaMetadata is still app-only.

// the queue's copy of the song with these edits laid over it
fun SongEdit.applyTo(metadata: MediaMetadata): MediaMetadata {
    // a local copy: smart casts don't reach a property declared in another module
    val album = album
    return metadata.copy(
    title = title ?: metadata.title,
    artists = artistList?.map { MediaMetadata.Artist(it.id, it.name) } ?: metadata.artists,
    // only renames an album the song already links to, a made up album id would open an empty page
    album = if (album != null && metadata.album != null) metadata.album.copy(title = album) else metadata.album,
    thumbnailUrl = cover ?: metadata.thumbnailUrl,
    )
}
