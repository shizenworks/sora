package com.mochyy.mochymusic.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.ui.component.BlurDirection
import com.mochyy.mochymusic.ui.component.BlurEffectCache
import com.mochyy.mochymusic.ui.component.FlatBarScrimAlpha
import com.mochyy.mochymusic.ui.component.GlassState
import com.mochyy.mochymusic.ui.component.LocalBottomSheetPageState
import com.mochyy.mochymusic.ui.component.PopupSheetHandle
import com.mochyy.mochymusic.ui.component.ProgressiveGlassBackground
import com.mochyy.mochymusic.ui.component.glassRoot
import com.mochyy.mochymusic.ui.component.rememberProgressiveBlur

/** The Stats route: the shared page with Android's glass bar, bottom-sheet picker and genre lookup. */
@Composable
fun AndroidStatsScreen(navController: NavController) {
    val context = LocalContext.current
    StatsScreen(
        navController = navController,
        chrome = rememberAndroidStatsChrome(),
        pickerHost = rememberAndroidStatsPickerHost(),
        genreShares = { artists -> RecapGenres.shares(context, artists) },
    )
}

@Composable
private fun rememberAndroidStatsChrome(): StatsChrome {
    val glass = remember { GlassState() }
    return remember(glass) {
        object : StatsChrome {
            @Composable
            override fun state(): StatsChromeState {
                val progressiveBar = rememberProgressiveBlur()
                return remember(glass, progressiveBar) {
                    object : StatsChromeState {
                        @Composable
                        override fun contentModifier(isActive: () -> Boolean): Modifier = Modifier.glassRoot(glass, isActive = isActive)

                        @Composable
                        override fun TopBackground(collapse: () -> Float, topInset: Dp) {
                            val showGlass by remember { derivedStateOf { collapse() > 0.01f } }
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(topInset + 84.dp)
                                        .graphicsLayer { alpha = collapse() },
                            ) {
                                if (showGlass) {
                                    ProgressiveGlassBackground(
                                        state = glass,
                                        maxBlurRadius = { 50f * collapse() },
                                        foundationColor = Color.Black.copy(alpha = 0.5f),
                                        direction = BlurDirection.BottomToTop,
                                        steps = 3,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                Box(
                                    Modifier.fillMaxSize().background(
                                        if (progressiveBar) {
                                            Brush.verticalGradient(listOf(Color.Black, Color.Black.copy(alpha = 0.7f), Color.Transparent))
                                        } else {
                                            SolidColor(Color.Black.copy(alpha = FlatBarScrimAlpha))
                                        },
                                    ),
                                )
                            }
                        }

                        override fun elementBlur(radius: Float): RenderEffect? = BlurEffectCache.element(radius)
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberAndroidStatsPickerHost(): StatsPickerHost {
    val sheets = LocalBottomSheetPageState.current
    return remember(sheets) {
        object : StatsPickerHost {
            override fun show(
                buttonBar: @Composable (dismiss: () -> Unit) -> Unit,
                content: @Composable ColumnScope.() -> Unit,
            ) {
                var handle: PopupSheetHandle? = null
                handle = sheets.showWithHandle(buttonBar = { buttonBar { handle?.dismiss() } }) { content() }
            }
        }
    }
}
