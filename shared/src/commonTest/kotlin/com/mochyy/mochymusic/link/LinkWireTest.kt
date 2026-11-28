// LinkWireTest.kt

package com.mochyy.mochymusic.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkWireTest {
    @Test
    fun baseUrlBracketsIpv6() {
        assertEquals("http://192.168.1.5:47801", LinkWire.baseUrl("192.168.1.5", 47801))
        assertEquals("http://[fe80::1]:47801", LinkWire.baseUrl("fe80::1", 47801))
    }

    @Test
    fun signsTheSamePathAndQueryTheServerSees() {
        val url = LinkUrl.parse("http://192.168.1.5:47801/file/a%20b?d=one%20id&e=2")
        assertEquals("/file/a b", url.path)
        assertEquals(mapOf("d" to "one id", "e" to "2"), url.query)
    }

    @Test
    fun bareHostIsTheRootPath() {
        assertEquals(LinkUrl("/", emptyMap()), LinkUrl.parse("http://192.168.1.5:47801"))
    }

    @Test
    fun rangesFollowThePlayer() {
        assertEquals(100L to 199L, LinkResponse.parseRange("bytes=100-199", 1000))
        assertEquals(100L to 999L, LinkResponse.parseRange("bytes=100-", 1000))
        assertEquals(500L to 999L, LinkResponse.parseRange("bytes=-500", 1000))
        assertEquals(1000L to 999L, LinkResponse.parseRange("bytes=1000-", 1000))
        assertNull(LinkResponse.parseRange("items=0-1", 1000))
    }
}
