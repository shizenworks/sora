package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.mochyy.mochymusic.ui.component.LocalZoomOutOverlayState
import com.mochyy.mochymusic.ui.component.navigateToTab
import com.mochyy.mochymusic.ui.screens.devices.rememberDevicesLink
import com.mochyy.mochymusic.ui.screens.devices.showDeviceIntegration
import com.mochyy.mochymusic.ui.screens.settings.modules.ModuleSettingsPages
import com.mochyy.mochymusic.ui.screens.settings.modules.moduleRoute
import com.mochyy.mochymusic.ui.screens.settings.modules.showFindModulesSheet
import com.mochyy.mochymusic.viewmodels.ModulesViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Android's settings list: the shared list plus the rows only the app can open, the tips card grown
 * from the phone's own audio and network state, and the module store sheet over the module card.
 * Everything Android-specific stays here so the shared screen is the same body on every platform.
 */
@Composable
fun AndroidMochySettingsScreen(navController: NavController, pane: SettingsPane? = null) {
    val sheets = LocalZoomOutOverlayState.current
    val modules: ModulesViewModel = koinViewModel()
    val devices = rememberDevicesLink()
    val context = LocalContext.current
    // picked again each time the list opens, so a tip that was just followed stops showing
    val recommendations = remember(context) { pickRecommendations(context) }

    MochySettingsScreen(
        navController = navController,
        selectedRoute = pane?.selectedRoute,
        chrome = AppMochySettingsChrome,
        platform = AndroidSettingsListPlatform,
        onOpenPage = if (pane != null) ({ navController.openSettingsPage(it) }) else null,
        recommendations = { open -> RecommendedCard(recommendations = recommendations, open = open) },
        onOpenModuleStore = {
            sheets.showFindModulesSheet(modules) { id -> navController.navigate(moduleRoute(id)) }
        },
        modulePages = { open, selected -> ModuleSettingsPages(open = open, selected = selected) },
        onOpenDevices = { sheets.showDeviceIntegration(devices) },
        onProfileClick = {
            // back to the profile page these were opened from, not a second copy of it on top
            if (!navController.popBackStack("settings", inclusive = false)) navController.navigateToTab("settings")
        },
    )
}

private val AndroidSettingsListPlatform = SettingsListPlatform(
    audioLab = true,
    customisation = true,
    modules = true,
    devices = true,
)
