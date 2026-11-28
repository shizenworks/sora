// CoverPalette.kt
//
// Cover colours for the shared components (Home's big cards, album gradients, the Liked cover).
// Android reads them with the same androidx.palette the app has always used; desktop and iOS
// quantize the pixels with materialKolor, as Metrolist does. The colour maths below is a port of
// androidx ColorUtils so every platform applies the same tone and shade.

package com.mochyy.mochymusic.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import com.materialkolor.quantize.QuantizerCelebi
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** one colour a cover is made of, with how much of the cover has it */
@Immutable
data class CoverSwatch(
    val argb: Int,
    val population: Int,
)

/** the colours read from one cover; [dominant] is already resolved against the caller's fallback */
@Immutable
data class CoverPalette(
    val swatches: List<CoverSwatch>,
    val dominant: Int,
)

/**
 * Turns a decoded cover into the swatches the shared components need.
 *
 * [image] is Coil's decoded image, so the actual can read it without a second decode. [resizeArea]
 * limits the work Palette does (0 leaves the bitmap as is), and [bottomFraction] reads only the
 * bottom of the cover, which is what lets a cover fade into the background without a seam.
 */
expect class CoverPaletteReader() {
    fun read(
        image: Any,
        maximumColorCount: Int,
        resizeArea: Int,
        bottomFraction: Float,
        fallbackArgb: Int,
    ): CoverPalette
}

/**
 * The desktop/iOS half of [CoverPaletteReader]: Compose pixels go through materialKolor's Celebi
 * quantizer, and the biggest bucket becomes the dominant colour. Android's actual uses the same
 * androidx.palette the app has always used instead.
 */
internal fun readCoverPalettePixels(
    image: ImageBitmap,
    maximumColorCount: Int,
    resizeArea: Int,
    bottomFraction: Float,
    fallbackArgb: Int,
): CoverPalette {
    val pixelMap = image.toPixelMap()
    val width = pixelMap.width
    val height = pixelMap.height
    if (width <= 0 || height <= 0) return CoverPalette(emptyList(), fallbackArgb)

    val firstRow = (height * bottomFraction.coerceIn(0f, 1f)).toInt().coerceIn(0, height - 1)
    // the same idea as Palette.resizeBitmapArea: read enough pixels to judge the cover, not every one
    val step = if (resizeArea > 0) {
        sqrt(width.toFloat() * height / resizeArea).toInt().coerceAtLeast(1)
    } else {
        1
    }
    val buffer = pixelMap.buffer
    val pixels = ArrayList<Int>(width * (height - firstRow) / (step * step))
    for (y in firstRow until height step step) {
        for (x in 0 until width step step) {
            val argb = buffer[y * width + x]
            if (argb ushr 24 < 128) continue
            pixels += argb or 0xFF000000.toInt()
        }
    }
    if (pixels.isEmpty()) return CoverPalette(emptyList(), fallbackArgb)

    val quantized = QuantizerCelebi.quantize(pixels.toIntArray(), maximumColorCount.coerceAtLeast(1))
    val swatches = quantized.entries
        .map { CoverSwatch(it.key, it.value) }
        .sortedByDescending { it.population }
    return CoverPalette(swatches, swatches.firstOrNull()?.argb ?: fallbackArgb)
}

/** [dominant] and a darker copy of it, then black: the player background shape Mochy already uses */
fun CoverPalette.gradientColors(): List<Color> {
    val primary = Color(dominant)
    return listOf(
        primary,
        primary.copy(
            red = (primary.red * 0.6f).coerceAtLeast(0f),
            green = (primary.green * 0.6f).coerceAtLeast(0f),
            blue = (primary.blue * 0.6f).coerceAtLeast(0f),
        ),
        Color.Black,
    )
}

/** the colour the cover is mostly made of, leaning towards ones with some colour in them */
fun CoverPalette.likedCoverColor(): Color? {
    val hsl = FloatArray(3)
    val swatch = swatches.maxByOrNull { swatch ->
        colorToHSL(swatch.argb, hsl)
        swatch.population * (0.35f + hsl[1]) * (1.15f - abs(hsl[2] - 0.45f) * 1.6f).coerceAtLeast(0.1f)
    } ?: return null
    return Color(likedCoverTone(swatch.argb))
}

