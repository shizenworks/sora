@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

// LinkDiscovery.ios.kt

package com.mochyy.mochymusic.link

import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSData
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.Foundation.create
import platform.darwin.NSObject
import platform.posix.memcpy

// Bonjour through NSNetService, the iOS side of the service types the android NsdManager advertises
actual class LinkAdvert actual constructor(
    platformContext: Any,
    type: String,
    name: String,
    port: Int,
    attributes: Map<String, String>,
) {
    private val service = NSNetService(domain = "local.", type = type, name = name.take(60), port = port)

    init {
        runCatching {
            service.setTXTRecordData(txtRecord(attributes))
            service.publish()
        }.onFailure { LinkLog.w("couldn't advertise $type (${it.message})") }
    }

    actual fun close() {
        service.stop()
    }
}

// everything of one service type on the wi-fi, as a list that updates while it's collected
actual fun discoverPeers(platformContext: Any, type: String): Flow<List<LinkPeer>> = callbackFlow {
    val browser = NSNetServiceBrowser()
    val found = LinkedHashMap<String, LinkPeer>()
    val services = mutableMapOf<String, NSNetService>()
    val resolvers = mutableMapOf<String, NSObject>()

    fun publish() {
        trySend(found.values.toList())
    }

    val delegate = object : NSObject(), NSNetServiceBrowserDelegateProtocol {
        @ObjCSignatureOverride
        override fun netServiceBrowser(browser: NSNetServiceBrowser, didFindService: NSNetService, moreComing: Boolean) {
            val service = didFindService
            services[service.name] = service
            val resolver = object : NSObject(), NSNetServiceDelegateProtocol {
                override fun netServiceDidResolveAddress(sender: NSNetService) {
                    peerOf(sender)?.let { peer ->
                        found[sender.name] = peer
                        publish()
                    }
                    resolvers.remove(sender.name)
                }
            }
            resolvers[service.name] = resolver
            service.delegate = resolver
            service.resolveWithTimeout(5.0)
        }

        @ObjCSignatureOverride
        override fun netServiceBrowser(browser: NSNetServiceBrowser, didRemoveService: NSNetService, moreComing: Boolean) {
            services.remove(didRemoveService.name)
            resolvers.remove(didRemoveService.name)
            if (found.remove(didRemoveService.name) != null) publish()
        }
    }
    browser.delegate = delegate
    browser.searchForServicesOfType(type, inDomain = "local.")
    publish()
    awaitClose {
        browser.stop()
        services.values.forEach { service -> runCatching { service.stop() } }
        // keep the browser's delegate referenced until browsing really stops
        delegate.hashCode()
    }
}

private fun peerOf(service: NSNetService): LinkPeer? {
    val id = service.attribute(LinkWire.AttrId)?.takeIf { it.isNotBlank() } ?: return null
    val host = service.hostName?.removeSuffix(".") ?: return null
    val name = service.attribute(LinkWire.AttrName)?.takeIf { it.isNotBlank() } ?: service.name
    val kind = DeviceKind.fromWire(service.attribute(LinkWire.AttrKind), DeviceKind.Tv)
    return LinkPeer(id, name, host, service.port.toInt(), kind)
}

// the TXT record is length-prefixed "key=value" entries, exactly what NsdManager's attributes become
private fun NSNetService.attribute(key: String): String? {
    val data = TXTRecordData() ?: return null
    val length = data.length.toInt()
    if (length <= 0) return null
    val bytes = ByteArray(length)
    bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
    var index = 0
    while (index < length) {
        val size = bytes[index].toInt() and 0xff
        if (size == 0 || index + 1 + size > length) break
        val entry = bytes.copyOfRange(index + 1, index + 1 + size).decodeToString()
        index += 1 + size
        val equals = entry.indexOf('=')
        if (equals > 0 && entry.substring(0, equals) == key) return entry.substring(equals + 1)
    }
    return null
}

private fun txtRecord(attributes: Map<String, String>): NSData? {
    val entries = attributes.mapNotNull { (key, value) ->
        val entry = "$key=$value".encodeToByteArray()
        if (entry.size > 255) null else entry
    }
    if (entries.isEmpty()) return null
    val size = entries.sumOf { it.size + 1 }
    val bytes = ByteArray(size)
    var index = 0
    entries.forEach { entry ->
        bytes[index] = entry.size.toByte()
        entry.copyInto(bytes, index + 1)
        index += entry.size + 1
    }
    return bytes.usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()) }
}
