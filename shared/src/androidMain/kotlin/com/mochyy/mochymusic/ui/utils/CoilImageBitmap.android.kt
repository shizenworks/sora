package com.mochyy.mochymusic.ui.utils

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.toBitmap

// Ported from Metrolist (github.com/MetrolistGroup/Metrolist), GPL-3.0: shared/src/androidMain/kotlin/com/metrolist/shared/ui/utils/CoilImageBitmap.android.kt
actual fun Image.toImageBitmap(): ImageBitmap {
    val bitmap = toBitmap()
    // software canvases used by image export cannot draw hardware-backed bitmaps
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
        checkNotNull(bitmap.copy(Bitmap.Config.ARGB_8888, false)).asImageBitmap()
    } else {
        bitmap.asImageBitmap()
    }
}
