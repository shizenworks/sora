// DeviceKind.desktop.kt

package com.mochyy.mochymusic.link

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

// the linked device on the other end is a tv or phone; this build is always the desktop app
actual fun deviceKindOf(platformContext: Any): DeviceKind = DeviceKind.Desktop

actual fun getDefaultDeviceName(): String =
    runCatching { InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Desktop"

actual fun getLocalIpV4Addresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
        .mapNotNull { it.hostAddress }
}.getOrDefault(emptyList())
