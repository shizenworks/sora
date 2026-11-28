package com.mochyy.mochymusic.ui.screens.settings.devspace

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens a link in the browser. It lives in :app, not with the dev space screens in :shared, because
 * it is an Android [Intent]; other screens (settings modules, the player's module menu, the profile
 * page) import it from here. Shared code opens links through Compose's LocalUriHandler instead.
 */
fun Context.openDevUrl(url: String) {
    val clean = url.trim()
    val parsed = runCatching { Uri.parse(clean) }.getOrNull() ?: return
    val scheme = parsed.scheme?.lowercase()
    // never let a module or bad link fire intent://, file://, or javascript:
    if (scheme != "https" && scheme != "http") return
    if (parsed.host.isNullOrBlank()) return
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, parsed).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
