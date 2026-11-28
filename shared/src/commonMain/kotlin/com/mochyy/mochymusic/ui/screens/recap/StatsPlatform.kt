package com.mochyy.mochymusic.ui.screens.recap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * The Stats page's glass. Android's GlassKit is still app-only, so the app supplies a chrome that
 * blurs the page under the top bar and the bar itself; every other platform falls back to a scrim.
 */
interface StatsChrome {
    @Composable
    fun state(): StatsChromeState
}

interface StatsChromeState {
    /** The page's root modifier; Android returns `glassRoot`, the others nothing. */
    @Composable
    fun contentModifier(isActive: () -> Boolean): Modifier

    /** The bar behind the title row. [collapse] is 0 at the top and 1 once the title scrolled away. */
    @Composable
    fun TopBackground(
        collapse: () -> Float,
        topInset: Dp,
    )

    /** The platform's element blur (Android: BlurEffectCache.element); null where there is none. */
    fun elementBlur(radius: Float): RenderEffect? = null
}

/** No glass: the bar is a black-to-transparent scrim, like the app's fallback when glass is off. */
object ScrimStatsChrome : StatsChrome {
    @Composable
    override fun state(): StatsChromeState =
        remember {
            object : StatsChromeState {
                @Composable
                override fun contentModifier(isActive: () -> Boolean): Modifier = Modifier

                @Composable
                override fun TopBackground(
                    collapse: () -> Float,
                    topInset: Dp,
                ) {
                    val show by remember { derivedStateOf { collapse() > 0.01f } }
                    if (show) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(topInset + 84.dp)
                                .graphicsLayer { alpha = collapse() }
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black, Color.Black.copy(alpha = 0.7f), Color.Transparent),
                                    ),
                                ),
                        )
                    }
                }
            }
        }
}

/**
 * Presents the month/year picker: Android opens it in the app's bottom sheet; the default is a plain
 * dialog. [buttonBar] is the "done" button - its [dismiss] closes the presentation.
 */
interface StatsPickerHost {
    fun show(
        buttonBar: @Composable (dismiss: () -> Unit) -> Unit,
        content: @Composable ColumnScope.() -> Unit,
    )
}

private class PickerSheet(
    val buttonBar: @Composable (dismiss: () -> Unit) -> Unit,
    val content: @Composable ColumnScope.() -> Unit,
)

/** The picker for platforms without the app's bottom sheet: a full-width dialog from the bottom. */
@Composable
fun rememberStatsPickerDialogHost(): StatsPickerHost {
    val sheet = remember { mutableStateOf<PickerSheet?>(null) }
    val current = sheet.value
    if (current != null) {
        Dialog(
            onDismissRequest = { sheet.value = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(Color(0xFF141414))
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                ) {
                    current.buttonBar { sheet.value = null }
                    current.content(this)
                }
            }
        }
    }
    return remember {
        object : StatsPickerHost {
            override fun show(
                buttonBar: @Composable (dismiss: () -> Unit) -> Unit,
                content: @Composable ColumnScope.() -> Unit,
            ) {
                sheet.value = PickerSheet(buttonBar, content)
            }
        }
    }
}
