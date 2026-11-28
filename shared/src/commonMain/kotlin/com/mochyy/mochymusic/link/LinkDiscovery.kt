// LinkDiscovery.kt

package com.mochyy.mochymusic.link

import kotlinx.coroutines.flow.Flow

// a device found on the wi-fi, with where to reach it; kind is a tv when the device didn't say
data class LinkPeer(val id: String, val name: String, val host: String, val port: Int, val kind: DeviceKind = DeviceKind.Tv)

// puts this device on the wi-fi under a service type until close is called
expect class LinkAdvert(platformContext: Any, type: String, name: String, port: Int, attributes: Map<String, String>) {
    fun close()
}

// everything of one service type on the wi-fi, as a list that updates while it's collected
expect fun discoverPeers(platformContext: Any, type: String): Flow<List<LinkPeer>>

// while a pairing screen is open, the device waits on the wi-fi for someone who types the code it shows
data class PairingCode(val code: String, val deviceName: String, val address: String? = null)
