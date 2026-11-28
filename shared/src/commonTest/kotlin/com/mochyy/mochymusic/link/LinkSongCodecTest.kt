// LinkSongCodecTest.kt

package com.mochyy.mochymusic.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// the json text below is what the android app's org.json has always put on the wire for this song
class LinkSongCodecTest {
    private val song = LinkSong(
        id = "dQw4w9WgXcQ",
        title = "Never Gonna Give You Up",
        artists = listOf(LinkSongArtist("UCuAXFkgsw1L7xaCfnd5JJOw", "Rick Astley")),
        duration = 213,
        thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
        album = LinkSongAlbum("MPREb_4pL8gzRtw1p", "Whenever You Need Somebody"),
        explicit = false,
    )

    private val fixed = """{"id":"dQw4w9WgXcQ","title":"Never Gonna Give You Up","duration":213,"thumbnailUrl":"https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg","explicit":false,"artists":[{"id":"UCuAXFkgsw1L7xaCfnd5JJOw","name":"Rick Astley"}],"album":{"id":"MPREb_4pL8gzRtw1p","title":"Whenever You Need Somebody"}}"""

    @Test
    fun encodesToTheFixedAndroidText() {
        assertEquals(fixed, LinkSongCodec.encode(song))
    }

    @Test
    fun decodesTheFixedAndroidText() {
        assertEquals(song, LinkSongCodec.decode(fixed))
    }

    @Test
    fun roundTripsAQueue() {
        val list = listOf(song, song.copy(id = "second", explicit = true, album = null, thumbnailUrl = null))
        assertEquals(list, LinkSongCodec.decodeList(LinkSongCodec.encodeList(list)))
    }

    @Test
    fun blankIdIsNotASong() {
        assertNull(LinkSongCodec.decode("""{"id":"","title":"nothing"}"""))
    }
}
