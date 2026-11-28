package com.mochyy.mochymusic

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.staticCompositionLocalOf

// the system bars plus whatever the player covers at the bottom; Android's MainActivity and TvShell
// provide it, desktop and iOS give their plain system bars
val LocalPlayerAwareWindowInsets = staticCompositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
