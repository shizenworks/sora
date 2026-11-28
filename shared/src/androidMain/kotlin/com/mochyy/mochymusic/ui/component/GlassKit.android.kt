package com.mochyy.mochymusic.ui.component

import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import com.mochyy.mochymusic.constants.BlurQuality
import com.mochyy.mochymusic.constants.DisableBlurKey
import com.mochyy.mochymusic.utils.rememberPreference

/*
 * RenderNode helpers. Each is annotated so it is only ever entered above the API level that
 * defines the class, which keeps the reference out of any method reachable on older devices.
 */

@RequiresApi(Build.VERSION_CODES.Q)
internal fun glassNodeFor(existing: Any?, width: Int, height: Int): Any =
    (existing as? RenderNode ?: RenderNode("GlassRoot")).apply { setPosition(0, 0, width, height) }

@RequiresApi(Build.VERSION_CODES.S)
fun glassNodeHasContent(node: Any): Boolean = (node as RenderNode).hasDisplayList()

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeBeginRecording(node: Any): android.graphics.Canvas = (node as RenderNode).beginRecording()

@RequiresApi(Build.VERSION_CODES.S)
internal fun glassNodeEndRecording(node: Any) {
    (node as RenderNode).endRecording()
}

@RequiresApi(Build.VERSION_CODES.S)
fun glassNodeWidth(node: Any): Int = (node as RenderNode).width

@RequiresApi(Build.VERSION_CODES.S)
fun glassNodeHeight(node: Any): Int = (node as RenderNode).height

@RequiresApi(Build.VERSION_CODES.S)
fun android.graphics.Canvas.drawGlassNode(node: Any) {
    drawRenderNode(node as RenderNode)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNode(width: Int, height: Int): Any =
    RenderNode("DamageBoundary").apply {
        setPosition(0, 0, width, height)
        // the whole point: the renderer only bounds a change by a node's size when the node clips
        // to it. Compose's own layers clip by outline instead, so to the renderer a change in any
        // of them is a change to the entire screen
        setClipToBounds(true)
    }

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNodeBeginRecording(node: Any): android.graphics.Canvas = (node as RenderNode).beginRecording()

@RequiresApi(Build.VERSION_CODES.Q)
private fun damageNodeEndRecording(node: Any) {
    (node as RenderNode).endRecording()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun android.graphics.Canvas.drawDamageNode(node: Any) {
    drawRenderNode(node as RenderNode)
}

actual fun Modifier.damageBoundary(): Modifier =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        this
    } else {
        this.drawWithCache {
            val width = size.width.toInt()
            val height = size.height.toInt()
            val node = if (width > 0 && height > 0) damageNode(width, height) else null
            onDrawWithContent {
                val target = drawContext.canvas
                if (node == null || !target.nativeCanvas.isHardwareAccelerated) {
                    drawContent()
                    return@onDrawWithContent
                }
                drawContext.canvas = Canvas(damageNodeBeginRecording(node))
                try {
                    drawContent()
                } finally {
                    drawContext.canvas = target
                    damageNodeEndRecording(node)
                }
                target.nativeCanvas.drawDamageNode(node)
            }
        }
            // a plain layer inside, so content that draws itself repaints only this rectangle
            .graphicsLayer()
    }

actual fun Modifier.glassRoot(state: GlassState, isActive: () -> Boolean): Modifier = this
    .onGloballyPositioned { state.rootPosition = it.positionInWindow() }
    .drawWithCache {
        val width = size.width.toInt()
        val height = size.height.toInt()

        val node: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && width > 0 && height > 0) {
            glassNodeFor(state.renderNode, width, height)
        } else null

        state.renderNode = node

        onDrawWithContent {
            val drawContextCanvas = drawContext.canvas

            if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isActive()) {
                val nativeCanvas = glassNodeBeginRecording(node)
                val composeCanvas = Canvas(nativeCanvas)

                drawContext.canvas = composeCanvas
                drawContent()

                drawContext.canvas = drawContextCanvas
                glassNodeEndRecording(node)

                if (glassNodeHasContent(node)) {
                    drawIntoCanvas { it.nativeCanvas.drawGlassNode(node) }
                }
            } else {
                drawContent()
            }
        }
    }

internal actual fun discardGlassBackdrop(node: Any?) {
    if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) glassNodeDiscard(node)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun glassNodeDiscard(node: Any) {
    (node as RenderNode).discardDisplayList()
}

internal actual val glassBlurSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

// flagship chips that don't always declare a performance class; anything else waits for one
private val StrongGlassChips = listOf(
    "sm8350", "sm8450", "sm8475", "sm8550", "sm8635", "sm8650", "sm8750", // snapdragon 888 up to 8 elite
    "mt6983", "mt6985", "mt6989", "mt6991", // dimensity 9000 up to 9400
)

