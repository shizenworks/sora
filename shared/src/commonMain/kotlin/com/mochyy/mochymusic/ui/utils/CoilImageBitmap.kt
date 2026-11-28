package com.mochyy.mochymusic.ui.utils

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image

// Ported from Metrolist (github.com/MetrolistGroup/Metrolist), GPL-3.0: shared/src/commonMain/kotlin/com/metrolist/shared/ui/utils/CoilImageBitmap.kt
/** Turns a decoded Coil image into Compose's bitmap, which common code can read pixels from. */
expect fun Image.toImageBitmap(): ImageBitmap
