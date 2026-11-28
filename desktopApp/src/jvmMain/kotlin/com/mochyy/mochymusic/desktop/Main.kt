// Main.kt
// Mochy for desktop, based on Mochy Music by realidkroo (github.com/realidkroo/mochy-music).

package com.mochyy.mochymusic.desktop

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import com.mochyy.mochymusic.shared.desktop.runDesktopApp

// The Android app's Inter files, put on the classpath by this module's build script.
private val Inter = FontFamily(
    Font("font/inter_thin.ttf", FontWeight.Thin),
    Font("font/inter_extralight.ttf", FontWeight.ExtraLight),
    Font("font/inter_light.ttf", FontWeight.Light),
    Font("font/inter_regular.ttf", FontWeight.Normal),
    Font("font/inter_medium.ttf", FontWeight.Medium),
    Font("font/inter_semibold.ttf", FontWeight.SemiBold),
    Font("font/inter_bold.ttf", FontWeight.Bold),
)

fun main() = runDesktopApp(Inter)
