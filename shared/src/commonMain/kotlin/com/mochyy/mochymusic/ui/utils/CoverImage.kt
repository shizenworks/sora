package com.mochyy.mochymusic.ui.utils

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.mochyy.mochymusic.ui.theme.CoverPalette
import com.mochyy.mochymusic.ui.theme.CoverPaletteReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// a palette needs the cover's pixels, not a hardware-backed bitmap (Metrolist keeps the same split)
internal expect fun ImageRequest.Builder.prepareForPalette(): ImageRequest.Builder

/**
 * Reads [url] at [size] and turns it into a palette, through the app's one image loader.
 * Returns null for a cover that can't be read; callers keep their own fallback.
 */
internal suspend fun readCoverPalette(
    context: PlatformContext,
    url: String,
    size: Int,
    maximumColorCount: Int,
    resizeArea: Int,
    bottomFraction: Float = 0f,
    fallbackArgb: Int,
): CoverPalette? = try {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(size, size)
        .prepareForPalette()
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()
    val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image
        ?: return null
    withContext(Dispatchers.Default) {
        CoverPaletteReader().read(image, maximumColorCount, resizeArea, bottomFraction, fallbackArgb)
    }
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
