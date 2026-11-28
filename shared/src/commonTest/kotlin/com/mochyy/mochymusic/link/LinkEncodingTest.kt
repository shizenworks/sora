// LinkEncodingTest.kt

package com.mochyy.mochymusic.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkEncodingTest {
    @Test
    fun base64MatchesTheAndroidNoWrapOutput() {
        assertEquals("bW9jaHk=", base64Encode("mochy".encodeToByteArray()))
        assertEquals("mochy", base64Decode("bW9jaHk=")?.decodeToString())
        assertEquals("", base64Encode(ByteArray(0)))
    }

    @Test
    fun hexIsLowercaseAndPadded() {
        assertEquals("0001feff", hex(byteArrayOf(0, 1, -2, -1)))
    }

    @Test
    fun urlEncodingMatchesJava() {
        assertEquals("a+b%2Fc%7E_-.", urlEncode("a b/c~_-."))
        assertEquals("a b/c~_-.", urlDecode("a+b%2Fc%7E_-."))
        assertEquals("Mochy 1", urlDecode("Mochy%201"))
    }

    @Test
    fun constantTimeEqualityStillCompares() {
        assertTrue(constantTimeEquals("abcdef", "abcdef"))
        assertFalse(constantTimeEquals("abcdef", "abcde"))
        assertFalse(constantTimeEquals("abcdef", "abcdeg"))
    }
}
