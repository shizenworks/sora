// Theme.kt

package com.mochyy.mochymusic.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.materialkolor.score.Score
import com.mochyy.mochymusic.shared.ui.theme.ForceDarkTheme as SharedForceDarkTheme
import com.mochyy.mochymusic.shared.ui.theme.LocalMochyAccent
import com.mochyy.mochymusic.shared.ui.theme.MochySharedTheme
import com.mochyy.mochymusic.shared.ui.theme.pureBlack as sharedPureBlack

val DefaultThemeColor = Color(0xFF8E8E8E)

/** mochy renders dark-only; the switch and the palette live in :shared so desktop and iOS match. */
const val ForceDarkTheme = SharedForceDarkTheme

@Composable
fun MochyTheme(
    darkTheme: Boolean = ForceDarkTheme,
    pureBlack: Boolean = false,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    MochySharedTheme(InterFontFamily, darkTheme, pureBlack, accent, content)
}

/** the accent the chrome is using, or null for mochy's own grey; controls that take the accent read it */
val LocalChromeAccent = LocalMochyAccent

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) = sharedPureBlack(apply)

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
