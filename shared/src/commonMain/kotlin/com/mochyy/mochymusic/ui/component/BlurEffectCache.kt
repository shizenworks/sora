package com.mochyy.mochymusic.ui.component

import androidx.compose.ui.graphics.RenderEffect
import kotlin.math.roundToInt

/**
 * High-performance blur-effect cache for Compose, and the one place a glass radius is decided.
 *
 * On Android 12-14 (API 31-34), HWUI applies Gaussian blurs at full layer resolution.
 * If radius is large (>54f), Skia cannot compute the convolution in a single pass,
 * causing multi-pass FBO buffer allocations and GPU fill-rate pipeline stalls.
 * On Android 15-16 (API 35+), HWUI has hardware pyramid downsampling and handles
 * arbitrary radii smoothly.
 *
 * This cache:
 * 1. Clamps extreme radii on API <= 34 to 54f by default to avoid multi-pass GPU stalls.
 * 2. Quantizes radii to prevent thousands of native SkImageFilter allocations.
 * 3. Returns null for radius <= 0.5f to immediately free offscreen layer overhead.
 *
 * Desktop and iOS keep the same radii (no clamp, Skia's blur has no HWUI limit) and make a fresh
 * Compose [androidx.compose.ui.graphics.BlurEffect] each time; there is nothing native to cache.
 */
object BlurEffectCache {
    /**
     * Retrieves or creates a cached blur effect.
     *
     * [tileMode] takes a [GlassTileMode] or, on Android, an `android.graphics.Shader.TileMode`
     * (what the app's call sites pass). Null is the platform default: CLAMP on Android, Decal on
     * desktop and iOS.
     */
    fun get(
        radius: Float,
        tileMode: Any? = null,
        allowLargeRadius: Boolean = false,
    ): RenderEffect? {
        val effectiveRadius = effectiveRadius(radius, allowLargeRadius)
        if (effectiveRadius <= 0.5f) return null
        return platformBlurEffect(quantize(effectiveRadius), tileMode)
    }

    /** a blur on a thing itself (a lyric line, a morphing title, a menu row), gone while element blur is off */
    fun element(
        radius: Float,
        tileMode: Any? = null,
    ): RenderEffect? = if (elementBlurEnabled()) get(radius, tileMode) else null

    /** The radius [get] actually blurs by: clamped on API <= 34 unless [allowLargeRadius]. */
    fun effectiveRadius(radius: Float, allowLargeRadius: Boolean = false): Float =
        platformEffectiveRadius(radius, allowLargeRadius)

    /** [radius] rounded to the step [get] caches by, so a cache keyed on it matches [get]'s. */
    fun quantize(radius: Float): Float = platformQuantize(radius)

    // drops cached native render effects when the ui hides so skia filters aren't pinned in ram
    fun clear() = clearPlatformBlurCache()
}

/** Quantization every platform shares: the steps [Android's cache](platformBlurEffect) keys on. */
internal fun glassQuantStep(radius: Float): Float = when {
    radius <= 16f -> 0.5f
    radius <= 32f -> 1.0f
    radius <= 64f -> 2.0f
    else -> 4.0f
}

internal fun quantizedGlassRadius(radius: Float): Float {
    val quant = glassQuantStep(radius)
    return (radius / quant).roundToInt().coerceAtLeast(1) * quant
}

internal expect fun platformBlurEffect(radius: Float, tileMode: Any?): RenderEffect?

internal expect fun platformEffectiveRadius(radius: Float, allowLargeRadius: Boolean): Float

internal expect fun platformQuantize(radius: Float): Float

internal expect fun clearPlatformBlurCache()
