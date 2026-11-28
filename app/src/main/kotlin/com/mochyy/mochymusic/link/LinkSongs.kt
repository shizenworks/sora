// LinkSongs.kt

package com.mochyy.mochymusic.link

import com.mochyy.mochymusic.models.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject

// app glue: the codec itself lives in shared, this keeps org.json callers unchanged
internal object LinkSongs {
    fun toJson(song: MediaMetadata): JSONObject = JSONObject(LinkSongCodec.encode(song.toLinkSong()))

    fun fromJson(json: JSONObject): MediaMetadata? = LinkSongCodec.decode(json.toString())?.toMediaMetadata()

    fun listToJson(songs: List<MediaMetadata>) = JSONArray(LinkSongCodec.encodeList(songs.map { it.toLinkSong() }))

    fun listFromJson(array: JSONArray?): List<MediaMetadata> =
        LinkSongCodec.decodeList(array?.toString()).map { it.toMediaMetadata() }
}

private fun MediaMetadata.toLinkSong() = LinkSong(
    id = id,
    title = title,
    artists = artists.map { LinkSongArtist(it.id, it.name) },
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    album = album?.let { LinkSongAlbum(it.id, it.title) },
    explicit = explicit,
)

private fun LinkSong.toMediaMetadata() = MediaMetadata(
    id = id,
    title = title,
    artists = artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    album = album?.let { MediaMetadata.Album(id = it.id, title = it.title) },
    explicit = explicit,
)
