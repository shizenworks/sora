package com.mochyy.mochymusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.nav_search
import org.jetbrains.compose.resources.painterResource

/**
 * What a settings sub-page's header draws each platform's own way. Android provides GlassKit's frosted
 * header, its blur cache and the app's own icons (app: AppSubSettingsChrome); desktop and iOS provide
 * [PlainSubSettingsChrome]. No default, the same as LocalPlayerAwareWindowInsets: a screen that forgets
 * it fails at once instead of quietly losing the glass.
 */
val LocalSubSettingsChrome = staticCompositionLocalOf<SubSettingsChrome> { error("No SubSettingsChrome provided") }

@Stable
interface SubSettingsChrome {
    /** made once per page: the scrolling content as a backdrop, and the frosted header drawn from it */
    @Composable
    fun rememberHeaderGlass(): HeaderGlass

    /** the blur on the title while it morphs into the bar; null for none */
    fun titleBlur(radius: Float): RenderEffect?

    @Composable
    fun backIcon(): Painter

    @Composable
    fun closeIcon(): Painter

    @Composable
    fun searchIcon(): Painter
}

@Stable
interface HeaderGlass {
    /** goes on the scrolling content, which the header frosts */
    fun backdrop(isActive: () -> Boolean): Modifier

    @Composable
    fun Background(
        maxBlurRadius: () -> Float,
        foundationColor: Color,
        direction: BlurDirection,
        steps: Int,
        modifier: Modifier,
    )
}

/** Desktop and iOS: a translucent header over the content, with Compose's own blur on the morphing title. */
object PlainSubSettingsChrome : SubSettingsChrome {
    @Composable
    override fun rememberHeaderGlass(): HeaderGlass = PlainHeaderGlass

    override fun titleBlur(radius: Float): RenderEffect? =
        if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Decal) else null

    @Composable
    override fun backIcon(): Painter = rememberVectorPainter(BackIcon)

    @Composable
    override fun closeIcon(): Painter = rememberVectorPainter(CloseIcon)

    // the same glyph as the app's raw/search.svg; desktop and iOS draw svg resources, Android doesn't
    @Composable
    override fun searchIcon(): Painter = painterResource(Res.drawable.nav_search)
}

private object PlainHeaderGlass : HeaderGlass {
    override fun backdrop(isActive: () -> Boolean): Modifier = Modifier

    @Composable
    override fun Background(
        maxBlurRadius: () -> Float,
        foundationColor: Color,
        direction: BlurDirection,
        steps: Int,
        modifier: Modifier,
    ) {
        Box(modifier.background(foundationColor))
    }
}

// app/src/main/res/drawable/arrow_back_ios.xml and close.xml name @android:color/white, so the
// ResourceBridge leaves them out; these are their paths
private val BackIcon: ImageVector by lazy {
    icon("arrow_back_ios") {
        moveTo(15f, 18f)
        lineTo(9f, 12f)
        lineToRelative(6f, -6f)
    }
}

private val CloseIcon: ImageVector by lazy {
    icon("close") {
        moveTo(18f, 6f)
        lineTo(6f, 18f)
        moveTo(6f, 6f)
        lineToRelative(12f, 12f)
    }
}

private fun icon(name: String, pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathData,
    ).build()
