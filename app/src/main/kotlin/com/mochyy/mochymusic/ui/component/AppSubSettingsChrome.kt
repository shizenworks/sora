package com.mochyy.mochymusic.ui.component

import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import coil3.compose.rememberAsyncImagePainter
import com.mochyy.mochymusic.R

/** The settings sub-page header as Android draws it: GlassKit's frosted header, BlurEffectCache and the app's icons. */
object AppSubSettingsChrome : SubSettingsChrome {
    @Composable
    override fun rememberHeaderGlass(): HeaderGlass = remember { GlassKitHeaderGlass(GlassState()) }

    // DECAL is API 31. The argument is read before BlurEffectCache.get can return early, so naming it
    // outside this check threw NoSuchFieldError on Android 11 and older the first time the header
    // drew: every settings page crashed on opening. Below 31 there's no blur either way.
    override fun titleBlur(radius: Float): RenderEffect? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BlurEffectCache.element(radius, Shader.TileMode.DECAL)
        } else {
            null
        }

    @Composable
    override fun backIcon(): Painter = painterResource(R.drawable.arrow_back_ios)

    @Composable
    override fun closeIcon(): Painter = painterResource(R.drawable.close)

    @Composable
    override fun searchIcon(): Painter = rememberAsyncImagePainter(model = R.raw.search)
}

private class GlassKitHeaderGlass(private val state: GlassState) : HeaderGlass {
    override fun backdrop(isActive: () -> Boolean): Modifier = Modifier.glassRoot(state, isActive)

    @Composable
    override fun Background(
        maxBlurRadius: () -> Float,
        foundationColor: Color,
        direction: BlurDirection,
        steps: Int,
        modifier: Modifier,
    ) {
        ProgressiveGlassBackground(
            state = state,
            maxBlurRadius = maxBlurRadius,
            foundationColor = foundationColor,
            direction = direction,
            steps = steps,
            modifier = modifier,
        )
    }
}