// keeps the hue and evens out the rest, so no one cover shouts over the others and the heart
// always reads. a grey cover stays grey: lifting its saturation would turn it red (hue 0)
fun likedCoverTone(argb: Int): Int {
    val hsl = FloatArray(3)
    colorToHSL(argb, hsl)
    if (hsl[1] >= 0.08f) hsl[1] = hsl[1].coerceIn(0.2f, 0.58f)
    hsl[2] = hsl[2].coerceIn(0.28f, 0.5f)
    return hslToColor(hsl)
}

/** a corner of the Liked cover, moved in lightness so four different covers still read as one */
fun likedCoverShade(color: Color, by: Float): Color {
    val hsl = FloatArray(3)
    colorToHSL(color.toArgb(), hsl)
    hsl[2] = (hsl[2] + by).coerceIn(0.2f, 0.58f)
    return Color(hslToColor(hsl))
}

/** the bottom of a cover, pushed dark like a sampled cover is */
fun coverThemeDarken(argb: Int): Int {
    val hsl = FloatArray(3)
    colorToHSL(argb, hsl)
    hsl[1] = (hsl[1] * 0.9f).coerceAtMost(0.62f)
    hsl[2] = (hsl[2] * 0.5f).coerceIn(0.09f, 0.3f)
    return hslToColor(hsl)
}

/** how much [color1] and [color2] show at [ratio], the same blend ColorUtils.blendARGB does */
fun blendARGB(color1: Int, color2: Int, ratio: Float): Int {
    val inverse = 1f - ratio
    val a = ((color1 ushr 24 and 0xFF) * inverse + (color2 ushr 24 and 0xFF) * ratio).toInt()
    val r = ((color1 shr 16 and 0xFF) * inverse + (color2 shr 16 and 0xFF) * ratio).toInt()
    val g = ((color1 shr 8 and 0xFF) * inverse + (color2 shr 8 and 0xFF) * ratio).toInt()
    val b = ((color1 and 0xFF) * inverse + (color2 and 0xFF) * ratio).toInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}

// --- androidx ColorUtils, ported so the maths matches on every platform ---

internal fun colorToHSL(color: Int, hsl: FloatArray) {
    val r = (color shr 16 and 0xFF) / 255f
    val g = (color shr 8 and 0xFF) / 255f
    val b = (color and 0xFF) / 255f

    val max = maxOf(r, maxOf(g, b))
    val min = minOf(r, minOf(g, b))
    val delta = max - min
    val lightness = (max + min) / 2f

    var hue = 0f
    var saturation = 0f
    if (max != min) {
        saturation = if (lightness > 0.5f) delta / (2f - max - min) else delta / (max + min)
        hue = when (max) {
            r -> (g - b) / delta + (if (g < b) 6f else 0f)
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        }
        hue /= 6f
    }
    hsl[0] = hue * 360f
    hsl[1] = saturation
    hsl[2] = lightness
}

internal fun hslToColor(hsl: FloatArray): Int {
    val h = hsl[0]
    val s = hsl[1]
    val l = hsl[2]

    val c = (1f - abs(2f * l - 1f)) * s
    val m = l - 0.5f * c
    val x = c * (1f - abs((h / 60f % 2f) - 1f))

    val hueSegment = (h / 60f).toInt()
    val (r, g, b) = when (hueSegment) {
        0 -> Triple(c + m, x + m, m)
        1 -> Triple(x + m, c + m, m)
        2 -> Triple(m, c + m, x + m)
        3 -> Triple(m, x + m, c + m)
        4 -> Triple(x + m, m, c + m)
        // ColorUtils treats 360 as the same red as 0
        5, 6 -> Triple(c + m, m, x + m)
        else -> Triple(0f, 0f, 0f)
    }
    return (255 shl 24) or
        (constrain((r * 255f).roundToInt()) shl 16) or
        (constrain((g * 255f).roundToInt()) shl 8) or
        constrain((b * 255f).roundToInt())
}

private fun constrain(amount: Int): Int = amount.coerceIn(0, 255)
