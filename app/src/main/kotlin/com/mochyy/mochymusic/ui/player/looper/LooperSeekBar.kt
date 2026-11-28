// The Looper's seek bar: a thin track with a white playhead pill, and the played part rippling
// like a wave while the song plays. Adapted from Natune's Looper player (NatuneGroup/Natune,
// GPL-3.0), based on ArchiveTune's Looper by Rukamori (GPL-3.0).
package com.mochyy.mochymusic.ui.player.looper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import com.mochyy.mochymusic.ui.player.styles.LivePosition
import com.mochyy.mochymusic.ui.player.styles.LocalStyleInteractive
import com.mochyy.mochymusic.ui.player.styles.rememberPlayedSeconds
import com.mochyy.mochymusic.ui.player.styles.rememberStyleShowing
import com.mochyy.mochymusic.ui.player.styles.whileStyleInteractive
import com.mochyy.mochymusic.utils.makeTimeString
import kotlin.math.sin

private val TrackHeight = 8.dp
private val TrackGap = 6.dp
private val ThumbWidth = 6.dp
private val ThumbHeight = 16.dp

/**
 * [position] is read in draw, so the playhead moves every frame without recomposing. A drag shows
 * its own position and seeks once, when the finger lifts. The wave stops when paused, when the
 * player is collapsed, and stays flat in a selector card.
 */
@Composable
internal fun LooperSeekBar(
    position: LivePosition,
    duration: Long,
    playing: Boolean,
    preview: Boolean,
    accent: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactive = LocalStyleInteractive.current
    val showing = rememberStyleShowing()
    val safeDuration = if (duration > 0L) duration else 1L
    var drag by remember { mutableStateOf<Float?>(null) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }

    val waving = playing && !preview
    val waveBlend by animateFloatAsState(
        targetValue = if (waving) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "looperWaveBlend",
    )
    // the only frame loop in the bar: runs while the song plays on an open player, and ends with either
    LaunchedEffect(waving) {
        if (!waving) return@LaunchedEffect
        snapshotFlow { showing() }.collectLatest { shown ->
            if (!shown) return@collectLatest
            var last = 0L
            while (true) {
                withFrameNanos { nanos ->
                    if (last != 0L) wavePhase = (wavePhase + (nanos - last) / 1_000_000_000f * 0.05f) % 1f
                    last = nanos
                }
            }
        }
    }

    val played = rememberPlayedSeconds(position)
    val shownMs = drag?.let { (it * safeDuration).toLong() } ?: (played * 1_000L).coerceIn(0L, safeDuration)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(ThumbHeight)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(safeDuration, preview, interactive) {
                    if (preview) return@pointerInput
                    whileStyleInteractive(interactive) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            fun fractionAt(x: Float): Float =
                                if (trackWidthPx <= 0f) 0f else (x / trackWidthPx).coerceIn(0f, 1f)
                            var lastX = down.position.x
                            drag = fractionAt(lastX)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                lastX = change.position.x
                                if (!change.pressed) break
                                change.consume()
                                drag = fractionAt(lastX)
                            }
                            onSeek((fractionAt(lastX) * safeDuration).toLong())
                            drag = null
                        }
                    }
                },
        ) {
            val fraction = drag ?: (position.positionMs().toFloat() / safeDuration).coerceIn(0f, 1f)
            val width = size.width
            val centerY = size.height / 2f
            val trackH = TrackHeight.toPx()
            val gap = TrackGap.toPx()
            val amplitude = 2.dp.toPx()
            val wavelength = 6.dp.toPx()
            val thumbW = ThumbWidth.toPx()
            val thumbH = ThumbHeight.toPx()
            val thumbX = fraction * width
            val corner = CornerRadius(trackH / 2f, trackH / 2f)

            val inactiveStart = (thumbX + gap).coerceAtMost(width)
            if (inactiveStart < width) {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(inactiveStart, centerY - trackH / 2f),
                    size = Size(width - inactiveStart, trackH),
                    cornerRadius = corner,
                )
            }

            val activeEnd = (thumbX - gap).coerceAtLeast(0f)
            if (activeEnd > 0f) {
                if (waveBlend <= 0.01f) {
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(0f, centerY - trackH / 2f),
                        size = Size(activeEnd, trackH),
                        cornerRadius = corner,
                    )
                } else {
                    val easeLength = wavelength * 3f
                    val path = Path()
                    val step = 1.dp.toPx().coerceAtLeast(1f)
                    var x = 0f
                    while (x <= activeEnd) {
                        val ease = when {
                            x < easeLength -> x / easeLength
                            x > activeEnd - easeLength -> (activeEnd - x) / easeLength
                            else -> 1f
                        }
                        val y = centerY +
                            (sin(x / wavelength + wavePhase * 2f * Math.PI.toFloat()) * amplitude) * ease * waveBlend
                        if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                        x += step
                    }
                    path.lineTo(activeEnd, centerY)
                    drawPath(path = path, color = accent, style = Stroke(width = trackH, cap = StrokeCap.Round))
                }
            }

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(thumbX - thumbW / 2f, centerY - thumbH / 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f),
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(makeTimeString(shownMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
            Text("-" + makeTimeString(safeDuration - shownMs), style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}
