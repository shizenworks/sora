// LikedCover.kt
//
// Liked songs' cover, the same everywhere it shows (Home, the Library, playlist lists, the Liked
// page itself): the colours of the four songs liked last, blended softly corner to corner, with a
// white heart on top. it's drawn rather than a picture, so it's a handful of gradients at any size
// and doesn't need blur, which older phones can't do. LikedCoverImage (Android) saves the same
// drawing as a picture for the places that only take an image link.

package com.mochyy.mochymusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.favorite_border
import com.mochyy.mochymusic.ui.theme.blendARGB
import com.mochyy.mochymusic.ui.theme.coverThemeDarken
import com.mochyy.mochymusic.ui.theme.likedCoverColor
import com.mochyy.mochymusic.ui.theme.likedCoverShade
import com.mochyy.mochymusic.ui.utils.readCoverPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.concurrent.Volatile

object LikedCoverColors {
    /** before anything is liked, or a cover can't be read: the cover from the design */
    val Default = listOf(Color(0xFF6E3F73), Color(0xFF9B6A80), Color(0xFF8C3B3E), Color(0xFF6B7438))

    private val cache = LinkedHashMap<String, Int>()
    private val cacheLock = Mutex()

    /** the last set worked out, so a second copy of the cover (the page it opens) starts on it */
    @Volatile
    var last: List<Color> = Default
        private set

    /** the colours for these covers, top-left, top-right, bottom-left, bottom-right */
    suspend fun of(context: PlatformContext, urls: List<String>): List<Color> {
        val read = urls.take(4).mapNotNull { extract(context, it) }
        val colors = when {
            read.isEmpty() -> Default
            // fewer than four liked: the ones there are fill the rest, a shade apart so it isn't flat
            else -> List(4) { i -> read.getOrNull(i) ?: likedCoverShade(read[i % read.size], if (i % 2 == 0) -0.08f else 0.07f) }
        }
        last = colors
        return colors
    }

    /** the Liked page's theme colour: the bottom of the cover, pushed dark like a sampled cover is */
    fun theme(colors: List<Color>): Color {
        val bottom = blendARGB(colors[2].toArgb(), colors[3].toArgb(), 0.5f)
        return Color(coverThemeDarken(bottom))
    }

    private suspend fun extract(context: PlatformContext, url: String): Color? {
        val known = cacheLock.withLock { cache[url] }
        if (known != null) return Color(known)
        return try {
            val palette = readCoverPalette(
                context = context,
                url = url,
                size = 112,
                maximumColorCount = 12,
                resizeArea = 0,
                fallbackArgb = 0,
            ) ?: return null
            val color = palette.likedCoverColor() ?: return null
            cacheLock.withLock {
                if (cache.size >= 64) cache.remove(cache.keys.first())
                cache[url] = color.toArgb()
            }
            color
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

/** the colours of the four songs liked last; each cover is read once and remembered */
@Composable
fun rememberLikedCoverColors(): List<Color> {
    val database = koinInject<MochyDatabase>()
    val context = LocalPlatformContext.current
    val urls by remember(database) { database.songDao.likedCovers() }.collectAsState(initial = null)
    var colors by remember { mutableStateOf(LikedCoverColors.last) }
    LaunchedEffect(urls) {
        urls?.let { colors = LikedCoverColors.of(context, it) }
    }
    return colors
}

// where each colour sits: just inside its corner, so the four run into each other
internal val LikedCoverAnchors = listOf(Offset(0.1f, 0.08f), Offset(0.94f, 0.14f), Offset(0.06f, 0.9f), Offset(0.92f, 0.94f))

/** the four averaged, under everything, so no corner of it is ever left bare */
internal fun likedCoverBase(corners: List<Color>): Color =
    corners.fold(0 to Color.Transparent) { (n, acc), c ->
        (n + 1) to Color(blendARGB(acc.toArgb(), c.toArgb(), 1f / (n + 1)))
    }.second

// Android keeps its native-canvas drawing (LikedCover.android.kt); desktop and iOS draw the same
// gradients with Compose brushes. Built once per size and set of colours inside drawWithCache, so the
// native paints and gradients aren't re-created on every redraw.
internal expect fun CacheDrawScope.likedCoverDrawer(corners: List<Color>): DrawScope.() -> Unit

/** The desktop/iOS drawing: the same corner gradients, through Compose instead of android.graphics. */
internal fun DrawScope.drawLikedCoverSkia(corners: List<Color>) {
    drawRect(color = likedCoverBase(corners))
    corners.forEachIndexed { i, color ->
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to color,
                    0.36f to color.copy(alpha = 0.72f),
                    1f to color.copy(alpha = 0f),
                ),
                center = Offset(size.width * LikedCoverAnchors[i].x, size.height * LikedCoverAnchors[i].y),
                radius = maxOf(size.width, size.height) * 0.9f,
                tileMode = TileMode.Clamp,
            )
        )
    }
    // a little darker towards the edges, which gives it depth and keeps the heart on top
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.4f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.22f),
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = maxOf(size.width, size.height) * 0.78f,
            tileMode = TileMode.Clamp,
        )
    )
}

/**
 * the cover itself; it fills [modifier]'s size and is clipped by the caller. [heartSize] is the
 * heart's width against the cover's.
 */
@Composable
fun LikedCover(
    modifier: Modifier = Modifier,
    heartSize: Float = 0.4f,
    heartAlignment: Alignment = Alignment.Center,
) {
    val colors = rememberLikedCoverColors()
    val c0 by animateColorAsState(colors[0], tween(600), label = "likedCover0")
    val c1 by animateColorAsState(colors[1], tween(600), label = "likedCover1")
    val c2 by animateColorAsState(colors[2], tween(600), label = "likedCover2")
    val c3 by animateColorAsState(colors[3], tween(600), label = "likedCover3")

    Box(
        contentAlignment = heartAlignment,
        modifier = modifier.drawWithCache {
            val draw = likedCoverDrawer(listOf(c0, c1, c2, c3))
            onDrawBehind(draw)
        },
    ) {
        Icon(
            painter = painterResource(Res.drawable.favorite_border),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .fillMaxWidth(heartSize)
                .aspectRatio(1f),
        )
    }
}
