package com.mochyy.mochymusic.ui.screens.recap

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.roundToInt

// The drawing pieces the story pages (and the page that hosts them) share: eases, the giant words'
// state, the cover palette, odometer numbers and gradient text. The Android-only parts of the same
// visuals - reading a palette out of a cover, the AGSL mesh - stay in :app next to their platform APIs.

fun easeOutCubic(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)
fun easeInCubic(t: Float): Float = t * t * t
fun easeInOutCubic(t: Float): Float =
    if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f

fun easeOutBack(t: Float, overshoot: Float = 1.5f): Float {
    val c3 = overshoot + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + overshoot * x * x
}

/** Item [index] of [count]'s share of a staggered 0..1 [progress]; each item runs for [length] of it. */
fun stagger(progress: Float, index: Int, count: Int, length: Float = 0.55f): Float {
    if (count <= 1) return progress.coerceIn(0f, 1f)
    val start = index * (1f - length) / (count - 1)
    return ((progress - start) / length).coerceIn(0f, 1f)
}

@Stable
class GiantRow(word: String, filled: Boolean) {
    var word by mutableStateOf(word)

    /** 0 = empty, 1 = full. Falling, it slides out to [side]; rising, it slides in from [side]. */
    val fill = Animatable(if (filled) 1f else 0f)
    var side by mutableFloatStateOf(1f)

