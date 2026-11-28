package com.mochyy.mochymusic.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.utils.AppDns
import com.mochyy.mochymusic.utils.NetMetered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

// what the shared Internet page can't do itself: watch the connection, name the DoH providers and
// relaunch after a region change. The page lives in shared/.../ui/screens/settings/InternetScreen.kt
@Composable
fun AndroidInternetSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val metered by NetMetered.metered.collectAsState()
    InternetSettingsScreen(
        navController = navController,
        metered = metered,
        dnsProviders = AppDns.providers,
        onRegionChanged = { restartForRegionChange(context) },
    )
}

/**
 * Applies a region change the way the ported design does: the visitor data and every cached
 * response were minted in the old region, so the process is replaced rather than
 * half-invalidated. The toast is held open briefly before the relaunch so it reads.
 */
internal fun restartForRegionChange(context: Context) {
    Toast.makeText(context, context.getString(R.string.youtube_music_region_restarting), Toast.LENGTH_SHORT).show()
    val scope = CoroutineScope(Dispatchers.Main)
    scope.launch {
        delay(400)
        withContext(NonCancellable) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(launchIntent)
            }
            exitProcess(0)
        }
    }
}
