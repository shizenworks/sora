// LinkHttp.kt

package com.mochyy.mochymusic.link

// where a streamed response writes: a socket on android and desktop, a connection on ios
fun interface LinkOutput {
    fun write(buffer: ByteArray, offset: Int, count: Int)
}

// one request as the link server reads it: the path without its query, the query split up, headers in lowercase
class LinkRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: ByteArray,
    val remoteHost: String,
) {
    fun header(name: String): String? = headers[name.lowercase()]

    fun bodyText(): String = body.decodeToString()
}

// what a route answers with: either a small body in memory or a writer that streams a big one
class LinkResponse private constructor(
    val status: Int,
    val contentType: String,
    val headers: Map<String, String>,
    val length: Long,
    val writer: (LinkOutput) -> Unit,
) {
    companion object {
        fun bytes(bytes: ByteArray, contentType: String = "application/octet-stream", status: Int = 200) =
            LinkResponse(status, contentType, emptyMap(), bytes.size.toLong()) { it.write(bytes, 0, bytes.size) }

        fun json(text: String, status: Int = 200) = bytes(text.encodeToByteArray(), "application/json; charset=utf-8", status)

        fun status(status: Int, message: String = "") = bytes(message.encodeToByteArray(), "text/plain; charset=utf-8", status)

        // a body whose size isn't known up front (a zipped database): it runs until the connection closes
        fun stream(contentType: String, writer: (LinkOutput) -> Unit) = LinkResponse(200, contentType, emptyMap(), -1L, writer)

        // a file-like source with seeking, answered whole or as the byte range the player asked for
        fun ranged(
            request: LinkRequest,
            totalLength: Long,
            contentType: String,
            open: (start: Long, length: Long, out: LinkOutput) -> Unit,
        ): LinkResponse {
            val range = parseRange(request.header("range"), totalLength)
                ?: return LinkResponse(200, contentType, mapOf("Accept-Ranges" to "bytes"), totalLength) { open(0, totalLength, it) }
            val (start, end) = range
            if (start >= totalLength || start > end) {
                return LinkResponse(416, contentType, mapOf("Content-Range" to "bytes */$totalLength"), 0) {}
            }
            val length = end - start + 1
            val headers = mapOf("Accept-Ranges" to "bytes", "Content-Range" to "bytes $start-$end/$totalLength")
            return LinkResponse(206, contentType, headers, length) { open(start, length, it) }
        }

        // "bytes=100-" or "bytes=100-199", the only kinds a media player sends
        internal fun parseRange(header: String?, total: Long): Pair<Long, Long>? {
            val trimmed = header?.trim() ?: return null
            if (!trimmed.startsWith("bytes=")) return null
            val first = trimmed.removePrefix("bytes=").substringBefore(',')
            val startText = first.substringBefore('-').trim()
            val endText = first.substringAfter('-', "").trim()
            if (startText.isEmpty()) {
                // "bytes=-500" is the last 500 bytes
                val suffix = endText.toLongOrNull() ?: return null
                return (total - suffix).coerceAtLeast(0) to total - 1
            }
            val start = startText.toLongOrNull() ?: return null
            val end = endText.toLongOrNull()?.coerceAtMost(total - 1) ?: (total - 1)
            return start to end
        }
    }
}

// the tiny http/1.1 server Mochy's link uses, one request per connection; each platform hosts it with its own sockets
expect class LocalDeviceServer(name: String, handler: (LinkRequest) -> LinkResponse) {
    val port: Int
    val isRunning: Boolean

    // binds the preferred port when it's free, otherwise any port; returns the listening port
    fun start(preferredPort: Int): Int

    fun stop()
}

typealias LinkHttpServer = LocalDeviceServer
