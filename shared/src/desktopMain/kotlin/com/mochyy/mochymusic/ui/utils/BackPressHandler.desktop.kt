package com.mochyy.mochymusic.ui.utils

import androidx.compose.runtime.Composable

// Compose's common BackHandler isn't on this build's classpath; until the shared navigation brings
// navigationevent, there is no system back here (the search overlay closes with its own X)
@Composable
actual fun BackPressHandler(enabled: Boolean, onBack: () -> Unit) = Unit