actual val DefaultBlurQuality: BlurQuality by lazy {
    val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL.lowercase(java.util.Locale.ROOT) else ""
    // high's glass took 11 to 15 ms of gpu per scroll frame on a mid-range 120 hz phone, more than a frame has
    val strong = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        (Build.VERSION.MEDIA_PERFORMANCE_CLASS >= Build.VERSION_CODES.S || StrongGlassChips.any { soc.startsWith(it) })
    if (strong) BlurQuality.HIGH else BlurQuality.LOW
}

/**
 * A [GlassTileMode] or an `android.graphics.Shader.TileMode` (what the app's older call sites pass),
 * as the platform enum. Only called above API 31, which is where the blur exists.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal fun Any?.asAndroidTileMode(default: android.graphics.Shader.TileMode): android.graphics.Shader.TileMode = when (this) {
    null -> default
    is android.graphics.Shader.TileMode -> this
    GlassTileMode.Clamp -> android.graphics.Shader.TileMode.CLAMP
    GlassTileMode.Decal -> android.graphics.Shader.TileMode.DECAL
    GlassTileMode.Mirror -> android.graphics.Shader.TileMode.MIRROR
    GlassTileMode.Repeat -> android.graphics.Shader.TileMode.REPEAT
    else -> default
}

/** The size a downscaled layout was given, before it was divided down. */
internal class FullSize {
    var width = 0
    var height = 0
}

/** Lays its content out at 1/[downscale] of the space it takes, from the top left. */
internal fun Modifier.downscaledLayout(downscale: () -> Int, full: FullSize): Modifier = layout { measurable, constraints ->
    val k = downscale().coerceAtLeast(1)
    val fullWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
    val fullHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
    full.width = fullWidth
    full.height = fullHeight
    val placeable = measurable.measure(
        Constraints.fixed((fullWidth + k - 1) / k, (fullHeight + k - 1) / k)
    )
    layout(fullWidth, fullHeight) { placeable.place(0, 0) }
}

/** The last masked glass effect, reused while its radius, size and mask stay the same. */
private class MaskedEffectCache {
    var radius = -1f
    var width = -1f
    var height = -1f
    var tileOrdinal = -1
    var mask: Brush? = null
    var effect: RenderEffect? = null
}

/**
 * The blur and the mask in one effect: the blurred content kept only where [mask] is opaque
 * (DstIn) - what drawing the glass into an offscreen layer and DstIn-ing the mask over it did,
 * without that extra layer.
 */
