package com.mochyy.mochymusic.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.mochyy.mochymusic.BuildConfig
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.LarpLevelKey
import com.mochyy.mochymusic.constants.LarpModeKey
import com.mochyy.mochymusic.constants.LyricsOrderOverrideKey
import com.mochyy.mochymusic.constants.LyricsProviderOrderKey
import com.mochyy.mochymusic.constants.PreferredLyricsProvider
import com.mochyy.mochymusic.constants.PreferredLyricsProviderKey
import com.mochyy.mochymusic.constants.SYSTEM_DEFAULT
import com.mochyy.mochymusic.core.updater.saveUpdateNotificationsSetting
import com.mochyy.mochymusic.i18n.AppLanguage
import com.mochyy.mochymusic.i18n.activityOrNull
import com.mochyy.mochymusic.lyrics.LyricsProviderRegistry
import com.mochyy.mochymusic.lyrics.YouTubeCaptionsLyricsProvider
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.LocalZoomOutOverlayState
import com.mochyy.mochymusic.ui.component.PopupSheetState
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.player.LarpLevel
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// the app behaviour rows that lean on Android-only sheets, per-app locale and the lyrics registry;
// the page itself is shared (shared/.../ui/screens/settings/AppBehaviourSettingsScreen.kt)
@Composable
fun AndroidAppBehaviourSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val overlay = LocalZoomOutOverlayState.current
    val japanesePack: (@Composable () -> SettingsItem)? =
        if (BuildConfig.COMPACT_APK) ({ japaneseLanguagePackSetting() }) else null
    AppBehaviourSettingsScreen(
        navController = navController,
        rows = AppBehaviourRows(
            larp = { larpItem(overlay) },
            language = { languageItem(overlay) },
            lyricsOrder = { lyricsOrderItem(overlay) },
            japaneseLanguagePack = japanesePack,
        ),
        onUpdateNoticeChanged = { hidden -> saveUpdateNotificationsSetting(context, !hidden) },
    )
}

// there's no limit to the larp
@Composable
private fun larpItem(overlay: PopupSheetState): SettingsItem {
    val (larping, setLarping) = rememberPreference(LarpModeKey, defaultValue = false)
    val (level, setLevel) = rememberEnumPreference(LarpLevelKey, defaultValue = LarpLevel.MAXXING)
    // the design's plain circle, nothing drawn in it
    val blank = remember { ColorPainter(Color.Transparent) }
    val openSheet = {
        overlay.showLarpSheet(current = level) { picked ->
            setLevel(picked)
            setLarping(true)
        }
    }
    val title = stringResource(R.string.larp_mode)
    return SettingsItem(
        // the row opens the sheet to change level too, the switch only turns it off without asking
        title = { Text(title) },
        highlightKey = "Larp mode",
        searchText = title,
        descriptionText = if (larping) stringResource(level.title) else stringResource(R.string.larp_mode_desc),
        detail = stringResource(R.string.larp_mode_detail),
        icon = blank,
        iconShape = CircleShape,
        onClick = openSheet,
        trailingContent = {
            AppSwitch(checked = larping, onCheckedChange = { on -> if (on) openSheet() else setLarping(false) })
        },
    )
}

// the app's own language, apart from the phone's; android 13+ keeps it in the system's per-app setting too
@Composable
private fun languageItem(overlay: PopupSheetState): SettingsItem {
    val context = LocalContext.current
    val activity = remember(context) { context.activityOrNull() }
    val language = remember(context) { AppLanguage.selection(context) }
    val languageName = if (language == SYSTEM_DEFAULT) stringResource(R.string.system_default) else appLanguageNativeName(language)
    val failure = stringResource(R.string.app_language_update_failed)
    val scope = rememberCoroutineScope()
    val pick = { tag: String ->
        if (tag != language && activity != null) {
            scope.launch {
                try {
                    AppLanguage.select(activity, tag)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    Toast.makeText(context, failure, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    return SettingsItem(
        title = { Text(stringResource(R.string.app_language)) },
        highlightKey = "App language",
        descriptionText = languageName,
        searchText = stringResource(R.string.app_language),
        icon = painterResource(R.drawable.translate),
        iconShape = CircleShape,
        enabled = activity != null,
        onClick = { overlay.showAppLanguageSheet(selected = language, onSelect = { pick(it) }) },
    )
}

// lyrics normally go to whoever has the most features, this lets the user's own provider order decide instead
@Composable
private fun lyricsOrderItem(overlay: PopupSheetState): SettingsItem {
    val (overridden, setOverridden) = rememberPreference(LyricsOrderOverrideKey, defaultValue = false)
    val (savedOrder, setSavedOrder) = rememberPreference(LyricsProviderOrderKey, defaultValue = "")
    val (preferred) = rememberEnumPreference(PreferredLyricsProviderKey, defaultValue = PreferredLyricsProvider.YOULYPLUS)
    // auto captions are only asked when nobody else has lyrics, so dragging them anywhere would change nothing
    val order = remember(savedOrder, preferred) {
        LyricsProviderRegistry.effectiveProviderOrder(savedOrder, preferred).filter { it != YouTubeCaptionsLyricsProvider.name }
    }
    val openSheet = {
        overlay.showLyricsProviderSortSheet(order) { sorted ->
            setSavedOrder(LyricsProviderRegistry.serializeProviderOrder(sorted))
            setOverridden(true)
        }
    }
    val title = stringResource(R.string.settings_lyrics_order_override)
    // The row opens the sort sheet. Switching off keeps the saved order for next time.
    return SettingsItem(
        title = { Text(title) },
        highlightKey = "Override lyrics sorting",
        searchText = title,
        descriptionText = if (overridden) {
            stringResource(R.string.settings_lyrics_order_override_on, LyricsProviderRegistry.getDisplayName(order.first()))
        } else {
            stringResource(R.string.settings_lyrics_order_override_desc)
        },
        detail = stringResource(R.string.settings_lyrics_order_override_detail),
        icon = rememberVectorPainter(Icons.AutoMirrored.Rounded.Sort),
        iconShape = CircleShape,
        onClick = openSheet,
        trailingContent = {
            AppSwitch(checked = overridden, onCheckedChange = { on -> if (on) openSheet() else setOverridden(false) })
        },
    )
}
