// DeviceKind.android.kt

package com.mochyy.mochymusic.link

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

actual fun deviceKindOf(platformContext: Any): DeviceKind {
    val context = platformContext as Context
    if (context.isTelevision()) return DeviceKind.Tv
    val mode = (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType
    if (mode == Configuration.UI_MODE_TYPE_CAR || context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return DeviceKind.Car
    return if (context.resources.configuration.smallestScreenWidthDp >= 600) DeviceKind.Tablet else DeviceKind.Phone
}

// LinkIdentity.name reads the name set in the phone's own settings; this is the model when nobody set one
actual fun getDefaultDeviceName(): String = Build.MODEL.orEmpty().ifBlank { "Android" }

actual fun getLocalIpV4Addresses(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .filterNot { it.isLoopbackAddress || it.isLinkLocalAddress }
        .mapNotNull { it.hostAddress }
}.getOrDefault(emptyList())

// a tv runs the big-screen app and the phone side of the link never starts there
fun Context.isTelevision(): Boolean {
    val modeType = (getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType
    return modeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}
