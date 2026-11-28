package com.mochyy.mochymusic.ui.component

import androidx.compose.ui.graphics.RenderEffect

internal actual fun platformBlurEffect(radius: Float, tileMode: Any?): RenderEffect? =
    skiaBlur(radius, tileMode)

// Skia's blur has no HWUI multi-pass limit, so the Android clamp doesn't apply here
internal actual fun platformEffectiveRadius(radius: Float, allowLargeRadius: Boolean): Float = radius

internal actual fun platformQuantize(radius: Float): Float = quantizedGlassRadius(radius)

internal actual fun clearPlatformBlurCache() = Unit
