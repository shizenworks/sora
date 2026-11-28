package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.constants.BlurQuality
import com.mochyy.mochymusic.constants.ElementBlurKey
import com.mochyy.mochymusic.utils.PreferencesCache

/**
 * The backdrop one glass root keeps for the surfaces drawn above it.
 *
 * [renderNode] is the platform's capture handle, deliberately typed [Any]: it is an
 * `android.graphics.RenderNode` on Android and stays null on desktop and iOS, where Compose has no
 * way to snapshot what is behind a composable; those surfaces draw their own frost instead (see
 * [GlassPillBackground]). Keeping the property as [Any] is also what lets the class reach Android 8
 * and 9: naming RenderNode in a property type would make ART resolve it on every accessor, and the
 * glass system backs the nav pill, the player and most screens.
 */
@Stable
class GlassState {
    var renderNode by mutableStateOf<Any?>(null)
    var rootPosition by mutableStateOf(Offset.Zero)

    // drops the recorded screen display list when the window hides so gpu textures can be reclaimed
    fun discard() {
        discardGlassBackdrop(renderNode)
    }
}

internal expect fun discardGlassBackdrop(node: Any?)

/**
 * Records what this composable draws as the backdrop its glass surfaces sample, and remembers where
 * it sits: [GlassPillBackground] draws the recording back, shifted, under its blur.
 *
 * Android records into a RenderNode (API 29+, drawn from API 31+). Desktop and iOS have no live
 * screen capture, so this only keeps the position; their glass draws a translucent frost of its own.
 */
expect fun Modifier.glassRoot(state: GlassState, isActive: () -> Boolean = { true }): Modifier

/**
 * Keeps whatever animates inside this from counting as a change to the rest of the screen.
 *
 * To the renderer, a change inside a Compose layer is a change to everything (Compose's layers clip
 * by outline, and the renderer only bounds a change by a node's size when it clips to bounds).
 * Android records this into a clip-to-bounds RenderNode so only this rectangle is dirtied; the other
 * platforms' compositors already bound the damage, so it is a no-op there.
 */
expect fun Modifier.damageBoundary(): Modifier

/**
 * Backdrop colour for the floating glass chrome - the navigation pill and the mini player.
 *
 * Deliberately a fixed dark tone rather than `MaterialTheme.colorScheme.surfaceContainer`: the
 * Material You colour follows the wallpaper accent and washes these surfaces out. They are drawn
 * with white icons over a white hairline border, so they are meant to read as dark glass. The
 * shade is the Figma's.
 */
val GlassChromeColor = Color(0xFF131313)

/** Backdrop blur for that chrome, heavier than the 24f default so it reads as properly frosted. */
const val GlassChromeBlurRadius = 48f

/**
 * How far below full resolution a glass surface draws what's under it before blurring.
 *
 * A blur this wide removes everything a full-resolution picture of the backdrop adds - Skia
 * itself shrinks the picture by halves before blurring a large radius - so drawing the backdrop
 * at half (or quarter) size and blurring it by a half (or quarter) radius gives the same glass.
 * What it saves is drawing all of the backdrop that sits under the glass at full resolution, plus
 * the first passes of the blur, every frame the glass or what's under it moves. Small radii keep
 * full resolution: there the detail still shows.
 */
internal fun glassDownscale(effectiveRadius: Float, low: Boolean = false): Int = when {
    effectiveRadius >= 64f -> 4
    // low quality drops a step sooner: a little softer, a quarter of the pixels to draw and blur
    low && effectiveRadius >= 32f -> 4
    effectiveRadius >= 16f -> 2
    // the faint first steps of a low progressive bar are its biggest, and drawn at full size they cost the most for a blur you can hardly see
    low && effectiveRadius >= 2f -> 2
    else -> 1
}

/** The blur quality chosen in Appearance, for every glass surface under it. */
val LocalBlurQuality = staticCompositionLocalOf { DefaultBlurQuality }

/** The quality a phone starts on until someone picks one in Customisation. Android reads the SoC; desktop and iOS start on high. */
expect val DefaultBlurQuality: BlurQuality

/** Whether this platform can blur the glass at all (Android 12 and up; always on desktop and iOS). */
internal expect val glassBlurSupported: Boolean

/** Lays its content out [margin] px bigger on every side, centred over the space it takes. */
fun Modifier.bleed(margin: () -> Int): Modifier = layout { measurable, constraints ->
    val m = margin().coerceAtLeast(0)
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
    val height = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
    val placeable = measurable.measure(Constraints.fixed(width + m * 2, height + m * 2))
    layout(width, height) { placeable.place(-m, -m) }
}

/**
 * How a glass blur repeats past the edge of its picture.
 *
 * Android call sites predate shared code and pass `android.graphics.Shader.TileMode` directly; the
 * parameter is typed [Any] and the Android side reads both forms, so those call sites stay as they
 * are. Null means "the platform's glass default": DECAL on Android, Decal elsewhere.
 */
