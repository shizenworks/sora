// AppFont.kt
// the font the whole app's text is set in, on every platform: a look's font, the user's own font file,
// the system font or mochy's Inter, in that order

package com.mochyy.mochymusic.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily

/** what the app's font is picked from; every field is a state, so text set in [InterFontFamily] follows a change */
object AppFont {
    /** a look module's font, standing in for everything else while that look is on; null when no look brings one */
    var look: FontFamily? by mutableStateOf(null)

    /** the font file the user picked in settings, loaded; null when there is none */
    var custom: FontFamily? by mutableStateOf(null)

    /** the phone's (or computer's) own font instead of Inter */
    var useSystem by mutableStateOf(false)

    /** mochy's Inter as this platform loads it, handed in at start-up; the system font until then */
    var inter: FontFamily by mutableStateOf(FontFamily.Default)
}

/** a look's font > the custom font > the system font > Inter */
internal fun <T> pickAppFont(look: T?, custom: T?, useSystem: Boolean, system: T, inter: T): T =
    look ?: custom ?: if (useSystem) system else inter

// read through a state, so text set in it follows a look's font, a custom font, or the choice, turning on or off
val InterFontFamily: FontFamily
    get() = pickAppFont(AppFont.look, AppFont.custom, AppFont.useSystem, FontFamily.Default, AppFont.inter)

/** the biggest font file the app copies in; a real font is well under this */
const val MaxCustomFontBytes: Long = 20L * 1024 * 1024

/** whether a picked file of [size] bytes may be used; an unknown size (null or negative) is checked while copying */
fun customFontSizeAllowed(size: Long?): Boolean = size == null || size < 0 || size <= MaxCustomFontBytes
