package com.mochyy.mochymusic.ui.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Image
import coil3.toBitmap

// Ported from Metrolist (github.com/MetrolistGroup/Metrolist), GPL-3.0: shared/src/desktopMain/kotlin/com/metrolist/shared/ui/utils/CoilImageBitmap.desktop.kt
actual fun Image.toImageBitmap(): ImageBitmap = toBitmap().asComposeImageBitmap()
