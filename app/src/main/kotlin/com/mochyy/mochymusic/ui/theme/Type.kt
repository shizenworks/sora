// Type.kt

package com.mochyy.mochymusic.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.shared.ui.theme.mochyTypography

/** mochy's Inter from the app's own font files, handed to the shared [AppFont] when the app starts */
val AndroidInter = FontFamily(
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

// Neiro's own type: SF Pro Display, the five cuts bundled with the ported players
val SfProFamily = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.Normal),
    Font(R.font.sf_pro_display_medium, FontWeight.Medium),
    Font(R.font.sf_pro_display_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_display_bold, FontWeight.Bold),
    Font(R.font.sf_pro_display_heavy, FontWeight.Black),
)

/** the app's text styles in [font], rebuilt when a look's font comes or goes; shared with desktop and iOS */
fun appTypography(font: FontFamily) = mochyTypography(font)
