// LocalDeviceServer.desktop.kt

package com.mochyy.mochymusic.link

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

// the same tiny http/1.1 server android hosts, on the desktop jvm's sockets
actual class LocalDeviceServer actual constructor(
    private val name: String,
    private val handler: (LinkRequest) -> LinkResponse,
) {
    @Volatile private var socket: ServerSocket? = null
    private var workers: ExecutorService? = null

    actual val port: Int get() = socket?.localPort ?: -1
    actual val isRunning: Boolean get() = socket?.isClosed == false

    @Synchronized
    actual fun start(preferredPort: Int): Int {
        socket?.takeIf { !it.isClosed }?.let { return it.localPort }
        val server = runCatching { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(preferredPort)) } }
            .getOrElse { ServerSocket(0) }
        socket = server
        val pool = Executors.newFixedThreadPool(MaxConnections) { runnable -> Thread(runnable, "$name-conn").apply { isDaemon = true } }
        workers = pool
        Thread({ acceptLoop(server, pool) }, "$name-accept").apply { isDaemon = true }.start()
        LinkLog.i("$name listening on ${server.localPort}")
        return server.localPort
    }

    @Synchronized
    actual fun stop() {
        runCatching { socket?.close() }
        socket = null
        workers?.shutdownNow()
        workers = null
    }

    private fun acceptLoop(server: ServerSocket, pool: ExecutorService) {
        while (!server.isClosed) {
            val client = try {
                server.accept()
            } catch (_: SocketException) {
                break
            } catch (e: Exception) {
                LinkLog.w("$name accept failed", e)
                continue
            }
            try {
                pool.execute { serve(client) }
            } catch (_: RejectedExecutionException) {
                runCatching { client.close() }
            }
        }
    }

    private fun serve(client: Socket) {
        client.use { connection ->
            runCatching {
                connection.soTimeout = ReadTimeoutMs
                val input = BufferedInputStream(connection.getInputStream())
                val request = readRequest(input, connection.inetAddress?.hostAddress.orEmpty()) ?: return
                val response = runCatching { handler(request) }.getOrElse {
                    LinkLog.w("$name route ${request.path} failed", it)
                    LinkResponse.status(500, it.message.orEmpty())
                }
                val out = connection.getOutputStream().buffered(64 * 1024)
                writeHead(out, response)
                if (request.method != "HEAD") {
                    response.writer(LinkOutput { buffer, offset, count -> out.write(buffer, offset, count) })
                }
                out.flush()
            }.onFailure {
                if (it !is SocketException) LinkLog.w("$name connection ended", it)
            }
        }
    }

    private fun readRequest(input: InputStream, remoteHost: String): LinkRequest? {
        val requestLine = readLine(input) ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 2) return null
        val headers = HashMap<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val colon = line.indexOf(':')
            if (colon > 0) headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
        }
        val length = headers["content-length"]?.toIntOrNull()?.coerceIn(0, MaxBodyBytes) ?: 0
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val got = input.read(body, read, length - read)
            if (got < 0) break
            read += got
        }
        val target = parts[1]
        val path = URLDecoder.decode(target.substringBefore('?'), "UTF-8")
        val query = LinkAuth.parseQuery(target.substringAfter('?', ""))
        return LinkRequest(parts[0].uppercase(), path, query, headers, body, remoteHost)
    }

    private fun readLine(input: InputStream): String? {
        val line = StringBuilder()
        while (true) {
            val byte = input.read()
            if (byte < 0) return if (line.isEmpty()) null else line.toString()
            if (byte == '\n'.code) return line.toString().trimEnd('\r')
            if (line.length > MaxHeaderLine) return null
            line.append(byte.toChar())
        }
    }

    private fun writeHead(out: OutputStream, response: LinkResponse) {
        val head = StringBuilder()
            .append("HTTP/1.1 ").append(response.status).append(' ').append(reason(response.status)).append("\r\n")
            .append("Content-Type: ").append(response.contentType).append("\r\n")
            .append("Connection: close\r\n")
        if (response.length >= 0) head.append("Content-Length: ").append(response.length).append("\r\n")
        response.headers.forEach { (name, value) -> head.append(name).append(": ").append(value).append("\r\n") }
        head.append("\r\n")
        out.write(head.toString().toByteArray(Charsets.US_ASCII))
    }

    private fun reason(status: Int) = when (status) {
        200 -> "OK"
        206 -> "Partial Content"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        409 -> "Conflict"
        416 -> "Range Not Satisfiable"
        426 -> "Upgrade Required"
        else -> "Error"
    }

    private companion object {
        const val MaxConnections = 6
        const val ReadTimeoutMs = 15_000
        const val MaxBodyBytes = 8 * 1024 * 1024
        const val MaxHeaderLine = 16 * 1024
    }
}
