package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.CrossmixTestToolsKey
import com.mochyy.mochymusic.constants.ForceMonthlyRecapKey
import com.mochyy.mochymusic.constants.ForcePlaybackKey
import com.mochyy.mochymusic.constants.ForceYtBackdropBlur1500Key
import com.mochyy.mochymusic.constants.HapticFocus
import com.mochyy.mochymusic.constants.HapticFocusKey
import com.mochyy.mochymusic.constants.HapticSensitivity
import com.mochyy.mochymusic.constants.LandscapeUnlockedKey
import com.mochyy.mochymusic.constants.MusicHapticsEnabledKey
import com.mochyy.mochymusic.constants.MusicHapticsSensitivityKey
import com.mochyy.mochymusic.constants.SetupCompletedKey
import com.mochyy.mochymusic.constants.ShowBigDiscStylesKey
import com.mochyy.mochymusic.constants.ShowStreamSourceToastKey
import com.mochyy.mochymusic.constants.StreamBackend
import com.mochyy.mochymusic.constants.StreamBackendKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.album
import com.mochyy.mochymusic.shared.resources.equalizer
import com.mochyy.mochymusic.shared.resources.experimental_big_disc_desc
import com.mochyy.mochymusic.shared.resources.experimental_big_disc_title
import com.mochyy.mochymusic.shared.resources.experimental_cipher_desc
import com.mochyy.mochymusic.shared.resources.experimental_cipher_title
import com.mochyy.mochymusic.shared.resources.experimental_crossmix_tools_desc
import com.mochyy.mochymusic.shared.resources.experimental_crossmix_tools_detail
import com.mochyy.mochymusic.shared.resources.experimental_crossmix_tools_title
import com.mochyy.mochymusic.shared.resources.experimental_force_playback_desc
import com.mochyy.mochymusic.shared.resources.experimental_force_playback_detail
import com.mochyy.mochymusic.shared.resources.experimental_force_playback_title
import com.mochyy.mochymusic.shared.resources.experimental_force_recap_desc
import com.mochyy.mochymusic.shared.resources.experimental_force_recap_detail
import com.mochyy.mochymusic.shared.resources.experimental_force_recap_title
import com.mochyy.mochymusic.shared.resources.experimental_force_yt_blur_desc
import com.mochyy.mochymusic.shared.resources.experimental_force_yt_blur_title
import com.mochyy.mochymusic.shared.resources.experimental_haptic_focus_balance
import com.mochyy.mochymusic.shared.resources.experimental_haptic_focus_bass
import com.mochyy.mochymusic.shared.resources.experimental_haptic_focus_title
import com.mochyy.mochymusic.shared.resources.experimental_haptic_focus_vibe
import com.mochyy.mochymusic.shared.resources.experimental_haptic_focus_vocal
import com.mochyy.mochymusic.shared.resources.experimental_haptic_sensitivity_high
import com.mochyy.mochymusic.shared.resources.experimental_haptic_sensitivity_low
import com.mochyy.mochymusic.shared.resources.experimental_haptic_sensitivity_medium
import com.mochyy.mochymusic.shared.resources.experimental_haptic_sensitivity_title
import com.mochyy.mochymusic.shared.resources.experimental_import_sync_desc
import com.mochyy.mochymusic.shared.resources.experimental_import_sync_title
import com.mochyy.mochymusic.shared.resources.experimental_music_haptics_desc
import com.mochyy.mochymusic.shared.resources.experimental_music_haptics_title
import com.mochyy.mochymusic.shared.resources.experimental_playback_diagnostics_desc
import com.mochyy.mochymusic.shared.resources.experimental_playback_diagnostics_title
import com.mochyy.mochymusic.shared.resources.experimental_repeat_setup_desc
import com.mochyy.mochymusic.shared.resources.experimental_repeat_setup_title
import com.mochyy.mochymusic.shared.resources.experimental_settings_title
import com.mochyy.mochymusic.shared.resources.experimental_show_stream_source_desc
import com.mochyy.mochymusic.shared.resources.experimental_show_stream_source_detail
import com.mochyy.mochymusic.shared.resources.experimental_show_stream_source_title
import com.mochyy.mochymusic.shared.resources.experimental_stream_backend
import com.mochyy.mochymusic.shared.resources.experimental_stream_backend_classic
import com.mochyy.mochymusic.shared.resources.experimental_stream_backend_detail
import com.mochyy.mochymusic.shared.resources.experimental_stream_backend_innertubex
import com.mochyy.mochymusic.shared.resources.experimental_unlock_landscape
import com.mochyy.mochymusic.shared.resources.experimental_unlock_landscape_desc
import com.mochyy.mochymusic.shared.resources.experimental_unlock_landscape_detail
import com.mochyy.mochymusic.shared.resources.graphic_eq
import com.mochyy.mochymusic.shared.resources.history
import com.mochyy.mochymusic.shared.resources.info
import com.mochyy.mochymusic.shared.resources.key
import com.mochyy.mochymusic.shared.resources.music_note
import com.mochyy.mochymusic.shared.resources.play
import com.mochyy.mochymusic.shared.resources.restore
import com.mochyy.mochymusic.shared.resources.screen_rotation
import com.mochyy.mochymusic.shared.resources.sliders
import com.mochyy.mochymusic.shared.resources.sync
import com.mochyy.mochymusic.shared.resources.tune
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.utils.LocalAppPreferences
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// settings > experimental: the switches that change how the app behaves before a feature settles
@Composable
fun ExperimentalSettingsScreen(
    navController: NavController,
    // the crossmix status and test rows read the app's player connection, so the Android host builds them
    crossmixRows: @Composable (Boolean) -> List<SettingsItem> = { emptyList() },
) {
    val (musicHapticsEnabled, onMusicHapticsEnabledChange) = rememberPreference(MusicHapticsEnabledKey, defaultValue = false)
    val (musicHapticsSensitivity, onMusicHapticsSensitivityChange) = rememberEnumPreference(MusicHapticsSensitivityKey, defaultValue = HapticSensitivity.MEDIUM)
    val (hapticFocus, onHapticFocusChange) = rememberEnumPreference(HapticFocusKey, defaultValue = HapticFocus.VIBE)
    val (showBigDiscStyles, onShowBigDiscStylesChange) = rememberPreference(
        ShowBigDiscStylesKey,
        defaultValue = false,
    )
    val (forceRecap, onForceRecapChange) = rememberPreference(
        ForceMonthlyRecapKey,
        defaultValue = false,
    )
    val (forceYtBackdropBlur1500, onForceYtBackdropBlur1500Change) = rememberPreference(
        ForceYtBackdropBlur1500Key,
        defaultValue = false,
    )

    val (crossmixTestTools, onCrossmixTestToolsChange) = rememberPreference(
        CrossmixTestToolsKey,
        defaultValue = false,
    )
    val crossmixTools = crossmixRows(crossmixTestTools)
    val (sourceBanner, onSourceBannerChange) = rememberPreference(ShowStreamSourceToastKey, defaultValue = false)
    val (forcePlayback, onForcePlaybackChange) = rememberPreference(ForcePlaybackKey, defaultValue = false)
    val (streamBackend, onStreamBackendChange) = rememberEnumPreference(
        StreamBackendKey,
        defaultValue = StreamBackend.INNERTUBEX,
    )
    val streamBackendTitle = stringResource(Res.string.experimental_stream_backend)
    // off by default: the whole app stays upright until the wide layout is ready
    val (landscapeUnlocked, onLandscapeUnlockedChange) = rememberPreference(
        LandscapeUnlockedKey,
        defaultValue = false,
    )
    val unlockLandscapeTitle = stringResource(Res.string.experimental_unlock_landscape)

    val store = LocalAppPreferences.current
    val coroutineScope = rememberCoroutineScope()

    SubSettingsScaffold(
        title = stringResource(Res.string.experimental_settings_title),
        onBack = { navController.navigateUp() },
    ) {
            SettingsGroup(
                style = SettingsGroupStyle.Grouped,
                items = buildList {
                    add(
                        SettingsItem(
                            title = { Text(unlockLandscapeTitle) },
                            highlightKey = unlockLandscapeTitle,
                            descriptionText = stringResource(Res.string.experimental_unlock_landscape_desc),
                            detail = stringResource(Res.string.experimental_unlock_landscape_detail),
                            icon = painterResource(Res.drawable.screen_rotation),
                            iconShape = CircleShape,
                            onClick = { onLandscapeUnlockedChange(!landscapeUnlocked) },
                            trailingContent = {
                                AppSwitch(checked = landscapeUnlocked, onCheckedChange = onLandscapeUnlockedChange)
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_import_sync_title)) },
                            highlightKey = "Import & sync",
                            descriptionText = stringResource(Res.string.experimental_import_sync_desc),
                            icon = painterResource(Res.drawable.sync),
                            iconShape = CircleShape,
                            onClick = { navController.navigate("import_sync") }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_crossmix_tools_title)) },
                            highlightKey = "Crossmix test tools",
                            descriptionText = stringResource(Res.string.experimental_crossmix_tools_desc),
                            detail = stringResource(Res.string.experimental_crossmix_tools_detail),
                            icon = painterResource(Res.drawable.graphic_eq),
                            iconShape = CircleShape,
                            onClick = { onCrossmixTestToolsChange(!crossmixTestTools) },
                            trailingContent = {
                                AppSwitch(
                                    checked = crossmixTestTools,
                                    onCheckedChange = onCrossmixTestToolsChange,
                                )
                            }
                        )
                    )
                    crossmixTools.forEach { add(it.copy(isSubOption = true)) }
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_show_stream_source_title)) },
                            highlightKey = "Show where songs play from",
                            descriptionText = stringResource(Res.string.experimental_show_stream_source_desc),
                            detail = stringResource(Res.string.experimental_show_stream_source_detail),
                            icon = painterResource(Res.drawable.info),
                            iconShape = CircleShape,
                            onClick = { onSourceBannerChange(!sourceBanner) },
                            trailingContent = {
                                AppSwitch(checked = sourceBanner, onCheckedChange = onSourceBannerChange)
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(streamBackendTitle) },
                            highlightKey = streamBackendTitle,
                            description = {
                                Text(
                                    stringResource(
                                        when (streamBackend) {
                                            StreamBackend.INNERTUBEX -> Res.string.experimental_stream_backend_innertubex
                                            StreamBackend.CLASSIC -> Res.string.experimental_stream_backend_classic
                                        }
                                    )
                                )
                            },
                            detail = stringResource(Res.string.experimental_stream_backend_detail),
                            icon = painterResource(Res.drawable.key),
                            iconShape = CircleShape,
                            onClick = {
                                onStreamBackendChange(
                                    when (streamBackend) {
                                        StreamBackend.INNERTUBEX -> StreamBackend.CLASSIC
                                        StreamBackend.CLASSIC -> StreamBackend.INNERTUBEX
                                    }
                                )
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_force_playback_title)) },
                            highlightKey = "Force playback",
                            descriptionText = stringResource(Res.string.experimental_force_playback_desc),
                            detail = stringResource(Res.string.experimental_force_playback_detail),
                            icon = painterResource(Res.drawable.play),
                            iconShape = CircleShape,
                            onClick = { onForcePlaybackChange(!forcePlayback) },
                            trailingContent = {
                                AppSwitch(checked = forcePlayback, onCheckedChange = onForcePlaybackChange)
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_force_yt_blur_title)) },
                            highlightKey = "Force 1500 px YouTube blur",
                            descriptionText = stringResource(Res.string.experimental_force_yt_blur_desc),
                            icon = painterResource(Res.drawable.tune),
                            iconShape = CircleShape,
                            onClick = { onForceYtBackdropBlur1500Change(!forceYtBackdropBlur1500) },
                            trailingContent = {
                                AppSwitch(
                                    checked = forceYtBackdropBlur1500,
                                    onCheckedChange = onForceYtBackdropBlur1500Change,
                                )
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_force_recap_title)) },
                            highlightKey = "Force show monthly recap",
                            descriptionText = stringResource(Res.string.experimental_force_recap_desc),
                            detail = stringResource(Res.string.experimental_force_recap_detail),
                            icon = painterResource(Res.drawable.history),
                            iconShape = CircleShape,
                            onClick = { onForceRecapChange(!forceRecap) },
                            trailingContent = {
                                AppSwitch(
                                    checked = forceRecap,
                                    onCheckedChange = onForceRecapChange,
                                )
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_big_disc_title)) },
                            highlightKey = "Big disc cover styles",
                            descriptionText = stringResource(Res.string.experimental_big_disc_desc),
                            icon = painterResource(Res.drawable.album),
                            iconShape = CircleShape,
                            onClick = { onShowBigDiscStylesChange(!showBigDiscStyles) },
                            trailingContent = {
                                AppSwitch(
                                    checked = showBigDiscStyles,
                                    onCheckedChange = onShowBigDiscStylesChange,
                                )
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_music_haptics_title)) },
                            highlightKey = "Music haptics",
                            descriptionText = stringResource(Res.string.experimental_music_haptics_desc),
                            icon = painterResource(Res.drawable.music_note),
                            iconShape = CircleShape,
                            onClick = { onMusicHapticsEnabledChange(!musicHapticsEnabled) },
                            trailingContent = {
                                AppSwitch(checked = musicHapticsEnabled, onCheckedChange = onMusicHapticsEnabledChange)
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_haptic_sensitivity_title)) },
                            description = {
                                Text(
                                    when (musicHapticsSensitivity) {
                                        HapticSensitivity.LOW -> stringResource(Res.string.experimental_haptic_sensitivity_low)
                                        HapticSensitivity.MEDIUM -> stringResource(Res.string.experimental_haptic_sensitivity_medium)
                                        HapticSensitivity.HIGH -> stringResource(Res.string.experimental_haptic_sensitivity_high)
                                    }
                                )
                            },
                            icon = painterResource(Res.drawable.sliders),
                            iconShape = CircleShape,
                            enabled = musicHapticsEnabled,
                            onClick = {
                                onMusicHapticsSensitivityChange(
                                    when (musicHapticsSensitivity) {
                                        HapticSensitivity.LOW -> HapticSensitivity.MEDIUM
                                        HapticSensitivity.MEDIUM -> HapticSensitivity.HIGH
                                        HapticSensitivity.HIGH -> HapticSensitivity.LOW
                                    }
                                )
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_haptic_focus_title)) },
                            description = {
                                Text(
                                    when (hapticFocus) {
                                        HapticFocus.BALANCE -> stringResource(Res.string.experimental_haptic_focus_balance)
                                        HapticFocus.VOCAL -> stringResource(Res.string.experimental_haptic_focus_vocal)
                                        HapticFocus.VIBE -> stringResource(Res.string.experimental_haptic_focus_vibe)
                                        HapticFocus.BASS -> stringResource(Res.string.experimental_haptic_focus_bass)
                                    }
                                )
                            },
                            icon = painterResource(Res.drawable.equalizer),
                            iconShape = CircleShape,
                            enabled = musicHapticsEnabled,
                            onClick = {
                                onHapticFocusChange(
                                    when (hapticFocus) {
                                        HapticFocus.BALANCE -> HapticFocus.VOCAL
                                        HapticFocus.VOCAL -> HapticFocus.VIBE
                                        HapticFocus.VIBE -> HapticFocus.BASS
                                        HapticFocus.BASS -> HapticFocus.BALANCE
                                    }
                                )
                            }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_cipher_title)) },
                            highlightKey = "Cipher",
                            descriptionText = stringResource(Res.string.experimental_cipher_desc),
                            icon = painterResource(Res.drawable.key),
                            iconShape = CircleShape,
                            onClick = { navController.navigate("cipher_settings") }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_playback_diagnostics_title)) },
                            highlightKey = "Playback diagnostics",
                            descriptionText = stringResource(Res.string.experimental_playback_diagnostics_desc),
                            icon = painterResource(Res.drawable.info),
                            iconShape = CircleShape,
                            onClick = { navController.navigate("playback_diagnostics") }
                        )
                    )
                    add(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.experimental_repeat_setup_title)) },
                            highlightKey = "Repeat initial setup",
                            descriptionText = stringResource(Res.string.experimental_repeat_setup_desc),
                            icon = painterResource(Res.drawable.restore),
                            iconShape = CircleShape,
                            onClick = {
                                coroutineScope.launch {
                                    store.edit { prefs ->
                                        prefs[SetupCompletedKey] = false
                                    }
                                }
                            }
                        )
                    )
                }
            )
    }
}
