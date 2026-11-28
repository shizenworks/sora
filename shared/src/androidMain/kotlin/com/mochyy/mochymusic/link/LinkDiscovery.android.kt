// LinkDiscovery.android.kt

package com.mochyy.mochymusic.link

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import java.net.Inet4Address
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// puts this device on the wi-fi under a service type until close is called
actual class LinkAdvert actual constructor(
    platformContext: Any,
    type: String,
    name: String,
    port: Int,
    attributes: Map<String, String>,
) {
    private val nsd = (platformContext as Context).getSystemService(Context.NSD_SERVICE) as NsdManager
    private val listener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(info: NsdServiceInfo) = Unit
        override fun onRegistrationFailed(info: NsdServiceInfo, error: Int) {
            LinkLog.w("couldn't advertise $type ($error)")
        }
        override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
        override fun onUnregistrationFailed(info: NsdServiceInfo, error: Int) = Unit
    }
    private var registered = false

    init {
        val info = NsdServiceInfo().apply {
            serviceName = name.take(60)
            serviceType = type
            setPort(port)
            attributes.forEach { (key, value) -> setAttribute(key, value) }
        }
        registered = runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }.isSuccess
    }

    actual fun close() {
        if (!registered) return
        registered = false
        runCatching { nsd.unregisterService(listener) }
    }
}

// everything of one service type on the wi-fi, as a list that updates while it's collected
actual fun discoverPeers(platformContext: Any, type: String): Flow<List<LinkPeer>> = callbackFlow {
    val context = platformContext as Context
    val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    val found = LinkedHashMap<String, LinkPeer>()
    // nsd only resolves one service at a time on older androids, so they wait their turn
    val waiting = ArrayDeque<NsdServiceInfo>()
    val resolving = AtomicBoolean(false)

    fun publish() {
        trySend(found.values.toList())
    }

    fun resolveNext() {
        if (!resolving.compareAndSet(false, true)) return
        val next = synchronized(waiting) { waiting.pollFirst() } ?: run { resolving.set(false); return }
        @Suppress("DEPRECATION")
        val started = runCatching {
            nsd.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo, error: Int) {
                    resolving.set(false)
                    resolveNext()
                }

                override fun onServiceResolved(info: NsdServiceInfo) {
                    val host = hostOf(info)
                    val attrs = info.attributes.orEmpty()
                    val id = attrs[LinkWire.AttrId]?.toString(Charsets.UTF_8)
                    if (host != null && id != null) {
                        val name = attrs[LinkWire.AttrName]?.toString(Charsets.UTF_8) ?: info.serviceName
                        val kind = DeviceKind.fromWire(attrs[LinkWire.AttrKind]?.toString(Charsets.UTF_8), DeviceKind.Tv)
                        found[info.serviceName] = LinkPeer(id, name, host, info.port, kind)
                        publish()
                    }
                    resolving.set(false)
                    resolveNext()
                }
            })
        }.isSuccess
        if (!started) resolving.set(false)
    }

    val listener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) = Unit
        override fun onDiscoveryStopped(serviceType: String) = Unit
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            LinkLog.w("discovery of $type failed ($errorCode)")
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit

        override fun onServiceFound(info: NsdServiceInfo) {
            synchronized(waiting) { waiting.addLast(info) }
            resolveNext()
        }

        override fun onServiceLost(info: NsdServiceInfo) {
            if (found.remove(info.serviceName) != null) publish()
        }
    }

    publish()
    val started = runCatching { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener) }.isSuccess
    awaitClose { if (started) runCatching { nsd.stopServiceDiscovery(listener) } }
}

private fun hostOf(info: NsdServiceInfo): String? {
    val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        info.hostAddresses.firstOrNull { it is Inet4Address } ?: info.hostAddresses.firstOrNull()
    } else {
        @Suppress("DEPRECATION")
        info.host
    }
    return address?.hostAddress?.substringBefore('%')
}
