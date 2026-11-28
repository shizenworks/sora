package com.mochyy.mochymusic.ui.screens.recap

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * The recap and Stats easing and float lerp. They sat in the story files, but the Stats charts and
 * ruler use them too, so they live where both can reach them. The curve is the one the screens have
 * always used.
 */
val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t
