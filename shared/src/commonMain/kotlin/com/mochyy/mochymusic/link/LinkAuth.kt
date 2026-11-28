// LinkAuth.kt

package com.mochyy.mochymusic.link

import kotlin.math.abs
import kotlin.time.Clock

// a device's own id and the key it shares with the one it's talking to; the key signs each call and never goes over the wi-fi itself
class LinkCredentials(val id: String, val key: String)

// how paired devices prove a call is theirs: an hmac over the call with the time and a nonce, so a copied call is no good to anyone listening on the wi-fi
object LinkAuth {
    const val Header = "X-Mochy-Auth"
    private const val Scheme = "v2"
    private const val MacName = "HmacSHA256"

    // a tv's clock and a phone's can drift a little; past this the call is turned away
    private const val WindowMs = 10 * 60_000L

    // a song link the player opens stays good this long, longer than any song or download takes
    const val LinkGoodForMs = 12 * 60 * 60_000L

    // the signature a player's link carries in its query, since a player sends no headers of ours
    private const val SignedId = "d"
    private const val Expires = "e"
    private const val Signature = "s"
    private val LinkFields = setOf(SignedId, Expires, Signature)

    private val NoBody = ByteArray(0)

    private fun now() = Clock.System.now().toEpochMilliseconds()

    // the header for one call, made fresh each time
    fun header(credentials: LinkCredentials, method: String, path: String, query: Map<String, String>, body: ByteArray?): String =
        headerAt(credentials, method, path, query, body, now(), hex(DeviceCrypto.secureRandomBytes(12)))

    // the fixed-time form, so a test can check the exact bytes an older build produces
    internal fun headerAt(
        credentials: LinkCredentials,
        method: String,
        path: String,
        query: Map<String, String>,
        body: ByteArray?,
        time: Long,
        nonce: String,
    ): String {
        val mac = sign(credentials.key, canonical(method, path, query, time, nonce, body ?: NoBody))
        return "$Scheme ${credentials.id} $time $nonce $mac"
    }

    // the query for a link the player can open with no header; only the path is signed, so a range or a head request is fine
    fun signedQuery(credentials: LinkCredentials, path: String, expiresAt: Long): String {
        val mac = sign(credentials.key, canonical("GET", path, emptyMap(), expiresAt, "", NoBody))
        return "$SignedId=${urlEncode(credentials.id)}&$Expires=$expiresAt&$Signature=$mac"
    }

    // a query string split the one way both sides agree on, so what's signed is what's checked
    fun parseQuery(encoded: String?): Map<String, String> =
        encoded.orEmpty().split('&').filter { it.isNotEmpty() }.associate { pair ->
            urlDecode(pair.substringBefore('=')) to urlDecode(pair.substringAfter('=', ""))
        }

    // one per server: remembers the nonces it has seen, so a call someone copied can't be played again
    class Verifier {
        private val seen = LinkedHashMap<String, Long>()
        private val lock = PlatformLock()

        // the sender's id when the call checks out against the key [keyOf] gives for that id, null otherwise
        fun verify(request: LinkRequest, keyOf: (id: String) -> String?, now: Long = Clock.System.now().toEpochMilliseconds()): String? = lock.withLock {
            if (request.query.containsKey(Signature)) return@withLock verifyLink(request, keyOf, now)
            val parts = request.header(Header)?.split(' ') ?: return@withLock null
            if (parts.size != 5 || parts[0] != Scheme) return@withLock null
            val id = parts[1]
            val time = parts[2].toLongOrNull() ?: return@withLock null
            val nonce = parts[3]
            if (abs(now - time) > WindowMs) return@withLock null
            val key = keyOf(id) ?: return@withLock null
            val expected = sign(key, canonical(request.method, request.path, request.query, time, nonce, request.body))
            if (!constantTimeEquals(expected, parts[4])) return@withLock null
            // the same call twice is a copy, the first one already ran
            if (!remember("$id:$nonce", time)) return@withLock null
            id
        }

        private fun verifyLink(request: LinkRequest, keyOf: (id: String) -> String?, now: Long): String? {
            val id = request.query[SignedId] ?: return null
            val expiresAt = request.query[Expires]?.toLongOrNull() ?: return null
            if (expiresAt < now) return null
            val key = keyOf(id) ?: return null
            val expected = sign(key, canonical("GET", request.path, emptyMap(), expiresAt, "", NoBody))
            return if (constantTimeEquals(expected, request.query[Signature].orEmpty())) id else null
        }

        // true the first time a (id, nonce) pair is seen
        private fun remember(key: String, time: Long): Boolean {
            if (seen.put(key, time) != null) return false
            while (seen.size > 2048) {
                val oldest = seen.keys.iterator()
                if (!oldest.hasNext()) break
                oldest.next()
                oldest.remove()
            }
            return true
        }
    }

    internal fun canonical(method: String, path: String, query: Map<String, String>, time: Long, nonce: String, body: ByteArray): String {
        val fields = query.filterKeys { it !in LinkFields }.entries.sortedBy { it.key }.joinToString("&") { "${it.key}=${it.value}" }
        return listOf(method.uppercase(), path, fields, time.toString(), nonce, hex(DeviceCrypto.sha256(body))).joinToString("\n")
    }

    internal fun sign(key: String, text: String): String =
        hex(DeviceCrypto.hmacSha256(key.encodeToByteArray(), text.encodeToByteArray()))

    // pairing: each side makes a key pair and sends only its public half; both then work out the same key from the two halves and the code on the screen
    fun newKeyPair(): DeviceKeyPair = DeviceCrypto.newKeyPair()

    fun publicText(pair: DeviceKeyPair): String = base64Encode(DeviceCrypto.publicBytes(pair))

    // null when the other side's public half doesn't parse; someone who only listened in has neither private half, and someone without the code has the wrong key
    fun sharedKey(pair: DeviceKeyPair, otherPublic: String, code: String, ids: List<String>): String? = runCatching {
        val other = base64Decode(otherPublic) ?: return null
        val secret = DeviceCrypto.sharedSecret(pair, other) ?: return null
        // the same digest android's incremental MessageDigest builds, byte for byte
        val mixed = "mochy-link-v2".encodeToByteArray() + secret + code.trim().encodeToByteArray() +
            ids.sorted().joinToString("").encodeToByteArray()
        hex(DeviceCrypto.sha256(mixed))
    }.getOrNull()
}
