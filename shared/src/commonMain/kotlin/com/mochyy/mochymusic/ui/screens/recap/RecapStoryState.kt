package com.mochyy.mochymusic.ui.screens.recap

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// What the story is about, as the pages read it. The app's RecapStoryData implements this so the
// pages and the host can keep their own types: the pages need the numbers, the host also has the
// account photo and the java.time joined date to format.
interface RecapStorySource {
    val stats: ListeningStats
    val monthProgress: Int
    val name: String

    /** "October" - or "2026" when a forced recap had to fall back to the whole year. */
    val monthName: String
    val artists: List<RecapArtist>
    val songs: List<RecapSong>
    val heroArtist: RecapArtist?
    val heroSong: RecapSong?
    val favouriteDay: Int
    val favouriteDayMs: Long
    val runnerUpMs: Long

    /** The joined date already spelled the way the journey page shows it. */
    val joinedDateText: String
    val allTimeMs: Long
}

/** The running story clock, as the pages read it. The app's RecapTime is one. */
interface RecapTimeSource {
    val seconds: Double

    /** Seconds wrapped to a small range, for sin/cos sway where float precision matters more than continuity. */
    val sway: Float
}

const val StoryPageCount = 11
const val P_HEY = 0
const val P_JOURNEY = 1
const val P_MONTH = 2
const val P_WEEKDAY = 3
const val P_ARTISTS = 4
const val P_ARTIST = 5
const val P_SONGS = 6
const val P_MELODY = 7
const val P_SONG = 8
const val P_RECEIPT = 9
const val P_END = 10

val StoryDark = Color(0xFF0C0C0C)
val HeaderGrey = Color(0xFF1C1C1C)
val StoryTextDark = Color(0xFF111111)
val HeaderText = Color(0xFF272727)
val StoryDots = Color(0xFF171717)
val HeaderDots = Color(0xFF262626)

suspend fun Animatable<Float, AnimationVector1D>.go(target: Float, ms: Int, easing: Easing = Emphasized, delayMs: Int = 0) {
    animateTo(target, tween(ms, delayMs, easing))
}

/** Runs [blocks] at the same time and only returns when all of them have. */
suspend fun together(vararg blocks: suspend CoroutineScope.() -> Unit) = coroutineScope {
    blocks.forEach { launch(block = it) }
}

@Stable
class PageAnim {
    val enter = Animatable(0f)
    val exit = Animatable(0f)
}

/** Sizes on the actual screen, plus a mapping from the 852dp-tall mock. */
@Immutable
class StoryGeometry(val width: Float, val height: Float, val top: Float, val density: Float) {
    fun dp(v: Float): Float = v * density

    /** A y from the mock (an 852dp-tall phone), proportionally on this one. */
    fun fy(mockDp: Float): Float = mockDp / 852f * height

    val inset: Float get() = dp(36f)

    /** Avatar centre x, centre y and diameter for a page. */
    fun avatar(page: Int): FloatArray = when (page) {
        P_HEY -> floatArrayOf(width / 2f, height * 0.43f, dp(127f))
        P_JOURNEY -> slot(dp(33f), top + dp(95f), dp(91f))
        P_MONTH -> slot(dp(33f), top + dp(106f), dp(143f))
        P_WEEKDAY -> slot(dp(33f), top + dp(91f), dp(85f))
        else -> slot(inset, top + dp(31f), dp(36f))
    }

    private fun slot(left: Float, top: Float, d: Float) = floatArrayOf(left + d / 2f, top + d / 2f, d)
}

/**
 * Every animated value that outlives a single page.
 *
 * [cardAspect] is the ID card's width/height the End page measures the card with, and [elementBlur]
 * is the platform's element blur (Android: BlurEffectCache.element), so a page can fade a group out
 * without either platform's helper moving here.
 */
@Stable
class StoryState(
    val geo: StoryGeometry,
    startCard: Rect,
    val cardAspect: Float,
    val elementBlur: (Float) -> RenderEffect?,
) {
    val reveal = Animatable(0f)
    val backdrop = Animatable(1f)

    /** 1 = the whole screen; 0 = closed down to a horizontal band. */
    val band = Animatable(1f)

    /** 0 = the whole screen; 1 = clipped to the page header (the way back). */
    val headerClip = Animatable(0f)

    /** Colours: 0 = story dark, 1 = page header grey. */
    val toHeader = Animatable(0f)
    val mesh = Animatable(0f)
    val dots = Animatable(1f)
    var paletteFrom by mutableStateOf(RecapPalette.Fallback)
    var paletteTo by mutableStateOf(RecapPalette.Fallback)
    val paletteMix = Animatable(1f)

    val text = GiantTextState("MOCHY", half = 4, filledRows = 0..0).also { it.color = { textColor() } }
    val logoSide = Animatable(0f)

    val cardX = Animatable(startCard.left)
    val cardY = Animatable(startCard.top)
    val cardRot = Animatable(0f)
    val cardAlpha = Animatable(1f)
    var cardBack by mutableStateOf(true)
    val scan = Animatable(0f)

    val avX = Animatable(geo.avatar(P_HEY)[0])
    val avY = Animatable(geo.avatar(P_HEY)[1])
    val avD = Animatable(geo.avatar(P_HEY)[2])
    val avRing = Animatable(0f)
    val avRingAlpha = Animatable(1f)
    val avAlpha = Animatable(0f)
    val avPop = Animatable(0f)

    val pages = List(StoryPageCount) { PageAnim() }
    var page by mutableIntStateOf(-1)
    var swipeOpen by mutableStateOf(false)
    var dragX by mutableFloatStateOf(0f)

    // artists
    val spin = Animatable(-2.6f)
    val spinGlow = Animatable(0f)
    val heroArtist = Animatable(0f)

    // songs
    val gridDrift = Animatable(0f)
    val gridPick = Animatable(0f)
    val heroCover = Animatable(0f)
    val heroCoverUp = Animatable(0f)

    fun palette(): RecapPalette = paletteOf(paletteFrom, paletteTo, paletteMix.value)

    fun backgroundColor(): Color = lerp(StoryDark, HeaderGrey, toHeader.value)

    fun textColor(): Color {
        val dark = lerp(StoryTextDark, HeaderText, toHeader.value)
        return lerp(dark, Color.White.copy(alpha = 0.06f), mesh.value)
    }

    fun dotColor(): Color = lerp(StoryDots, HeaderDots, toHeader.value)

    suspend fun crossfadePalette(to: RecapPalette, ms: Int) {
        paletteFrom = palette()
        paletteTo = to
        paletteMix.snapTo(0f)
        paletteMix.go(1f, ms, FastOutSlowInEasing)
    }

    suspend fun avatarTo(page: Int, ms: Int = 900, delayMs: Int = 0) = together(
        { avX.go(geo.avatar(page)[0], ms, delayMs = delayMs) },
        { avY.go(geo.avatar(page)[1], ms, delayMs = delayMs) },
        { avD.go(geo.avatar(page)[2], ms, delayMs = delayMs) },
    )
}
