// LinkIdentity.ios.kt

package com.mochyy.mochymusic.link

import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIDevice

// the app's own id and name, kept in the system defaults like every other ios setting
actual object LinkIdentity {
    private const val IdKey = "device_id"

    actual fun id(platformContext: Any): String {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.stringForKey(IdKey)?.takeIf { it.isNotBlank() }?.let { return it }
        val fresh = NSUUID().UUIDString
        defaults.setObject(fresh, forKey = IdKey)
        return fresh
    }

    actual fun name(platformContext: Any): String =
        UIDevice.currentDevice.name.ifBlank { getDefaultDeviceName() }
}
