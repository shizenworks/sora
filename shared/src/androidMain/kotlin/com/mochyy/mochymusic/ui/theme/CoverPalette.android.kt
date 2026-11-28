package com.mochyy.mochymusic.ui.theme

import androidx.palette.graphics.Palette
import coil3.Image
import coil3.toBitmap

// Android keeps Mochy's own reader: androidx.palette, with the same maximum count, resize area and
// bottom region the app used before the components moved.
actual class CoverPaletteReader {
    actual fun read(
        image: Any,
        maximumColorCount: Int,
        resizeArea: Int,
        bottomFraction: Float,
        fallbackArgb: Int,
    ): CoverPalette {
        val bitmap = (image as? Image)?.toBitmap() ?: return CoverPalette(emptyList(), fallbackArgb)
        var builder = Palette.from(bitmap).maximumColorCount(maximumColorCount)
        if (resizeArea > 0) builder = builder.resizeBitmapArea(resizeArea)
        if (bottomFraction > 0f) {
            builder = builder.setRegion(0, (bitmap.height * bottomFraction).toInt(), bitmap.width, bitmap.height)
        }
        val palette = builder.generate()
        return CoverPalette(
            swatches = palette.swatches.map { CoverSwatch(it.rgb, it.population) },
            dominant = palette.getDominantColor(fallbackArgb),
        )
    }
}
