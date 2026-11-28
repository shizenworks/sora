// LocalDeviceServer.ios.kt

package com.mochyy.mochymusic.link

// A Network.framework NWListener host is the next piece here; until then iOS takes part as a client
// and controller only, the same limit Metrolist's iOS LocalDeviceServer has.
actual class LocalDeviceServer actual constructor(
    private val name: String,
    private val handler: (LinkRequest) -> LinkResponse,
) {
    actual val port: Int get() = -1
    actual val isRunning: Boolean get() = false

    actual fun start(preferredPort: Int): Int = -1

    actual fun stop() = Unit
}
