// LinkSong.kt

package com.mochyy.mochymusic.link

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

// a song as one device hands it to another: enough to show it and to find it on youtube, no local paths
data class LinkSong(
    val id: String,
    val title: String,
    val artists: List<LinkSongArtist> = emptyList(),
    val duration: Int = -1,
    val thumbnailUrl: String? = null,
    val album: LinkSongAlbum? = null,
    val explicit: Boolean = false,
)

data class LinkSongArtist(val id: String?, val name: String)

data class LinkSongAlbum(val id: String, val title: String)

// the exact json android's org.json has always sent on /play, /next, /queue, /now and /changes
object LinkSongCodec {
    fun encode(song: LinkSong): String = toJson(song).toString()

    fun encodeList(songs: List<LinkSong>): String = JsonArray(songs.map { toJson(it) }).toString()

    fun decode(text: String): LinkSong? {
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
        return fromJson(root)
    }

    fun decodeList(text: String?): List<LinkSong> {
        val array = runCatching { text?.let { Json.parseToJsonElement(it) as? JsonArray } }.getOrNull() ?: return emptyList()
        return array.mapNotNull { fromJson(it as? JsonObject ?: return@mapNotNull null) }
    }

    private fun toJson(song: LinkSong): JsonObject = buildJsonObject {
        put("id", song.id)
        put("title", song.title)
        put("duration", song.duration)
        put("thumbnailUrl", song.thumbnailUrl)
        put("explicit", song.explicit)
        put(
            "artists",
            buildJsonArray {
                song.artists.forEach { artist ->
                    add(
                        buildJsonObject {
                            put("id", artist.id?.let { JsonPrimitive(it) } ?: JsonNull)
                            put("name", artist.name)
                        },
                    )
                }
            },
        )
        song.album?.let { album ->
            put(
                "album",
                buildJsonObject {
                    put("id", album.id)
                    put("title", album.title)
                },
            )
        }
    }

    private fun fromJson(json: JsonObject): LinkSong? {
        val id = json.text("id")?.takeIf { it.isNotBlank() } ?: return null
        val artistArray = json["artists"] as? JsonArray
        val artists = (0 until (artistArray?.size ?: 0)).mapNotNull { index ->
            val artist = artistArray?.get(index) as? JsonObject ?: return@mapNotNull null
            LinkSongArtist(
                id = artist.text("id")?.takeIf { it.isNotBlank() && it != "null" },
                name = artist.text("name").orEmpty(),
            )
        }
        return LinkSong(
            id = id,
            title = json.text("title").orEmpty(),
            artists = artists,
            duration = (json["duration"] as? JsonPrimitive)?.intOrNull ?: -1,
            thumbnailUrl = json.text("thumbnailUrl")?.takeIf { it.isNotBlank() && it != "null" },
            album = (json["album"] as? JsonObject)?.let { album ->
                LinkSongAlbum(album.text("id").orEmpty(), album.text("title").orEmpty())
            },
            explicit = (json["explicit"] as? JsonPrimitive)?.booleanOrNull ?: false,
        )
    }
}

private fun JsonObject.text(key: String): String? {
    val element = this[key] ?: return null
    if (element is JsonNull) return null
    return (element as? JsonPrimitive)?.content
}
