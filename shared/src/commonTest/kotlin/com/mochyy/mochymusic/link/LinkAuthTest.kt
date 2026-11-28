// LinkAuthTest.kt

package com.mochyy.mochymusic.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// fixed vectors so a change to the canonical form or the hmac is caught before an old phone stops pairing
class LinkAuthTest {
    private val credentials = LinkCredentials("phone-1", "test-key-0123456789")

    @Test
    fun headerMatchesTheAndroidBytes() {
        val header = LinkAuth.headerAt(
            credentials = credentials,
            method = "GET",
            path = "/hello",
            query = mapOf("port" to "47801", "kind" to "phone"),
            body = null,
            time = 1_700_000_000_000,
            nonce = "00112233445566778899aabb",
        )
        assertEquals("v2 phone-1 1700000000000 00112233445566778899aabb c2d038f160b3e68a841d87342b238b16f5c9d067580fc1ea6b55c81d392608b2", header)
    }

    @Test
    fun signedQueryMatchesTheAndroidBytes() {
        val query = LinkAuth.signedQuery(credentials, "/file/dQw4w9WgXcQ", 1_700_000_000_000)
        assertEquals("d=phone-1&e=1700000000000&s=f72e4a2d7c04e92beb83b0c776e25a9ff1c2b6fdf285ac9c07c17cf8520e644f", query)
    }

    @Test
    fun verifierAcceptsOnceAndRefusesAReplay() {
        val time = 1_700_000_000_000
        val header = LinkAuth.headerAt(credentials, "GET", "/hello", mapOf("port" to "47801"), null, time, "aabbccddeeff001122334455")
        val request = LinkRequest(
            method = "GET",
            path = "/hello",
            query = mapOf("port" to "47801"),
            headers = mapOf("x-mochy-auth" to header),
            body = ByteArray(0),
            remoteHost = "192.168.1.20",
        )
        val verifier = LinkAuth.Verifier()
        assertEquals("phone-1", verifier.verify(request, { if (it == "phone-1") credentials.key else null }, now = time))
        assertNull(verifier.verify(request, { credentials.key }, now = time))
        assertNull(verifier.verify(request, { credentials.key }, now = time + 11 * 60_000))
    }

    @Test
    fun oldKeyHeaderIsNotAValidSignature() {
        val request = LinkRequest(
            method = "GET",
            path = "/hello",
            query = emptyMap(),
            headers = mapOf("x-mochy-key" to "old-key"),
            body = ByteArray(0),
            remoteHost = "192.168.1.20",
        )
        assertNull(LinkAuth.Verifier().verify(request, { credentials.key }, now = 1_700_000_000_000))
    }
}
