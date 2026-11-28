// colours and type shared by the dev spaces page

package com.mochyy.mochymusic.ui.screens.settings.devspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.ui.theme.InterFontFamily

// the mock's measures: 34dp page sides, 24dp cards
internal val DevSideInset = 34.dp
internal val DevCardCorner = 24.dp
internal val DevCardPad = 20.dp

internal val DevCard = Color(0xFF1C1C1E)
internal val DevRow = Color(0xFF2C2C2E)
internal val DevMuted = Color(0xFF8E8E93)
internal val DevPill = Color(0xFF3A3A3C)
internal val DevRule = Color.White.copy(alpha = 0.16f)
internal val DevLinkText = Color(0xFFAEAEB2)

internal fun devText(
    size: Float,
    weight: FontWeight = FontWeight.Bold,
    color: Color = Color.White,
    tracking: Float = -0.4f,
) = TextStyle(fontFamily = InterFontFamily, fontWeight = weight, fontSize = size.sp, letterSpacing = tracking.sp, color = color)

/** a big figure's size in dp, so it follows the card's width rather than the font-size setting */
@Composable
internal fun devFigureSp(dp: Float): TextUnit = with(LocalDensity.current) { dp.dp.toSp() }

/** opens a link; a device with no browser is not worth a crash, the same as the app's Context.openDevUrl */
internal fun UriHandler.openDevLink(url: String) {
    val clean = url.trim()
    // only web links: never let a module or a bad link open another kind of address
    if (!clean.startsWith("https://", ignoreCase = true) && !clean.startsWith("http://", ignoreCase = true)) return
    runCatching { openUri(clean) }
}

// Context.openDevUrl stays in :app (app/.../devspace/OpenDevUrl.kt): it is an Android Intent. The
// screens here open links through Compose's LocalUriHandler.
