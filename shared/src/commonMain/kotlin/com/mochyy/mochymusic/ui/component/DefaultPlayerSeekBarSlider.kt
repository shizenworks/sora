package com.mochyy.mochymusic.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import com.mochyy.mochymusic.shared.ui.theme.LocalMochyAccent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.ui.component.liquid.LiquidGlassSlider
import com.mochyy.mochymusic.ui.component.liquid.LocalLiquidGlass
import kotlin.math.roundToInt

/** A settings slider that uses the player's default rounded seek-bar treatment. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultPlayerSeekBarSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    enabled: Boolean = true,
) {
    // the liquid glass sliders switch, when it is on, is the look of every slider in the app
    val glass = LocalLiquidGlass.current
    if (glass.surface(glass.sliders)) {
        LiquidGlassSlider(
            value = value,
            onValueChange = { raw ->
                onValueChange(if (steps > 0) snapToSteps(raw, valueRange, steps) else raw)
            },
            modifier = modifier.sliderDragScrollLock(),
            valueRange = valueRange,
            enabled = enabled,
        )
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    // the accent only when one is picked, so the default grey stays as it was
    val activeBase = LocalMochyAccent.current ?: MaterialTheme.colorScheme.onSurface
    val activeColor = if (enabled) activeBase else activeBase.copy(alpha = 0.38f)
    val inactiveColor = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f) else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        interactionSource = interactionSource,
        thumb = { Spacer(Modifier.size(0.dp)) },
        track = { sliderState ->
            val range = sliderState.valueRange
            val span = (range.endInclusive - range.start).takeIf { it > 0f }
            val fraction = span?.let { (sliderState.value - range.start) / it } ?: 0f
            SeekBarTrack(
                style = SeekBarStyle.DEFAULT,
                fraction = fraction,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                animateWave = false,
                active = isDragged || isPressed,
            )
        },
        modifier = modifier
            .sliderDragScrollLock()
            .fillMaxWidth()
            .height(28.dp),
    )
}

/** Material's steps mean this many stops strictly between the two ends: steps + 2 values in all. */
private fun snapToSteps(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
): Float {
    val span = range.endInclusive - range.start
    if (span <= 0f || steps <= 0) return value.coerceIn(range)
    val intervals = steps + 1
    val stepped = (((value - range.start) / span * intervals).roundToInt().toFloat() / intervals) * span + range.start
    return stepped.coerceIn(range)
}
