// DeviceKind.ios.kt

package com.mochyy.mochymusic.link

import platform.UIKit.UIDevice

actual fun deviceKindOf(platformContext: Any): DeviceKind =
    if (UIDevice.currentDevice.model.contains("iPad", ignoreCase = true)) DeviceKind.Tablet else DeviceKind.Phone

actual fun getDefaultDeviceName(): String = "Apple ${UIDevice.currentDevice.model}"

// Bonjour discovery runs over the system stack, so there is no interface list to read here
actual fun getLocalIpV4Addresses(): List<String> = emptyList()
