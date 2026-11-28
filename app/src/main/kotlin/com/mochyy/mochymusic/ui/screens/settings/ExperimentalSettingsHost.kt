package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

// the crossmix status and test rows read the app's player connection, so they stay here;
// the page itself is shared (shared/.../ui/screens/settings/ExperimentalSettingsScreen.kt)
@Composable
fun AndroidExperimentalSettingsScreen(navController: NavController) {
    ExperimentalSettingsScreen(
        navController = navController,
        crossmixRows = { visible -> crossmixTestItems(isVisible = visible) },
    )
}
