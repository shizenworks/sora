// LinkIdentity.android.kt

package com.mochyy.mochymusic.link

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID

// this device as the other side sees it: the same preferences file and key the app has always used
actual object LinkIdentity {
    private const val Prefs = "mochy_link"
    private const val IdKey = "device_id"

    actual fun id(platformContext: Any): String {
        val context = platformContext as Context
        val prefs = context.getSharedPreferences(Prefs, Context.MODE_PRIVATE)
        prefs.getString(IdKey, null)?.let { return it }
        val fresh = UUID.randomUUID().toString()
        prefs.edit().putString(IdKey, fresh).apply()
        return fresh
    }

    // the name set in the phone's own settings when there is one ("Kei's Pixel"), the model otherwise
    actual fun name(platformContext: Any): String {
        val context = platformContext as Context
        val named = runCatching { Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME) }.getOrNull()
        return named?.takeIf { it.isNotBlank() } ?: Build.MODEL.orEmpty().ifBlank { "Android" }
    }
}
