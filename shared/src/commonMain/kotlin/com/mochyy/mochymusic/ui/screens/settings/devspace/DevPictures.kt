package com.mochyy.mochymusic.ui.screens.settings.devspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter

/** which person's picture to draw; named, because the files stay in the platform's own resources */
enum class DevPicture { Idkroo, Keycii }

/**
 * The people's photos. The two files are in app/src/main/res/drawable-nodpi, which the ResourceBridge
 * leaves out (AboutSheet reads them as R.drawable), so the platform loads them: Android through Coil,
 * exactly as the dev space always did (app: AppDevPictures). The others draw the mock's greys.
 */
@Stable
interface DevPictures {
    /** someone's picture, round */
    @Composable
    fun avatar(picture: DevPicture): Painter

    /** the page banner: the same picture, blurred small, for a backdrop */
    @Composable
    fun banner(picture: DevPicture): Painter
}

/** For platforms with no access to the app's drawables: the mock's greys, no photo. */
object PlainDevPictures : DevPictures {
    @Composable
    override fun avatar(picture: DevPicture): Painter = AvatarPlaceholder

    @Composable
    override fun banner(picture: DevPicture): Painter = BannerPlaceholder
}

private val AvatarPlaceholder: Painter = ColorPainter(Color(0xFFD9D9D9))
private val BannerPlaceholder: Painter = ColorPainter(Color(0xFF4A4A4A))

/** provided next to LocalSubSettingsChrome on Android (MainActivity, TvShell); other platforms get the plain one */
val LocalDevPictures = staticCompositionLocalOf<DevPictures> { PlainDevPictures }
