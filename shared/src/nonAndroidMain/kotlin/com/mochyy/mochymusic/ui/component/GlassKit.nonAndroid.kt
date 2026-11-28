package com.mochyy.mochymusic.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.mochyy.mochymusic.constants.BlurQuality

/*
 * Desktop and iOS glass, on Skia.
 *
 * Skia can blur a layer (BlurEffect -> an ImageFilter blur), and that is what every blur below is.
 * What Compose on Skia can't do is read back what is drawn *behind* a composable, which is what
 * Android's RenderNode glass samples. So a glass surface here is a translucent frost over the
 * foundation and tint, with the same gradient masks and hairlines Android draws - it just has no
 * blurred app underneath, and progressive blur fades the frost in instead of growing a blur.
 */

actual fun Modifier.glassRoot(state: GlassState, isActive: () -> Boolean): Modifier =
    this.onGloballyPositioned { state.rootPosition = it.positionInWindow() }

actual fun Modifier.damageBoundary(): Modifier = this

internal actual fun discardGlassBackdrop(node: Any?) = Unit

internal actual val glassBlurSupported: Boolean = true

actual val DefaultBlurQuality: BlurQuality = BlurQuality.HIGH

/** A [GlassTileMode] or an `android.graphics.Shader.TileMode` name, as Compose's [TileMode]. */
internal fun Any?.asComposeTileMode(default: TileMode): TileMode = when (this) {
    null -> default
    is TileMode -> this
    GlassTileMode.Clamp -> TileMode.Clamp
    GlassTileMode.Decal -> TileMode.Decal
    GlassTileMode.Mirror -> TileMode.Mirror
    GlassTileMode.Repeat -> TileMode.Repeated
    else -> default
}

/** A blur of a thing itself on Skia; null while it is too small to see. */
internal fun skiaBlur(radius: Float, tileMode: Any?): BlurEffect? =
    if (radius > 0.5f) BlurEffect(radius, radius, tileMode.asComposeTileMode(TileMode.Decal)) else null

@Composable
actual fun GlassPillBackground(
    state: GlassState,
    blurRadius: () -> Float,
    tint: Color,
    foundationColor: Color?,
    shape: Shape?,
    tileMode: Any?,
    mask: Brush?,
    accentWash: (() -> Color)?,
    bleed: Boolean,
    progressive: BlurDirection?,
    progressiveRamp: Float,
    modifier: Modifier,
) {
    val currentTint by rememberUpdatedState(tint)
    val currentFoundation by rememberUpdatedState(foundationColor)
    val currentWash by rememberUpdatedState(accentWash)

    Box(
        modifier = modifier
            .then(if (shape != null) Modifier.clip(shape) else Modifier)
            .then(
                // the mask is a DstIn over the whole surface, so it needs its own buffer, as below API 31 does on Android
                if (mask != null) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = mask, blendMode = BlendMode.DstIn)
                        }
                } else Modifier
            )
            .clipToBounds(),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val foundation = currentFoundation
            if (foundation != null) drawRect(color = foundation, size = size)
            if (currentTint != Color.Transparent) drawRect(color = currentTint, size = size)
            val wash = currentWash?.invoke()
            if (wash != null && wash.alpha > 0f) drawRect(color = wash, size = size)
        }
    }
}

@Composable
actual fun ProgressiveGlassBackground(
    state: GlassState,
    maxBlurRadius: () -> Float,
    tint: Color,
    foundationColor: Color?,
    direction: BlurDirection,
    modifier: Modifier,
    steps: Int,
) {
    if (!rememberProgressiveBlur()) {
        Box(
            modifier = modifier
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    val line = FlatBarEdgeWidth.toPx()
                    val y = if (direction == BlurDirection.TopToBottom) 0f else size.height - line
                    drawRect(FlatBarEdgeColor, topLeft = Offset(0f, y), size = Size(size.width, line))
                },
        ) {
            GlassPillBackground(
                state = state,
                blurRadius = maxBlurRadius,
                tint = tint,
                foundationColor = foundationColor,
                tileMode = GlassTileMode.Clamp,
                bleed = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }
    // no variable-radius blur on Skia yet: fade the frost in over the first step, the way Android's
    // progressive glass is faded in where its first band starts
    val fade = 1f / steps.coerceAtLeast(1)
    val mask = remember(direction, fade) {
        if (direction == BlurDirection.TopToBottom) {
            Brush.verticalGradient(0f to Color.Transparent, fade to Color.Black, 1f to Color.Black)
        } else {
            Brush.verticalGradient(0f to Color.Black, (1f - fade) to Color.Black, 1f to Color.Transparent)
        }
    }
    GlassPillBackground(
        state = state,
        blurRadius = maxBlurRadius,
        tint = tint,
        foundationColor = foundationColor,
        mask = mask,
        modifier = modifier,
    )
}

@Composable
internal actual fun PlatformDownscaledBlur(
    radius: () -> Float,
    tileMode: Any?,
    mask: Brush?,
    modifier: Modifier,
    offscreen: (() -> Boolean)?,
    growDown: Boolean?,
    growAcross: Boolean,
    growRamp: Float,
    draw: DrawScope.(fullSize: Size) -> Unit,
) {
    val currentRadius by rememberUpdatedState(radius)
    val currentOffscreen by rememberUpdatedState(offscreen)

    Box(
        modifier = modifier
            .clipToBounds()
            .then(
                if (mask != null) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = mask, blendMode = BlendMode.DstIn)
                        }
                } else Modifier
            ),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    renderEffect = skiaBlur(currentRadius(), tileMode)
                    compositingStrategy = if (currentOffscreen?.invoke() == true) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                    clip = true
                },
        ) {
            draw(size)
        }
    }
}
