// MochyTypography.kt
// Mochy's text styles (Mochy Music by realidkroo, github.com/realidkroo/mochy-music), shared so the
// Android, desktop and iOS apps set type the same way. Each app passes its own Inter family in.

package com.mochyy.mochymusic.shared.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

private val GlobalFontWeight = FontWeight.SemiBold
private val GlobalLetterSpacing = (-0.05).em

/** the app's text styles in [font], rebuilt when a look's font comes or goes */
fun mochyTypography(font: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    displayMedium = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    displaySmall = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    headlineLarge = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    headlineMedium = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    headlineSmall = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    titleLarge = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    titleMedium = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    titleSmall = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    bodyLarge = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    bodyMedium = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    bodySmall = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    labelLarge = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    labelMedium = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = GlobalLetterSpacing
    ),
    labelSmall = TextStyle(
        fontFamily = font,
        fontWeight = GlobalFontWeight,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = GlobalLetterSpacing
    )
)
