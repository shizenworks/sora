package com.mochyy.mochymusic.ui.component

import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

private val composeCache = ConcurrentHashMap<Int, RenderEffect>()
private val nativeCache = ConcurrentHashMap<Int, android.graphics.RenderEffect>()

internal actual fun platformBlurEffect(radius: Float, tileMode: Any?): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || radius <= 0.5f) {
        return null
    }

    val mode = tileMode.asAndroidTileMode(Shader.TileMode.CLAMP)
    val quant = glassQuantStep(radius)
    val step = (radius / quant).roundToInt().coerceAtLeast(1)
    val key = (step * 100) + (quant * 10).toInt() + mode.ordinal

    return composeCache.getOrPut(key) {
        val r = step * quant
        android.graphics.RenderEffect
            .createBlurEffect(r, r, mode)
            .asComposeRenderEffect()
    }
}

internal actual fun platformEffectiveRadius(radius: Float, allowLargeRadius: Boolean): Float =
    if (Build.VERSION.SDK_INT <= 34 && !allowLargeRadius) radius.coerceAtMost(54f) else radius

internal actual fun platformQuantize(radius: Float): Float = quantizedGlassRadius(radius)

internal actual fun clearPlatformBlurCache() {
    composeCache.clear()
    nativeCache.clear()
}

/**
 * Retrieves or creates a cached android.graphics.RenderEffect for View/TextureView.
 *
 * An extension rather than a member of the shared object: its type is Android's own RenderEffect,
 * which the other platforms don't have. Android call sites are unchanged.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun BlurEffectCache.getNative(
    radius: Float,
    tileMode: Any? = null,
): android.graphics.RenderEffect? {
    if (radius <= 0.5f) return null

    val effectiveRadius = radius.coerceAtMost(54f)
    val mode = tileMode.asAndroidTileMode(Shader.TileMode.CLAMP)
    val quant = glassQuantStep(effectiveRadius)
    val step = (effectiveRadius / quant).roundToInt().coerceAtLeast(1)
    val key = (step * 100) + (quant * 10).toInt() + mode.ordinal

    return nativeCache.getOrPut(key) {
        val r = step * quant
        android.graphics.RenderEffect.createBlurEffect(r, r, mode)
    }
}
