package com.mochyy.mochymusic.ui.theme

import coil3.Image
import com.mochyy.mochymusic.ui.utils.toImageBitmap

actual class CoverPaletteReader {
    actual fun read(
        image: Any,
        maximumColorCount: Int,
        resizeArea: Int,
        bottomFraction: Float,
        fallbackArgb: Int,
    ): CoverPalette {
        val bitmap = (image as? Image)?.toImageBitmap() ?: return CoverPalette(emptyList(), fallbackArgb)
        return readCoverPalettePixels(bitmap, maximumColorCount, resizeArea, bottomFraction, fallbackArgb)
    }
}