@RequiresApi(Build.VERSION_CODES.S)
private fun maskedBlurEffect(radius: Float, tileMode: android.graphics.Shader.TileMode, mask: android.graphics.Shader): RenderEffect {
    val content = if (radius > 0.5f) {
        android.graphics.RenderEffect.createBlurEffect(radius, radius, tileMode)
    } else {
        android.graphics.RenderEffect.createOffsetEffect(0f, 0f)
    }
    return android.graphics.RenderEffect.createBlendModeEffect(
        content,
        android.graphics.RenderEffect.createShaderEffect(mask),
        android.graphics.BlendMode.DST_IN,
    ).asComposeRenderEffect()
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
    if (growDown != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ProgressiveBlurBands(radius, mask, modifier, offscreen, growDown, growAcross, growRamp, draw)
        return
    }
    val currentRadius by rememberUpdatedState(radius)
    val currentOffscreen by rememberUpdatedState(offscreen)
    // A radius that animates only re-lays this out when it crosses into another resolution step.
    val low = LocalBlurQuality.current == BlurQuality.LOW
    val downscale by remember(low) { derivedStateOf { glassDownscale(currentRadius(), low) } }
    val maskCache = remember { MaskedEffectCache() }
    val full = remember { FullSize() }

    Box(
        modifier = modifier
            .clipToBounds()
            .then(
                // Without RenderEffect (below API 31) the mask is applied the old way.
                if (mask != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(brush = mask, blendMode = BlendMode.DstIn)
                        }
                } else Modifier
            )
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .downscaledLayout({ downscale }, full)
                .graphicsLayer {
                    val k = downscale.toFloat()
                    scaleX = k
                    scaleY = k
                    transformOrigin = TransformOrigin(0f, 0f)
                    val r = currentRadius() / k
                    renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val mode = tileMode.asAndroidTileMode(android.graphics.Shader.TileMode.DECAL)
                        if (mask is ShaderBrush) {
                            val quantized = if (r > 0.5f) BlurEffectCache.quantize(r) else 0f
                            val cache = maskCache
                            if (cache.effect == null || cache.radius != quantized || cache.width != size.width ||
                                cache.height != size.height || cache.tileOrdinal != mode.ordinal || cache.mask != mask
                            ) {
                                cache.effect = maskedBlurEffect(quantized, mode, mask.createShader(size))
                                cache.radius = quantized
                                cache.width = size.width
                                cache.height = size.height
                                cache.tileOrdinal = mode.ordinal
                                cache.mask = mask
                            }
                            cache.effect
                        } else {
                            BlurEffectCache.get(r, mode)
                        }
                    } else {
                        null
                    }
                    compositingStrategy = if (currentOffscreen?.invoke() == true) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                    clip = true
                }
        ) {
            val k = downscale.toFloat()
            val fullSize = Size(full.width.toFloat(), full.height.toFloat())
            scale(1f / k, 1f / k, pivot = Offset.Zero) { draw(fullSize) }
        }
    }
}

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
    var position by remember { mutableStateOf(Offset.Zero) }

    val (disableBlur) = rememberPreference(DisableBlurKey, defaultValue = false)
    val (blurStrengthPx) = rememberPreference(com.mochyy.mochymusic.constants.BlurStrengthPxKey, defaultValue = 25f)

    val currentBlurRadius by rememberUpdatedState(blurRadius)

    // The blur actually applied, clamped the way BlurEffectCache clamps it.
    val radius = {
        if (disableBlur) 0f
        else BlurEffectCache.effectiveRadius(currentBlurRadius() * (blurStrengthPx / 25f))
    }

    PlatformDownscaledBlur(
        radius = radius,
        tileMode = tileMode,
        mask = mask,
        growDown = progressive?.let { it == BlurDirection.TopToBottom },
        growRamp = progressiveRamp,
        modifier = modifier
            .then(if (shape != null) Modifier.clip(shape) else Modifier)
            .then(
                // no blur below Android 12, so nothing to read past the edges; and low quality
                // keeps to what's right under the glass, a much smaller area to blur
                if (bleed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    LocalBlurQuality.current == BlurQuality.HIGH
                ) {
                    Modifier.bleed { kotlin.math.ceil(radius()).toInt() }
                } else Modifier
            )
            .onGloballyPositioned { position = it.positionInWindow() },
    ) { fullSize ->
        if (foundationColor != null) {
            drawRect(color = foundationColor, size = fullSize)
        }
        if (!disableBlur) {
            val node = state.renderNode
            if (node != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && glassNodeHasContent(node)) {
                val relX = position.x - state.rootPosition.x
                val relY = position.y - state.rootPosition.y
                translate(left = -relX, top = -relY) {
                    drawIntoCanvas { it.nativeCanvas.drawGlassNode(node) }
                }
            }
        }
        if (tint != Color.Transparent) {
            drawRect(color = tint, size = fullSize)
        }
        val wash = accentWash?.invoke()
        if (wash != null && wash.alpha > 0f) {
            drawRect(color = wash, size = fullSize)
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
    // off, the bar is one even blur that stops at a hard edge with a hairline, a classic frosted bar instead of a fade
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
                // blurred from what's really past the edge, so the hard edge doesn't smear into a glow
                bleed = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }
    // low quality too: its stacked steps each drew and blurred the list again, three to five times per scroll frame
    val smooth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    if (smooth) {
        // one pass whose radius grows row by row, faded in where the first step used to fade in (low draws it in one band, see BandPlan)
        val fade = 1f / steps.coerceAtLeast(1)
        val mask = remember(direction, fade) {
            if (direction == BlurDirection.TopToBottom) {
                Brush.verticalGradient(0f to Color.Transparent, fade to Color.Black, 1f to Color.Black)
            } else {
                Brush.verticalGradient(0f to Color.Black, (1f - fade) to Color.Black, 1f to Color.Transparent)
            }
        }
        Box(modifier = modifier) {
            GlassPillBackground(
                state = state,
                blurRadius = maxBlurRadius,
                tint = tint,
                foundationColor = foundationColor,
                mask = mask,
                progressive = direction,
                modifier = Modifier.fillMaxSize(),
            )
        }
        return
    }
    Box(modifier = modifier) {
        for (i in 1..steps) {
            val fraction = i.toFloat() / steps

            val radiusProvider = { maxBlurRadius() * (fraction * fraction) }

            val fadeStart = (i - 1).toFloat() / steps
            val heightFraction = 1f - fadeStart

            if (heightFraction > 0f) {
                val internalFadeDistance = (1f / steps) / heightFraction
                // Each step fades in over its first slice. The mask used to be DstIn-ed over the
                // step in an offscreen layer of its own - one more full-width buffer per step,
                // every frame the bar's backdrop moved; it now runs inside the step's blur.
                val mask = remember(direction, internalFadeDistance) {
                    if (direction == BlurDirection.TopToBottom) {
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            internalFadeDistance to Color.Black,
                            1f to Color.Black
                        )
                    } else {
                        Brush.verticalGradient(
                            0f to Color.Black,
                            (1f - internalFadeDistance) to Color.Black,
                            1f to Color.Transparent
                        )
                    }
                }

                GlassPillBackground(
                    state = state,
                    blurRadius = radiusProvider,
                    tint = tint,
                    foundationColor = foundationColor,
                    mask = mask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightFraction)
                        .align(if (direction == BlurDirection.TopToBottom) Alignment.BottomCenter else Alignment.TopCenter)
                )
            }
        }
    }
}
