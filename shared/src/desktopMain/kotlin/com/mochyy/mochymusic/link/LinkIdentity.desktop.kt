// LinkIdentity.desktop.kt

package com.mochyy.mochymusic.link

import java.util.UUID
import java.util.prefs.Preferences

// the desktop app's own id and name; the key survives restarts the same way android's preferences do
actual object LinkIdentity {
    private const val IdKey = "device_id"
    private val prefs: Preferences by lazy { Preferences.userRoot().node("com/mochyy/mochymusic/link") }

    actual fun id(platformContext: Any): String {
        prefs.get(IdKey, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val fresh = UUID.randomUUID().toString()
        prefs.put(IdKey, fresh)
        runCatching { prefs.flush() }
        return fresh
    }

    actual fun name(platformContext: Any): String = getDefaultDeviceName()
}
