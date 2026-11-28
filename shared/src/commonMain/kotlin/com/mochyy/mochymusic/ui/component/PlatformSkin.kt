// the looks only one platform can draw. Android fills these in (liquid glass, folding cards, live page
// previews); desktop and iOS leave them empty and get the plain controls

package com.mochyy.mochymusic.ui.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/** a platform's own switch, drawn in place of the plain one */
typealias SkinSwitch = @Composable (checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier, enabled: Boolean) -> Unit

/** a platform's own slider, drawn in place of the plain one */
typealias SkinSlider = @Composable (
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChangeFinished: (() -> Unit)?,
) -> Unit

/** a settings page link drawn as a card with a little screen of that page in it */
typealias SkinPreviewCard = @Composable (
    category: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier,
    preview: @Composable BoxScope.() -> Unit,
) -> Unit

/** the pages that can show a live preview above their rows */
enum class SettingsPreviewPage { Internet, Proxy, YouTubeSync, ListenBrainz }

class PlatformSkin(
    /** null while the platform's switch look is off */
    val switch: SkinSwitch? = null,
    /** null while the platform's slider look is off */
    val slider: SkinSlider? = null,
    /** how a settings group moves as it scrolls under the top bar */
    val sectionMotion: (Modifier.(scroll: ScrollState, clearTopPx: Float) -> Modifier)? = null,
    /** marks a row the settings search can jump to */
    val highlight: (@Composable Modifier.(keys: List<String>) -> Modifier)? = null,
    val previewCard: SkinPreviewCard? = null,
    val pagePreview: (@Composable (SettingsPreviewPage) -> Unit)? = null,
)

val LocalPlatformSkin = staticCompositionLocalOf { PlatformSkin() }

/** the live preview at the top of [page], on platforms that draw one */
@Composable
fun PagePreview(page: SettingsPreviewPage) {
    LocalPlatformSkin.current.pagePreview?.invoke(page)
}
