// LocalDeviceServerTest.kt

package com.mochyy.mochymusic.link

import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertTrue

// the desktop actual really speaks the same one-request http the android server does
class LocalDeviceServerTest {
    @Test
    fun answersARequestAndCloses() {
        val server = LocalDeviceServer("test") { request ->
            if (request.path == "/hello" && request.query["x"] == "1") LinkResponse.json("""{"ok":true}""") else LinkResponse.status(404)
        }
        val port = server.start(0)
        try {
            Socket("127.0.0.1", port).use { socket ->
                socket.soTimeout = 5_000
                socket.getOutputStream().apply {
                    write("GET /hello?x=1 HTTP/1.1\r\nHost: localhost\r\n\r\n".encodeToByteArray())
                    flush()
                }
                val text = socket.getInputStream().readBytes().decodeToString()
                assertTrue(text.startsWith("HTTP/1.1 200 OK"), text)
                assertTrue(text.endsWith("""{"ok":true}"""), text)
            }
        } finally {
            server.stop()
        }
    }
}
