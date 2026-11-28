package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import com.mochyy.mochymusic.utils.cipher.PlayerJsFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

// the player-script download, file cache and date format are app-only; the page is shared
// (shared/.../ui/screens/settings/CipherSettingsScreen.kt)
@Composable
fun AndroidCipherSettingsScreen(navController: NavController) {
    val host = remember { AndroidCipherHost() }
    CipherSettingsScreen(navController = navController, host = host)
}

private class AndroidCipherHost : CipherHost {
    override val cacheTtlMs: Long = PlayerJsFetcher.CACHE_TTL_MS

    override fun readStatus(): CipherCacheStatus {
        val status = PlayerJsFetcher.readCacheStatus()
        return CipherCacheStatus(status.playerHash, status.lastUpdatedMs, status.sizeBytes)
    }

    override suspend fun refresh(): String? = withContext(Dispatchers.IO) {
        PlayerJsFetcher.invalidateCache()
        PlayerJsFetcher.getPlayerJs(forceRefresh = true)?.second
    }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) { PlayerJsFetcher.invalidateCache() }
    }

    override fun formatUpdatedAt(epochMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
}
