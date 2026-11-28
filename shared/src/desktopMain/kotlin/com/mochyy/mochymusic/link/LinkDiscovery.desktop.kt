// LinkDiscovery.desktop.kt

package com.mochyy.mochymusic.link

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// JmDNS on the desktop, the same library Metrolist uses; the service type gains the .local. suffix it wants
actual class LinkAdvert actual constructor(
    platformContext: Any,
    private val type: String,
    name: String,
    port: Int,
    attributes: Map<String, String>,
) {
    private val instances = mutableListOf<JmDNS>()
    private var registered = false

    init {
        val serviceType = jmDnsType(type)
        lanAddresses().forEach { address ->
            runCatching {
                val jmDns = JmDNS.create(address, "Mochy")
                jmDns.registerService(
                    ServiceInfo.create(serviceType, name.take(60), port, 0, 0, attributes),
                )
                instances += jmDns
                registered = true
            }.onFailure { LinkLog.w("couldn't advertise $type (${it.message})") }
        }
    }

    actual fun close() {
        if (!registered) return
        registered = false
        instances.forEach { jmDns ->
            runCatching {
                jmDns.unregisterAllServices()
                jmDns.close()
            }
        }
        instances.clear()
    }
}

// everything of one service type on the wi-fi, as a list that updates while it's collected
actual fun discoverPeers(platformContext: Any, type: String): Flow<List<LinkPeer>> = callbackFlow {
    val serviceType = jmDnsType(type)
    val instances = lanAddresses().mapNotNull { address ->
        runCatching { JmDNS.create(address, "Mochy") }
            .onFailure { LinkLog.w("couldn't listen on ${address.hostAddress} (${it.message})") }
            .getOrNull()
    }
    val found = LinkedHashMap<String, LinkPeer>()
    val listeners = mutableListOf<Pair<JmDNS, ServiceListener>>()

    fun publish() {
        trySend(found.values.toList())
    }

    instances.forEach { jmDns ->
        val listener = object : ServiceListener {
            override fun serviceAdded(event: ServiceEvent) {
                runCatching { jmDns.requestServiceInfo(event.type, event.name) }
            }

            override fun serviceRemoved(event: ServiceEvent) {
                if (found.remove(event.name) != null) publish()
            }

            override fun serviceResolved(event: ServiceEvent) {
                peerOf(event.info)?.let { peer ->
                    found[event.name] = peer
                    publish()
                }
            }
        }
        listeners += jmDns to listener
        runCatching { jmDns.addServiceListener(serviceType, listener) }
            .onFailure { LinkLog.w("discovery of $type failed (${it.message})") }
    }

    publish()
    awaitClose {
        listeners.forEach { (jmDns, listener) ->
            runCatching { jmDns.removeServiceListener(serviceType, listener) }
        }
        instances.forEach { jmDns -> runCatching { jmDns.close() } }
    }
}

private fun peerOf(info: ServiceInfo): LinkPeer? {
    val id = info.getPropertyString(LinkWire.AttrId)?.takeIf { it.isNotBlank() } ?: return null
    val host = info.inet4Addresses.firstOrNull()?.hostAddress
        ?: info.inetAddresses.firstOrNull()?.hostAddress
        ?: return null
    val name = info.getPropertyString(LinkWire.AttrName)?.takeIf { it.isNotBlank() } ?: info.name
    val kind = DeviceKind.fromWire(info.getPropertyString(LinkWire.AttrKind), DeviceKind.Tv)
    return LinkPeer(id, name, host.substringBefore('%'), info.port, kind)
}

private fun jmDnsType(type: String): String = when {
    type.endsWith(".local.") -> type
    type.endsWith(".") -> "${type}local."
    else -> "$type.local."
}

private fun lanAddresses(): List<InetAddress> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback && runCatching { it.supportsMulticast() }.getOrDefault(false) }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
        .distinctBy { it.hostAddress }
}.getOrDefault(emptyList())
