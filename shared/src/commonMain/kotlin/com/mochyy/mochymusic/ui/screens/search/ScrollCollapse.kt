package com.mochyy.mochymusic.ui.screens.search

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val SearchCollapseEasing = CubicBezierEasing(0.57f, 0.53f, 0f, 1f)

const val SearchCollapseDurationMs = 900

/**
 * Same latch-and-settle collapse mechanics as [rememberCollapseProgress] but for a plain
 * [androidx.compose.foundation.ScrollState] (a `Column` + `verticalScroll`, e.g. the Settings
 * screens) instead of a `LazyList`. Crossing the threshold flips a binary target that
 * [animateFloatAsState] always eases all the way to 0 or 1, so the morph finishes smoothly even
 * after the user's finger lifts and scrolling stops, instead of being pinned 1:1 to scroll offset.
 */
@Composable
fun rememberScrollCollapseProgress(
    scrollState: androidx.compose.foundation.ScrollState,
    enterDp: Dp = 24.dp,
    exitDp: Dp = 6.dp,
): State<Float> {
    val density = LocalDensity.current
    val enterPx = with(density) { enterDp.toPx() }
    val exitPx = with(density) { exitDp.toPx() }
    val latch = remember(scrollState) { booleanArrayOf(false) }
    val collapsed by remember(scrollState, enterPx, exitPx) {
        derivedStateOf {
            val offset = scrollState.value.toFloat()
            val next = when {
                offset > enterPx -> true
                offset < exitPx -> false
                else -> latch[0]
            }
            latch[0] = next
            next
        }
    }
    return animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(
            durationMillis = SearchCollapseDurationMs,
            easing = SearchCollapseEasing,
        ),
        label = "scrollCollapse",
    )
}