    suspend fun slideOut(durationMs: Int = 520) {
        side = -1f
        fill.animateTo(0f, androidx.compose.animation.core.tween(durationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }

    suspend fun slideIn(newWord: String = word, durationMs: Int = 900) {
        if (fill.value <= 0.001f) {
            word = newWord
        } else if (newWord != word) {
            slideOut()
            word = newWord
        }
        side = 1f
        fill.animateTo(1f, androidx.compose.animation.core.tween(durationMs, easing = androidx.compose.animation.core.FastOutSlowInEasing))
    }
}

/**
 * Rows of one huge word, endlessly sliding. Rotated 90° the rows read as columns (the page header
 * shows only row 0, hugging the right edge); turning the plane back is the story's "warp" from
 * columns into rows. Rows are indexed -[half]..[half] from the plane's centre.
 */
@Stable
class GiantTextState(word: String, val half: Int = 4, filledRows: IntRange = 0..0) {
    val rows: List<GiantRow> = (-half..half).map { GiantRow(word, it in filledRows) }
    fun row(index: Int): GiantRow = rows[index + half]

    val rotation = Animatable(90f)

    /** Plane centre as a fraction of the width. */
    val centerX = Animatable(PageColumnCenterX)

    /** Plane centre's offset from the vertical middle, in px, screen space. */
    val offsetY = Animatable(0f)
    val scale = Animatable(1f)
    val alpha = Animatable(1f)

    /** Read in draw, so a colour made of animated values follows them without recomposing. */
    var color: () -> Color = { Color(0xFF272727) }

    companion object {
        const val PageColumnCenterX = 0.86f
    }
}

/** Four tones taken from a cover or an artist photo, dark enough that white text always reads. */
@Stable
class RecapPalette(val c0: Color, val c1: Color, val c2: Color, val c3: Color) {
    companion object {
        val Story = RecapPalette(Color(0xFF0C0C0C), Color(0xFF0C0C0C), Color(0xFF0C0C0C), Color(0xFF0C0C0C))

        /** The mock's red, for when a picture can't be read. */
        val Fallback = RecapPalette(Color(0xFF732C2A), Color(0xFF7A3B26), Color(0xFF4E1D1C), Color(0xFF6B3324))
    }
}

internal fun paletteOf(a: RecapPalette, b: RecapPalette, t: Float): RecapPalette = when {
    t <= 0f -> a
    t >= 1f -> b
    else -> RecapPalette(lerp(a.c0, b.c0, t), lerp(a.c1, b.c1, t), lerp(a.c2, b.c2, t), lerp(a.c3, b.c3, t))
}

/**
 * [text] whose digits roll into place like an odometer as [progress] goes 0..1, staggered left to
 * right, while letters rise in. Drawn glyph by glyph with tabular digits, so nothing reflows.
 */
@Composable
fun OdometerText(
    text: String,
    style: TextStyle,
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: () -> Color = { style.color },
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val tnum = remember(style) { style.copy(fontFeatureSettings = "tnum") }
    val full = remember(text, tnum) { measurer.measure(text, tnum, softWrap = false, maxLines = 1) }
    val lefts = remember(full) { FloatArray(text.length) { full.getBoundingBox(it).left } }
    val glyphs = remember(text, tnum) {
        val chars = (text.toSet() + ('0'..'9')).filter { it != ' ' }
        chars.associateWith { measurer.measure(it.toString(), tnum, softWrap = false, maxLines = 1) }
    }
    val width = with(density) { full.size.width.toDp() }
    val height = with(density) { full.size.height.toDp() }
    Spacer(
        modifier = modifier
            .size(width, height)
            .drawWithCache {
                val lineH = full.size.height.toFloat()
                onDrawBehind {
                    val p = progress().coerceIn(0f, 1f)
                    val c = color()
                    for (i in text.indices) {
                        val ch = text[i]
                        if (ch == ' ') continue
                        val local = stagger(p, i, text.length, length = 0.62f)
                        if (local <= 0f) continue
                        val x = lefts[i]
                        if (ch.isDigit()) {
                            val digit = ch - '0'
                            val spins = 1 + i % 3
                            val value = (spins * 10 + digit) * easeOutCubic(local)
                            val base = floor(value).toInt()
                            val frac = value - base
                            val fadeIn = (local * 5f).coerceAtMost(1f)
                            clipRect(left = x - 4f, top = 0f, right = x + (glyphs[ch]?.size?.width ?: 0) + 4f, bottom = lineH) {
                                glyphs['0' + base % 10]?.let { drawText(it, color = c, topLeft = Offset(x, -frac * lineH), alpha = (1f - frac) * fadeIn) }
                                if (frac > 0.001f) {
                                    glyphs['0' + (base + 1) % 10]?.let { drawText(it, color = c, topLeft = Offset(x, (1f - frac) * lineH), alpha = frac * fadeIn) }
                                }
                            }
                        } else {
                            val e = easeOutCubic(local)
                            glyphs[ch]?.let { drawText(it, color = c, topLeft = Offset(x, (1f - e) * lineH * 0.35f), alpha = e) }
                        }
                    }
                }
            },
    )
}

/**
 * Tints whatever it wraps (text) with a moving gradient of [colors], easing to plain white as
 * [whiteness] reaches 1. The offscreen layer only exists while the tint is showing.
 */
internal fun Modifier.gradientToWhite(colors: () -> List<Color>, whiteness: () -> Float, time: RecapTimeSource): Modifier = this
    // SrcIn has to blend into the text's own buffer: without one it lands on everything already
    // drawn under the text and fills its whole box (the white bar under the artist photo)
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .then(
        Modifier.drawWithContent {
            drawContent()
            val w = whiteness()
            if (w >= 0.999f) return@drawWithContent
            val shift = ((time.seconds * 0.35) % 1.0).toFloat() * size.width * 2f
            val tinted = colors().map { lerp(it, Color.White, w) }
            drawRect(
                Brush.linearGradient(
                    tinted + tinted.first(),
                    start = Offset(-size.width * 2f + shift, 0f),
                    end = Offset(shift, size.height),
                    tileMode = TileMode.Repeated,
                ),
                blendMode = BlendMode.SrcIn,
            )
        },
    )

/** The square dot pattern behind the page header and the story: one tiled shader, one draw. */
fun Modifier.recapDots(
    color: () -> Color,
    alpha: () -> Float = { 1f },
    spacing: Dp = 17.dp,
    radius: Dp = 1.3.dp,
): Modifier = drawWithCache {
    val step = spacing.toPx().roundToInt().coerceAtLeast(4)
    val tile = ImageBitmap(step, step)
    androidx.compose.ui.graphics.Canvas(tile).drawCircle(
        Offset(step / 2f, step / 2f),
        radius.toPx(),
        Paint().apply {
            this.color = Color.White
            isAntiAlias = true
        },
    )
    val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    var lastColor = Color.Unspecified
    var filter: ColorFilter? = null
    // behind the content: over the giant words (drawn before this), under any children
    onDrawBehind {
        val a = alpha()
        if (a <= 0.004f) return@onDrawBehind
        val c = color()
        if (c != lastColor) {
            lastColor = c
            filter = ColorFilter.tint(c, BlendMode.SrcIn)
        }
        drawRect(brush, alpha = a, colorFilter = filter)
    }
}
