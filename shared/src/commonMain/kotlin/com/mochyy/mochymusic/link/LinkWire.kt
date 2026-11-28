// LinkWire.kt

package com.mochyy.mochymusic.link

import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.cancel

// the names both sides agree on, so a phone and a tv on different app versions still find each other
object LinkWire {
    // a phone keeps this one up while it has any device paired
    const val PhoneServiceType = "_mochylink._tcp."

    // a device only shows up as this while its pairing screen is open, the name is from when only tvs had one
    const val PairServiceType = "_mochytv._tcp."

    const val PhonePort = 47_801
    const val PairPort = 47_802

    // the tv listens here for its phone's play, skip and look commands while a linked profile is open
    const val TvControlPort = 47_803

    // bumped when a route changes shape, so an old phone and a new tv can say so instead of failing quietly; 2 signs every call and pairs without sending the key
    const val ProtocolVersion = 2

    // the header the old protocol carried the key itself in; only looked for, so an un-updated device hears "update" instead of "unpaired"
    const val OldKeyHeader = "X-Mochy-Key"

    const val AttrId = "id"
    const val AttrName = "n"
    const val AttrKind = "k"

    private const val FastReadTimeoutMs = 30_000L
    private const val SlowReadTimeoutMs = 3 * 60_000L

    // a lan is fast and a phone that's gone should be noticed quickly, so short waits
    private val client by lazy { createDeviceConnectHttpClient() }

    // a big library takes the phone a while to copy and squeeze before the first byte goes out
    suspend fun getSlow(url: String, auth: LinkCredentials?): LinkReply = request(url, auth, null, HttpMethod.Get, SlowReadTimeoutMs)

    fun baseUrl(host: String, port: Int) = "http://${if (host.contains(':')) "[$host]" else host}:$port"

    suspend fun get(url: String, auth: LinkCredentials?): LinkReply = request(url, auth, null, HttpMethod.Get, FastReadTimeoutMs)

    suspend fun postJson(url: String, auth: LinkCredentials?, json: String): LinkReply =
        request(url, auth, json.encodeToByteArray(), HttpMethod.Post, FastReadTimeoutMs)

    // a call signed with the key when there is one; the path and query are read back the way the server reads them, so both sign the same thing
    private suspend fun request(url: String, auth: LinkCredentials?, body: ByteArray?, method: HttpMethod, timeoutMs: Long): LinkReply {
        val parts = LinkUrl.parse(url)
        val signature = auth?.let { LinkAuth.header(it, method.value, parts.path, parts.query, body) }
        val response = client.request(url) {
            this.method = method
            signature?.let { header(LinkAuth.Header, it) }
            timeout { requestTimeoutMillis = timeoutMs }
            if (body != null) {
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                setBody(body)
            }
        }
        return LinkReply(response)
    }
}

// one answer from a paired device; the body is read only when asked for, so a library download stays a stream
class LinkReply internal constructor(private val response: HttpResponse) {
    val code: Int = response.status.value

    private var consumed = false

    val isSuccessful: Boolean get() = code in 200..299

    suspend fun text(): String {
        consumed = true
        return response.bodyAsText()
    }

    suspend fun bytes(): ByteArray {
        consumed = true
        return response.bodyAsBytes()
    }

    suspend fun channel(): ByteReadChannel {
        consumed = true
        return response.bodyAsChannel()
    }

    // for a call whose body nobody reads, so its connection isn't kept waiting
    fun close() {
        if (!consumed) {
            consumed = true
            // cancelling the response's own job drops the unread body and frees the connection
            response.cancel()
        }
    }
}

// the pieces of a url both sides sign: java's URLDecoder sees the encoded path, and the query is split after
internal data class LinkUrl(val path: String, val query: Map<String, String>) {
    companion object {
        fun parse(url: String): LinkUrl {
            val rest = url.substringAfter("://", url)
            val slash = rest.indexOf('/')
            val pathAndQuery = if (slash < 0) "/" else rest.substring(slash)
            val encodedPath = pathAndQuery.substringBefore('?')
            val encodedQuery = if ('?' in pathAndQuery) pathAndQuery.substringAfter('?') else null
            return LinkUrl(urlDecode(encodedPath.ifEmpty { "/" }), LinkAuth.parseQuery(encodedQuery))
        }
    }
}
