// Android's side of the shared platform skin: liquid glass controls, folding settings groups and the
// live page previews

package com.mochyy.mochymusic.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.ui.component.PlatformSkin
import com.mochyy.mochymusic.ui.component.SettingsPreviewPage
import com.mochyy.mochymusic.ui.component.SkinSlider
import com.mochyy.mochymusic.ui.component.SkinSwitch
import com.mochyy.mochymusic.ui.component.liquid.LiquidGlassConfig
import com.mochyy.mochymusic.ui.component.liquid.LiquidGlassSlider
import com.mochyy.mochymusic.ui.component.liquid.LiquidGlassSwitch
import com.mochyy.mochymusic.ui.motion.columnFold
import com.mochyy.mochymusic.ui.screens.gesture.PortedPreviewHeader
import com.mochyy.mochymusic.ui.screens.gesture.PreviewCard
import com.mochyy.mochymusic.ui.screens.gesture.highlightTarget
import com.mochyy.mochymusic.ui.screens.settings.previews.InternetPreview
import com.mochyy.mochymusic.ui.screens.settings.previews.ListenBrainzPreview
import com.mochyy.mochymusic.ui.screens.settings.previews.ProxyPreview
import com.mochyy.mochymusic.ui.screens.settings.previews.YouTubeSyncPreview

@Composable
fun rememberAndroidPlatformSkin(glass: LiquidGlassConfig): PlatformSkin = remember(glass) {
    val switch: SkinSwitch = { checked, onCheckedChange, modifier, enabled ->
        LiquidGlassSwitch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
    }
    val slider: SkinSlider = { value, onValueChange, modifier, valueRange, enabled, onFinished ->
        LiquidGlassSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = valueRange,
            enabled = enabled,
            onValueChangeFinished = onFinished,
        )
    }
    PlatformSkin(
        switch = switch.takeIf { glass.surface(glass.toggles) },
        slider = slider.takeIf { glass.surface(glass.sliders) },
        sectionMotion = { scroll, clearTopPx -> columnFold(scroll, clearTopPx) },
        highlight = { keys -> highlightTarget(keys) },
        previewCard = { category, title, onClick, modifier, preview ->
            PreviewCard(category = category, title = title, onClick = onClick, modifier = modifier, preview = preview)
        },
        pagePreview = { page ->
            PortedPreviewHeader {
                when (page) {
                    SettingsPreviewPage.Internet -> InternetPreview()
                    SettingsPreviewPage.Proxy -> ProxyPreview()
                    SettingsPreviewPage.YouTubeSync -> YouTubeSyncPreview()
                    SettingsPreviewPage.ListenBrainz -> ListenBrainzPreview()
                }
            }
            Spacer(Modifier.height(16.dp))
        },
    )
}
