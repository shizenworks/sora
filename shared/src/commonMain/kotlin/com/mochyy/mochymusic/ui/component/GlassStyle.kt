package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import com.mochyy.mochymusic.utils.rememberPreference

enum class BlurDirection { TopToBottom, BottomToTop }

// how dark the one even tint is that replaces a bar's fading scrim while progressive blur is off
const val FlatBarScrimAlpha = 0.45f

// whether bars blur progressively; off, they're flat with a hairline, and their own fading scrims turn into one tint
@Composable
fun rememberProgressiveBlur(): Boolean {
    val (enabled) = rememberPreference(com.mochyy.mochymusic.constants.EnableProgressiveBlurKey, defaultValue = true)
    return enabled
}
