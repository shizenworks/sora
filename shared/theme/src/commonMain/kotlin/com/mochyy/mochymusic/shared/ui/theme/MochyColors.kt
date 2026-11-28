// MochyColors.kt
// Mochy's palette (from Mochy Music by realidkroo), shared by every platform.

package com.mochyy.mochymusic.shared.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * mochy renders dark-only. The light palette was never finished - large parts of the UI hardcode white
 * text and dark glass - so it is pinned as one switch instead of being decided at every call site.
 */
const val ForceDarkTheme = true

/** Null when the default gray theme is active. Custom controls can use the generated accent. */
val LocalMochyAccent = compositionLocalOf<Color?> { null }

val MochyAccentPresets = listOf(
    0xFFFF6B6B.toInt(),
    0xFFFFB300.toInt(),
    0xFF9CCC65.toInt(),
    0xFF26C6DA.toInt(),
    0xFF42A5F5.toInt(),
    0xFFAB78FF.toInt(),
    0xFFF06292.toInt(),
)

/** Mochy's base scheme; the light one exists but the app never turns it on, see [ForceDarkTheme]. */
fun mochyColorScheme(darkTheme: Boolean, accent: Color? = null): ColorScheme =
    if (accent != null) {
        dynamicColorScheme(seedColor = accent, isDark = darkTheme, style = PaletteStyle.TonalSpot)
    } else if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFFE0E0E0),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF333333),
            onPrimaryContainer = Color.White,
            secondary = Color(0xFFB0B0B0),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF2C2C2C),
            onSecondaryContainer = Color.White,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1A1A1A),
            onSurfaceVariant = Color.White,
            surfaceTint = Color.Black
        )
    } else {
        lightColorScheme(
            primary = Color.Black,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E0E0),
            onPrimaryContainer = Color.Black,
            secondary = Color(0xFF333333),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFCCCCCC),
            onSecondaryContainer = Color.Black,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            surfaceVariant = Color(0xFFF0F0F0),
            onSurfaceVariant = Color.Black,
            surfaceTint = Color.White
        )
    }

fun ColorScheme.pureBlack(apply: Boolean): ColorScheme =
    if (apply) copy(surface = Color.Black, background = Color.Black) else this

/**
 * The shared theme owns all Material roles so the selected palette reaches every screen.
 */
@Composable
fun MochySharedTheme(
    font: FontFamily,
    darkTheme: Boolean = ForceDarkTheme,
    pureBlack: Boolean = false,
    accent: Color? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme, pureBlack, accent) {
        mochyColorScheme(darkTheme, accent).pureBlack(darkTheme && pureBlack)
    }
    val typography = remember(font) { mochyTypography(font) }
    CompositionLocalProvider(LocalMochyAccent provides accent?.let { colorScheme.primary }) {
        MaterialTheme(colorScheme = colorScheme, typography = typography) {
            CompositionLocalProvider(LocalContentColor provides colorScheme.onSurface) {
                ProvideTextStyle(value = typography.bodyLarge, content = content)
            }
        }
    }
}
