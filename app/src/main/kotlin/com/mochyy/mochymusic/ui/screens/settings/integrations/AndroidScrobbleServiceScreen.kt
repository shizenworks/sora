// Android's sign-in sheet and toasts, wired into the shared scrobbling page.

package com.mochyy.mochymusic.ui.screens.settings.integrations

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.scrobble.ScrobbleSite
import com.mochyy.mochymusic.ui.component.LocalZoomOutOverlayState
import com.mochyy.mochymusic.ui.component.PopupSheetHandle
import com.mochyy.mochymusic.viewmodels.IntegrationsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AndroidScrobbleServiceScreen(
    navController: NavController,
    site: ScrobbleSite,
    viewModel: IntegrationsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val overlay = LocalZoomOutOverlayState.current
    val host = ScrobbleServiceHost(
        toast = { message, long ->
            Toast.makeText(context, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        },
        showSignIn = {
            var sheet: PopupSheetHandle? = null
            sheet = overlay.showWithHandle {
                ScrobbleSignInContent(
                    site = site,
                    viewModel = viewModel,
                    onSignedIn = { name ->
                        sheet?.dismiss()
                        Toast.makeText(context, context.getString(R.string.scrobble_signed_in, site.label, name), Toast.LENGTH_LONG).show()
                    },
                )
            }
        },
    )
    ScrobbleServiceScreen(
        navController = navController,
        site = ScrobbleSiteInfo(
            id = site.name,
            label = site.label,
            enabledKey = site.enabledKey,
            isConfigured = site.api.isConfigured,
            profileUrl = site::profileUrl,
        ),
        viewModel = viewModel,
        host = host,
    )
}