@Suppress("unused")
enum class GlassTileMode { Clamp, Decal, Mirror, Repeat }

/**
 * A blur of the recorded backdrop, drawn under [tint] and [foundationColor].
 *
 * [progressive] grows the blur from sharp at the near edge to [blurRadius] at the far one (Android
 * 13+; the other platforms fade the frost in over the same stretch instead).
 *
 * [tileMode] takes a [GlassTileMode] or, on Android, an `android.graphics.Shader.TileMode`.
 */
@Composable
expect fun GlassPillBackground(
    state: GlassState,
    blurRadius: () -> Float = { 24f },
    tint: Color = Color.Transparent,
    foundationColor: Color? = null,
    shape: Shape? = null,

    /**
     * Null means DECAL on Android, resolved inside the API 31 guard: `Shader.TileMode.DECAL` is API
     * 31, and naming it as a default argument makes every caller that omits the parameter evaluate
     * it - throwing NoSuchFieldError on Android 8 through 11. The value is only ever *used* above
     * API 31, so it is resolved there instead.
     */
    tileMode: Any? = null,
    /**
     * Fades the finished glass (backdrop, foundation and tint alike) by this brush's alpha, as a
     * DstIn would. On API 31+ the mask runs inside the blur's own effect; it used to need an
     * offscreen layer of its own around the glass.
     */
    mask: Brush? = null,
    /** a colour laid over [tint], read only while drawing so it can animate without recomposing */
    accentWash: (() -> Color)? = null,
    /**
     * Also blur a margin of the backdrop as wide as the blur around the surface, so the edges are
     * blurred from what's really past them. Without it the blur has to make that part up, and a
     * clamped edge row smears anything bright touching it across the surface. The extra is drawn
     * outside the bounds, so the parent has to clip.
     */
    bleed: Boolean = false,
    // grows the blur from sharp at the start to blurRadius at the far end, in one pass (android 13+)
    progressive: BlurDirection? = null,
    // how far along the progressive blur reaches its full radius (1 is the far end), holding it from there on
    progressiveRamp: Float = 1f,
    modifier: Modifier = Modifier,
)

// the hairline where a flat bar's blur stops, when progressive blur is off
internal val FlatBarEdgeColor = Color.White.copy(alpha = 0.16f)
internal val FlatBarEdgeWidth = 1.dp

/**
 * The bar's glass: a progressive blur from the bar's edge, or - with progressive blur off - one even
 * frost with a hairline at that edge.
 */
@Composable
expect fun ProgressiveGlassBackground(
    state: GlassState,
    maxBlurRadius: () -> Float = { 24f },
    tint: Color = Color.Transparent,
    foundationColor: Color? = null,
    direction: BlurDirection = BlurDirection.TopToBottom,
    modifier: Modifier = Modifier,
    steps: Int = 5,
)

/**
 * Whatever [draw] draws, blurred by [radius] (the radius actually applied - see
 * [BlurEffectCache.effectiveRadius]) and optionally faded by [mask] (DstIn).
 *
 * Android draws it at the resolution [glassDownscale] picks for the radius: into a layer 1/k the
 * size, blurred by radius/k and scaled back up - see [glassDownscale] for why that's the same
 * picture for less work. [draw] works in full-size coordinates and gets the full size; the scaling
 * is done by the platform. Desktop and iOS blur at full resolution; there [growDown] is a plain
 * blur, as Skia's glass has no variable-radius pass yet.
 */
@Composable
fun DownscaledBlur(
    radius: () -> Float,
    tileMode: Any?,
    mask: Brush?,
    modifier: Modifier = Modifier,
    /** Draw into a buffer of its own even without a blur, for a [draw] that erases with blend modes. */
    offscreen: (() -> Boolean)? = null,
    // set to grow the blur from sharp to radius across the height (true: strongest at the bottom), android 13+ only
    growDown: Boolean? = null,
    // grows across the width instead (growDown true: strongest at the right), full strength ramp of the way along
    growAcross: Boolean = false,
    growRamp: Float = 1f,
    draw: DrawScope.(fullSize: Size) -> Unit,
) = PlatformDownscaledBlur(radius, tileMode, mask, modifier, offscreen, growDown, growAcross, growRamp, draw)

/**
 * The platform half of [DownscaledBlur]. A function rather than an expect composable because the
 * draw lambda is receiver-scoped; the Android and Skia files implement it.
 */
@Composable
internal expect fun PlatformDownscaledBlur(
    radius: () -> Float,
    tileMode: Any?,
    mask: Brush?,
    modifier: Modifier = Modifier,
    offscreen: (() -> Boolean)? = null,
    growDown: Boolean? = null,
    growAcross: Boolean = false,
    growRamp: Float = 1f,
    draw: DrawScope.(fullSize: Size) -> Unit,
)

/** Whether element blurs (lyric lines, morphing titles, page wipes) are on: the same switch the app mirrors into AppMotion. */
internal fun elementBlurEnabled(): Boolean = PreferencesCache.snapshot.value[ElementBlurKey] ?: true
