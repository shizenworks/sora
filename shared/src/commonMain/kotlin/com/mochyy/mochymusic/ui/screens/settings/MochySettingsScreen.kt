// MochySettingsScreen.kt

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.mochyy.mochymusic.ui.screens.settings.integrations.IntegrationsRoute
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mochyy.mochymusic.LocalPlayerAwareWindowInsets
import com.mochyy.mochymusic.constants.ProfilePicUriKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.app_language
import com.mochyy.mochymusic.shared.resources.appearance_animation
import com.mochyy.mochymusic.shared.resources.appearance_bloom
import com.mochyy.mochymusic.shared.resources.appearance_blur
import com.mochyy.mochymusic.shared.resources.appearance_blur_quality
import com.mochyy.mochymusic.shared.resources.appearance_blur_strength
import com.mochyy.mochymusic.shared.resources.appearance_canvas
import com.mochyy.mochymusic.shared.resources.appearance_canvas_wifi
import com.mochyy.mochymusic.shared.resources.appearance_format_badge
import com.mochyy.mochymusic.shared.resources.appearance_lyrics_animation
import com.mochyy.mochymusic.shared.resources.appearance_lyrics_soft_light
import com.mochyy.mochymusic.shared.resources.appearance_progressive_blur
import com.mochyy.mochymusic.shared.resources.appearance_sample_rate
import com.mochyy.mochymusic.shared.resources.appearance_youtube_background
import com.mochyy.mochymusic.shared.resources.audiopass_hub_title
import com.mochyy.mochymusic.shared.resources.canvas_best_quality
import com.mochyy.mochymusic.shared.resources.canvas_order
import com.mochyy.mochymusic.shared.resources.car_settings_title
import com.mochyy.mochymusic.shared.resources.cast_connected
import com.mochyy.mochymusic.shared.resources.contrast
import com.mochyy.mochymusic.shared.resources.customise_animation_speed
import com.mochyy.mochymusic.shared.resources.customise_cover_rounding
import com.mochyy.mochymusic.shared.resources.customise_element_blur
import com.mochyy.mochymusic.shared.resources.customise_gesture
import com.mochyy.mochymusic.shared.resources.customise_home_styles
import com.mochyy.mochymusic.shared.resources.customise_playlist_styles
import com.mochyy.mochymusic.shared.resources.customise_player_styles
import com.mochyy.mochymusic.shared.resources.customise_swipe_down_anywhere
import com.mochyy.mochymusic.shared.resources.customise_swipe_left
import com.mochyy.mochymusic.shared.resources.customise_swipe_right
import com.mochyy.mochymusic.shared.resources.customise_swipe_up_follows
import com.mochyy.mochymusic.shared.resources.customise_swipe_up_queue
import com.mochyy.mochymusic.shared.resources.customise_top_bar
import com.mochyy.mochymusic.shared.resources.devices_title
import com.mochyy.mochymusic.shared.resources.disable_screenshot
import com.mochyy.mochymusic.shared.resources.equalizer
import com.mochyy.mochymusic.shared.resources.hide_explicit
import com.mochyy.mochymusic.shared.resources.integration
import com.mochyy.mochymusic.shared.resources.integrations
import com.mochyy.mochymusic.shared.resources.internet
import com.mochyy.mochymusic.shared.resources.language
import com.mochyy.mochymusic.shared.resources.larp_mode
import com.mochyy.mochymusic.shared.resources.library_add
import com.mochyy.mochymusic.shared.resources.lyrics
import com.mochyy.mochymusic.shared.resources.palette
import com.mochyy.mochymusic.shared.resources.pause_listen_history
import com.mochyy.mochymusic.shared.resources.play
import com.mochyy.mochymusic.shared.resources.playback_audio_offload
import com.mochyy.mochymusic.shared.resources.playback_efficient_decoding
import com.mochyy.mochymusic.shared.resources.playback_hifi
import com.mochyy.mochymusic.shared.resources.playback_hires
import com.mochyy.mochymusic.shared.resources.playback_keep_playing
import com.mochyy.mochymusic.shared.resources.playback_mix_other_apps
import com.mochyy.mochymusic.shared.resources.playback_preload_lyrics
import com.mochyy.mochymusic.shared.resources.playback_preload_next
import com.mochyy.mochymusic.shared.resources.playback_save_state
import com.mochyy.mochymusic.shared.resources.playback_skip_silence
import com.mochyy.mochymusic.shared.resources.playback_usb_passthrough
import com.mochyy.mochymusic.shared.resources.playback_usb_prefer
import com.mochyy.mochymusic.shared.resources.proxy
import com.mochyy.mochymusic.shared.resources.settings
import com.mochyy.mochymusic.shared.resources.settings_accent
import com.mochyy.mochymusic.shared.resources.settings_albums_from_likes
import com.mochyy.mochymusic.shared.resources.settings_app_behaviour
import com.mochyy.mochymusic.shared.resources.settings_appearance
import com.mochyy.mochymusic.shared.resources.settings_audio_lab_category
import com.mochyy.mochymusic.shared.resources.settings_custom_themes
import com.mochyy.mochymusic.shared.resources.settings_customisation_category
import com.mochyy.mochymusic.shared.resources.settings_data_saver
import com.mochyy.mochymusic.shared.resources.settings_disable_video_autoplay
import com.mochyy.mochymusic.shared.resources.settings_dont_skip_failure
import com.mochyy.mochymusic.shared.resources.settings_hide_descriptions
import com.mochyy.mochymusic.shared.resources.settings_hide_nav_on_scroll
import com.mochyy.mochymusic.shared.resources.settings_hide_player_thumbnail
import com.mochyy.mochymusic.shared.resources.settings_modules_category
import com.mochyy.mochymusic.shared.resources.settings_nav_bar
import com.mochyy.mochymusic.shared.resources.settings_nothing_matches
import com.mochyy.mochymusic.shared.resources.settings_now_playing
import com.mochyy.mochymusic.shared.resources.settings_offline_mode
import com.mochyy.mochymusic.shared.resources.settings_playback_category
import com.mochyy.mochymusic.shared.resources.settings_persistent_shuffle
import com.mochyy.mochymusic.shared.resources.settings_skip_timeout
import com.mochyy.mochymusic.shared.resources.settings_start_tab
import com.mochyy.mochymusic.shared.resources.settings_sync_lyrics_video
import com.mochyy.mochymusic.shared.resources.settings_system_font
import com.mochyy.mochymusic.shared.resources.settings_tablet_layout
import com.mochyy.mochymusic.shared.resources.settings_ui_scale
import com.mochyy.mochymusic.shared.resources.settings_updates_notice
import com.mochyy.mochymusic.shared.resources.settings_youtube_translations
import com.mochyy.mochymusic.shared.resources.storage
import com.mochyy.mochymusic.shared.resources.ic_mochy_mark
import com.mochyy.mochymusic.shared.resources.lyrics_auto_scroll
import com.mochyy.mochymusic.shared.resources.lyrics_click_change
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_belarusian
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_bulgarian
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_chinese
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_hindi
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_japanese
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_korean
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_kyrgyz
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_macedonian
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_punjabi
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_russian
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_serbian
import com.mochyy.mochymusic.shared.resources.lyrics_romanize_ukrainian
import com.mochyy.mochymusic.shared.resources.pause_music_when_media_is_muted
import com.mochyy.mochymusic.shared.resources.pause_search_history
import com.mochyy.mochymusic.shared.resources.playback_infinite_queue
import com.mochyy.mochymusic.shared.resources.playback_pip
import com.mochyy.mochymusic.shared.resources.playback_resume_bluetooth
import com.mochyy.mochymusic.shared.resources.settings_ai_translator
import com.mochyy.mochymusic.shared.resources.settings_floating_lyrics
import com.mochyy.mochymusic.shared.resources.settings_hide_music_videos
import com.mochyy.mochymusic.shared.resources.settings_index_youtube_source
import com.mochyy.mochymusic.shared.resources.settings_index_youtube_sync
import com.mochyy.mochymusic.shared.resources.settings_keep_cpu_awake
import com.mochyy.mochymusic.shared.resources.settings_local_auto_metadata
import com.mochyy.mochymusic.shared.resources.settings_lyrics_order_override
import com.mochyy.mochymusic.shared.resources.settings_mute_recovery_volume
import com.mochyy.mochymusic.shared.resources.settings_queue_add_in_order
import com.mochyy.mochymusic.shared.resources.settings_queue_lock
import com.mochyy.mochymusic.shared.resources.settings_queue_prevent_duplicates
import com.mochyy.mochymusic.shared.resources.settings_romanize_as_main
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_intro
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_offtopic
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_outro
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_selfpromo
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_sponsor
import com.mochyy.mochymusic.shared.resources.settings_studio_for_videos
import com.mochyy.mochymusic.shared.resources.settings_translate_auto
import com.mochyy.mochymusic.shared.resources.settings_youtube_plays
import com.mochyy.mochymusic.shared.resources.video_quality
import com.mochyy.mochymusic.shared.ui.theme.LocalMochyAccent
import com.mochyy.mochymusic.ui.component.SettingsHighlight
import com.mochyy.mochymusic.ui.screens.search.rememberScrollCollapseProgress
import com.mochyy.mochymusic.ui.theme.InterFontFamily
import com.mochyy.mochymusic.ui.utils.BackPressHandler
import com.mochyy.mochymusic.utils.rememberPreference
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private data class SettingsEntry(
    val title: String,
    val page: String,
    val route: String,
    val keywords: String = "",
    val titleRes: StringResource? = null,
    val pageRes: StringResource? = null,
) {
    val highlightKey: String get() = title
}

// search ids stay english so rows can find themselves, what's shown and matched follows the app's language
private val LocalizedSettingsTitles = mapOf(
    "Customisation" to Res.string.settings_customisation_category,
    "Blur" to Res.string.appearance_blur,
    "Progressive blur" to Res.string.appearance_progressive_blur,
    "Blur quality" to Res.string.appearance_blur_quality,
    "Blur strength" to Res.string.appearance_blur_strength,
    "Animation" to Res.string.appearance_animation,
    "Lyrics animation" to Res.string.appearance_lyrics_animation,
    "Larp mode" to Res.string.larp_mode,
    "Disable App Updates notice" to Res.string.settings_updates_notice,
    "Offline mode" to Res.string.settings_offline_mode,
    "Data saver" to Res.string.settings_data_saver,
    "Proxy" to Res.string.proxy,
    "Albums from liked songs" to Res.string.settings_albums_from_likes,
    "Keep playing even when closed" to Res.string.playback_keep_playing,
    "Play alongside other apps" to Res.string.playback_mix_other_apps,
    "Save Player's last state" to Res.string.playback_save_state,
    "Local song auto metadata" to Res.string.settings_local_auto_metadata,
    "Keep shuffle on" to Res.string.settings_persistent_shuffle,
    "Preload next song" to Res.string.playback_preload_next,
    "Preload queue lyrics" to Res.string.playback_preload_lyrics,
    "Mute recovery volume" to Res.string.settings_mute_recovery_volume,
    "Hide player thumbnail" to Res.string.settings_hide_player_thumbnail,
    "Swipe up opens the queue" to Res.string.customise_swipe_up_queue,
    "Audio offload" to Res.string.playback_audio_offload,
    "Efficient decoding" to Res.string.playback_efficient_decoding,
    "Android Auto" to Res.string.car_settings_title,
    "Disable auto-playing YouTube video" to Res.string.settings_disable_video_autoplay,
    "Translations from YouTube" to Res.string.settings_youtube_translations,
    "Sync lyrics to music videos" to Res.string.settings_sync_lyrics_video,
    "Don't skip on failure" to Res.string.settings_dont_skip_failure,
    "Skip timeout failure" to Res.string.settings_skip_timeout,
    "Skip Silence" to Res.string.playback_skip_silence,
    "Audio format badge" to Res.string.appearance_format_badge,
    "Show sample rate" to Res.string.appearance_sample_rate,
    "Always enable Youtube background" to Res.string.appearance_youtube_background,
    "Enable animated canvas" to Res.string.appearance_canvas,
    "High quality bloom" to Res.string.appearance_bloom,
    "Tinted lyrics" to Res.string.appearance_lyrics_soft_light,
    "Audio Pass" to Res.string.audiopass_hub_title,
    "Prefer USB DAC" to Res.string.playback_usb_prefer,
    "USB DAC passthrough" to Res.string.playback_usb_passthrough,
    "Hi-res output" to Res.string.playback_hires,
    "Hi-fi resampler" to Res.string.playback_hifi,
    "Audio Lab" to Res.string.settings_audio_lab_category,
    "Modules" to Res.string.settings_modules_category,
    "Storage" to Res.string.storage,
    "App behaviour" to Res.string.settings_app_behaviour,
    "Playback" to Res.string.settings_playback_category,
    "Now playing" to Res.string.settings_now_playing,
    "Disable description on settings" to Res.string.settings_hide_descriptions,
    "Hide explicit content" to Res.string.hide_explicit,
    "Hide music videos" to Res.string.settings_hide_music_videos,
    "Pause listen history" to Res.string.pause_listen_history,
    "Disable screenshot" to Res.string.disable_screenshot,
    "Cover rounding" to Res.string.customise_cover_rounding,
    "Playlist styles" to Res.string.customise_playlist_styles,
    "Player styles" to Res.string.customise_player_styles,
    "Home styles" to Res.string.customise_home_styles,
    "Element blur" to Res.string.customise_element_blur,
    "Animation speed" to Res.string.customise_animation_speed,
    "Top Bar on player" to Res.string.customise_top_bar,
    "Gesture" to Res.string.customise_gesture,
    "Swipe right" to Res.string.customise_swipe_right,
    "Swipe left" to Res.string.customise_swipe_left,
    "Swipe down anywhere to close" to Res.string.customise_swipe_down_anywhere,
    "Open with your finger" to Res.string.customise_swipe_up_follows,
)
private val LocalizedSettingsPages = mapOf(
    "App behaviour" to Res.string.settings_app_behaviour,
    "Playback" to Res.string.settings_playback_category,
    "Now playing" to Res.string.settings_now_playing,
    "Customisation" to Res.string.settings_customisation_category,
    "Appearance" to Res.string.settings_appearance,
    "Storage" to Res.string.storage,
    "Audio Lab" to Res.string.settings_audio_lab_category,
    "Modules" to Res.string.settings_modules_category,
    "Integrations" to Res.string.integrations,
)

// routes that only the app registers keep their exact strings here; shared constants are imported
private const val DeviceIntegrationRoute = "device_integration"
private const val AndroidAutoSettingsRoute = "integrations_settings/android_auto"
private const val DiscordRoute = "integrations_settings/discord"
private const val LastFmRoute = "integrations_settings/scrobble/LASTFM"
private const val LibreFmRoute = "integrations_settings/scrobble/LIBREFM"
private const val YouTubeSyncRoute = "youtube_sync"
private const val AccountSwitcherRoute = "youtube_accounts"

private val SettingsIndex = listOf(
    // Appearance
    SettingsEntry("Appearance", "Appearance", "appearance_settings", "look theme tabs navigation bar layout scale colour color font tablet", Res.string.settings_appearance),
    SettingsEntry("Default start tab", "Appearance", "appearance_settings", "start open launch first tab home search library", Res.string.settings_start_tab),
    SettingsEntry("Hide navigation bar on scroll", "Appearance", "appearance_settings", "nav navigation bar tabs hide slide scroll list auto", Res.string.settings_hide_nav_on_scroll),
    SettingsEntry("Navigation bar", "Appearance", "nav_bar_settings", "nav bar tabs order reorder show hide home search library settings choose", Res.string.settings_nav_bar),
    SettingsEntry("Accent colour", "Appearance", "appearance_settings", "accent colour color palette theme primary highlight album art cover", Res.string.settings_accent),
    SettingsEntry("Custom themes", "Appearance", "appearance_settings", "theme themes custom palette colour color seed create edit delete save saved", Res.string.settings_custom_themes),
    SettingsEntry("UI scale", "Appearance", "appearance_settings", "scale size zoom bigger smaller density text display", Res.string.settings_ui_scale),
    SettingsEntry("Tablet layout", "Appearance", "appearance_settings", "tablet wide layout side rail landscape big screen split pane", Res.string.settings_tablet_layout),
    SettingsEntry("System font", "Appearance", "appearance_settings", "font typeface text system phone default inter", Res.string.settings_system_font),

    // Customisation
    SettingsEntry("Customisation", "Customisation", "customisation_settings", "customize cover rounding playlist player home styles gesture swipe blur animation lyrics"),
    SettingsEntry("Cover rounding", "Customisation", "customisation_settings", "square rounded corner radius px thumbnail art"),
    SettingsEntry("Playlist styles", "Customisation", "customisation_settings", "flow classic minimal header layout"),
    SettingsEntry("Player styles", "Customisation", "customisation_settings", "flagship turntable ipod poster timeline lyrics studio customize player cover"),
    SettingsEntry("Home styles", "Customisation", "customisation_settings", "mochyy listen now hop in hero layout grid"),
    SettingsEntry("Blur", "Customisation", "customisation_settings", "blur glass frosted performance lag"),
    SettingsEntry("Progressive blur", "Customisation", "customisation_settings", "gradient fade top bar hairline flat"),
    SettingsEntry("Blur quality", "Customisation", "customisation_settings", "low high glass performance"),
    SettingsEntry("Blur strength", "Customisation", "customisation_settings", "radius px glass amount"),
    SettingsEntry("Element blur", "Customisation", "customisation_settings", "lyrics line blur cover zoom title morph menu page wipe"),
    SettingsEntry("Animation", "Customisation", "customisation_settings", "transition motion off reduce disable"),
    SettingsEntry("Animation speed", "Customisation", "customisation_settings", "faster slower duration scale motion"),
    SettingsEntry("Lyrics animation", "Customisation", "customisation_settings", "lyrics style 1 2 boring plain shader glow wave agsl letters"),
    SettingsEntry("Top Bar on player", "Customisation", "customisation_settings", "status bar hide clock battery player top"),
    SettingsEntry("Gesture", "Customisation", "customisation_settings", "swipe slide song list left right queue library download play next playlist details"),
    SettingsEntry("Swipe right", "Customisation", "customisation_settings", "slide song list queue library download play next playlist details action"),
    SettingsEntry("Swipe left", "Customisation", "customisation_settings", "slide song list queue library download play next playlist details action"),
    SettingsEntry("Swipe down anywhere to close", "Customisation", "customisation_settings", "player close dismiss swipe down pull gesture anywhere"),
    SettingsEntry("Swipe up opens the queue", "Customisation", "customisation_settings", "swipe up queue lyrics open player gesture", Res.string.customise_swipe_up_queue),
    SettingsEntry("Open with your finger", "Customisation", "customisation_settings", "player open swipe up mini player follow finger speed velocity gesture"),

    // App behaviour
    SettingsEntry("App behaviour", "App behaviour", "general_settings", "general app language updates network data library privacy"),
    SettingsEntry("Larp mode", "App behaviour", "general_settings", "larp 67 flac 768 khz wadax atlantis dac hi-res lossless bit-perfect flex"),
    SettingsEntry("App language", "App behaviour", "general_settings", "language bahasa locale device system indonesia english translate", Res.string.app_language),
    SettingsEntry("Disable App Updates notice", "App behaviour", "general_settings", "update popup notification updater"),
    SettingsEntry("Disable description on settings", "App behaviour", "general_settings", "hide subtitle text rows titles only"),
    SettingsEntry("Hide explicit content", "App behaviour", "general_settings", "explicit content songs filter hide clean family", Res.string.hide_explicit),
    SettingsEntry("Hide music videos", "App behaviour", "general_settings", "music video mv videos filter hide clip"),
    SettingsEntry("Pause listen history", "App behaviour", "general_settings", "listen history pause private incognito privacy youtube plays stop"),
    SettingsEntry("Disable screenshot", "App behaviour", "general_settings", "screenshot screen capture recents privacy secure block"),
    SettingsEntry("Offline mode", "App behaviour", "general_settings", "play downloaded cache offline"),
    SettingsEntry("Data saver", "App behaviour", "general_settings", "data saver mobile metered cellular bandwidth lower quality canvas video"),
    SettingsEntry("Internet", "App behaviour", InternetSettingsRoute, "internet region country dns doh proxy wifi cellular quality blocked spoof"),
    SettingsEntry("YouTube Music region", "App behaviour", InternetSettingsRoute, "region country geo block spoof youtube music"),
    SettingsEntry("DNS over HTTPS", "App behaviour", InternetSettingsRoute, "dns doh https cloudflare google adguard quad9 resolver"),
    SettingsEntry("Audio quality on Wi-Fi", "App behaviour", InternetSettingsRoute, "quality wifi ceiling stream mobile data cellular"),
    SettingsEntry("Proxy", "App behaviour", ProxySettingsRoute, "proxy socks http server vpn network region country blocked bot sign in"),
    SettingsEntry("Albums from liked songs", "App behaviour", "general_settings", "albums library liked songs old way apple music"),
    SettingsEntry("Local song auto metadata", "App behaviour", "general_settings", "match tags id3 covers lyrics"),
    SettingsEntry("Pause search history", "App behaviour", "general_settings", "search history pause private incognito privacy", Res.string.pause_search_history),
    SettingsEntry("Count plays on YouTube views", "App behaviour", "general_settings", "youtube history plays views report count privacy", Res.string.settings_youtube_plays),
    SettingsEntry("Override lyrics sorting", "App behaviour", "general_settings", "lyrics provider order sort priority duet word sync apple music source", Res.string.settings_lyrics_order_override),

    // Playback
    SettingsEntry("Playback", "Playback", PlaybackSettingsRoute, "background bluetooth shuffle skip silence mute queue failure"),
    SettingsEntry("Keep playing even when closed", "Playback", PlaybackSettingsRoute, "background close stop playback"),
    SettingsEntry("Play alongside other apps", "Playback", PlaybackSettingsRoute, "audio focus mix pause other app video game sound together"),
    SettingsEntry("Resume on Bluetooth connection", "Playback", PlaybackSettingsRoute, "bluetooth headphones earbuds car connect resume auto play start", Res.string.playback_resume_bluetooth),
    SettingsEntry("Pause when muted", "Playback", PlaybackSettingsRoute, "pause mute volume silent zero off automatic", Res.string.pause_music_when_media_is_muted),
    SettingsEntry("Mute recovery volume", "Playback", PlaybackSettingsRoute, "pause mute recovery volume restore play zero silent percent", Res.string.settings_mute_recovery_volume),
    SettingsEntry("Preload queue lyrics", "Playback", PlaybackSettingsRoute, "lyrics preload prefetch next queued songs unmetered wifi", Res.string.playback_preload_lyrics),
    SettingsEntry("Save Player's last state", "Playback", PlaybackSettingsRoute, "persistent queue restore track state"),
    SettingsEntry("Infinite queue", "Playback", PlaybackSettingsRoute, "autoplay radio related endless queue songs keep going never stop", Res.string.playback_infinite_queue),
    SettingsEntry("Keep CPU awake during playback", "Playback", PlaybackSettingsRoute, "cpu wake lock awake screen off background sleep stop music battery", Res.string.settings_keep_cpu_awake),
    SettingsEntry("Prevent duplicates in queue", "Playback", PlaybackSettingsRoute, "queue duplicate copy twice same song move again", Res.string.settings_queue_prevent_duplicates),
    SettingsEntry("Add to queue in order", "Playback", PlaybackSettingsRoute, "queue play next order sequence keep add later", Res.string.settings_queue_add_in_order),
    SettingsEntry("Lock queue", "Playback", PlaybackSettingsRoute, "queue lock remove move swipe reorder protect", Res.string.settings_queue_lock),
    SettingsEntry("SponsorBlock", "Playback", PlaybackSettingsRoute, "sponsorblock sponsor skip segments non-music intro outro self promotion youtube", Res.string.settings_sponsorblock),
    SettingsEntry("SponsorBlock non-music sections", "Playback", PlaybackSettingsRoute, "sponsorblock off topic music non-music skip", Res.string.settings_sponsorblock_offtopic),
    SettingsEntry("SponsorBlock sponsor reads", "Playback", PlaybackSettingsRoute, "sponsorblock sponsor advert ad read skip", Res.string.settings_sponsorblock_sponsor),
    SettingsEntry("SponsorBlock self-promotion", "Playback", PlaybackSettingsRoute, "sponsorblock self promo promotion skip", Res.string.settings_sponsorblock_selfpromo),
    SettingsEntry("SponsorBlock intros", "Playback", PlaybackSettingsRoute, "sponsorblock intro start skip", Res.string.settings_sponsorblock_intro),
    SettingsEntry("SponsorBlock outros", "Playback", PlaybackSettingsRoute, "sponsorblock outro end credits skip", Res.string.settings_sponsorblock_outro),
    SettingsEntry("Picture-in-picture", "Playback", PlaybackSettingsRoute, "video music pip picture in picture window float leave app background", Res.string.playback_pip),
    SettingsEntry("Keep shuffle on", "Playback", PlaybackSettingsRoute, "shuffle persistent spotify random keep new playlist"),
    SettingsEntry("Preload next song", "Playback", PlaybackSettingsRoute, "queue next track buffer preload unmetered wi-fi"),
    SettingsEntry("Audio offload", "Playback", PlaybackSettingsRoute, "battery offload decode chip power efficiency hardware"),
    SettingsEntry("Efficient decoding", "Playback", PlaybackSettingsRoute, "battery decode decoder ffmpeg aac opus codec power efficient in-app"),
    SettingsEntry("Skip Silence", "Playback", PlaybackSettingsRoute, "silence trim gap interval"),
    SettingsEntry("Don't skip on failure", "Playback", PlaybackSettingsRoute, "fail stream error stay skip"),
    SettingsEntry("Skip timeout failure", "Playback", PlaybackSettingsRoute, "skip timeout seconds failed stream"),

    // Now playing
    SettingsEntry("Now playing", "Now playing", NowPlayingSettingsRoute, "player lyrics video canvas badge format"),
    SettingsEntry("Hide player thumbnail", "Now playing", NowPlayingSettingsRoute, "hide thumbnail artwork cover album art logo player", Res.string.settings_hide_player_thumbnail),
    SettingsEntry("Audio format badge", "Now playing", NowPlayingSettingsRoute, "format codec opus aac flac badge title quality"),
    SettingsEntry("Show sample rate", "Now playing", NowPlayingSettingsRoute, "sample rate khz hz format badge quality"),
    SettingsEntry("Translations from YouTube", "Now playing", NowPlayingSettingsRoute, "captions translate lyrics language"),
    SettingsEntry("AI translator", "Now playing", AiTranslatorSettingsRoute, "translate lyrics ai openrouter deepl custom endpoint model api key provider openai", Res.string.settings_ai_translator),
    SettingsEntry("Translate lyrics automatically", "Now playing", AiTranslatorSettingsRoute, "translate lyrics automatic auto language on load", Res.string.settings_translate_auto),
    SettingsEntry("Sync lyrics to music videos", "Now playing", NowPlayingSettingsRoute, "lyrics video timing intro skit"),
    SettingsEntry("Romanisation as the main line", "Now playing", NowPlayingSettingsRoute, "romaji romanization pinyin latin lyrics japanese korean chinese", Res.string.settings_romanize_as_main),
    SettingsEntry("Romanize Japanese lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics japanese romaji japanese kana kanji", Res.string.lyrics_romanize_japanese),
    SettingsEntry("Romanize Korean lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics korean hangul korean", Res.string.lyrics_romanize_korean),
    SettingsEntry("Romanize Chinese lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics chinese pinyin chinese han hanzi", Res.string.lyrics_romanize_chinese),
    SettingsEntry("Romanize Russian lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics russian cyrillic russian", Res.string.lyrics_romanize_russian),
    SettingsEntry("Romanize Ukrainian lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics ukrainian cyrillic ukrainian", Res.string.lyrics_romanize_ukrainian),
    SettingsEntry("Romanize Serbian lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics serbian cyrillic serbian latin", Res.string.lyrics_romanize_serbian),
    SettingsEntry("Romanize Bulgarian lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics bulgarian cyrillic bulgarian", Res.string.lyrics_romanize_bulgarian),
    SettingsEntry("Romanize Belarusian lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics belarusian cyrillic belarusian", Res.string.lyrics_romanize_belarusian),
    SettingsEntry("Romanize Kyrgyz lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics kyrgyz cyrillic kyrgyz", Res.string.lyrics_romanize_kyrgyz),
    SettingsEntry("Romanize Macedonian lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics macedonian cyrillic macedonian", Res.string.lyrics_romanize_macedonian),
    SettingsEntry("Romanize Hindi lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics hindi devanagari hindi", Res.string.lyrics_romanize_hindi),
    SettingsEntry("Romanize Punjabi lyrics", "Now playing", NowPlayingSettingsRoute, "romanize romanise lyrics punjabi gurmukhi punjabi", Res.string.lyrics_romanize_punjabi),
    SettingsEntry("Change lyrics on click", "Now playing", NowPlayingSettingsRoute, "lyrics tap seek jump line click", Res.string.lyrics_click_change),
    SettingsEntry("Auto scroll lyrics", "Now playing", NowPlayingSettingsRoute, "lyrics scroll follow current line back", Res.string.lyrics_auto_scroll),
    SettingsEntry("High quality bloom", "Now playing", NowPlayingSettingsRoute, "lyrics bloom quality extra soft pass glow"),
    SettingsEntry("Tinted lyrics", "Now playing", NowPlayingSettingsRoute, "lyrics soft light blend tint colour color cover inactive grey gray lines"),
    SettingsEntry("Floating lyrics overlay bubble", "Now playing", NowPlayingSettingsRoute, "floating lyrics overlay bubble pill other apps draw over", Res.string.settings_floating_lyrics),
    SettingsEntry("Always enable Youtube background", "Now playing", NowPlayingSettingsRoute, "youtube video background canvas"),
    SettingsEntry("Video quality", "Now playing", NowPlayingSettingsRoute, "video quality resolution 4k 2160p 1440p 1080p 720p 480p sharp", Res.string.video_quality),
    SettingsEntry("Disable auto-playing YouTube video", "Now playing", NowPlayingSettingsRoute, "music video live autoplay cover"),
    SettingsEntry("Studio version for music videos", "Now playing", NowPlayingSettingsRoute, "music video mv omv audio studio album version skit sound effects official", Res.string.settings_studio_for_videos),
    SettingsEntry("Enable animated canvas", "Now playing", NowPlayingSettingsRoute, "canvas animation spotify looping visual"),
    SettingsEntry("Canvas order", "Now playing", NowPlayingSettingsRoute, "canvas order apple music tidal spotify source priority", Res.string.canvas_order),
    SettingsEntry("Canvas on Wi-Fi only", "Now playing", NowPlayingSettingsRoute, "canvas wifi mobile data", Res.string.appearance_canvas_wifi),
    SettingsEntry("Sharpest animated covers", "Now playing", NowPlayingSettingsRoute, "canvas animated cover motion apple music quality hd 4k sharp resolution", Res.string.canvas_best_quality),

    // Audio Lab
    SettingsEntry("Audio Lab", "Audio Lab", "audio_lab", "sound effects equalizer fx quality"),
    SettingsEntry("Transition", "Audio Lab", "crossmix_settings", "crossfade crossmix automix transition gapless blend"),
    SettingsEntry("Blend skips", "Audio Lab", "crossmix_settings", "skip next previous back crossfade seamless preload transition fade blend"),
    SettingsEntry("Equalizer", "Audio Lab", "equalizer", "eq bass treble band parametric graph autoeq headphones correction preset"),
    SettingsEntry("AutoEQ", "Audio Lab", "equalizer", "headphones earbuds correction harman database model"),
    SettingsEntry("Audio Quality", "Audio Lab", "audio_quality", "bitrate stream high low lossless hi-res flac monochrome"),
    SettingsEntry("Audio Pass", "Audio Lab", "audio_pass", "dac usb output headphones bluetooth codec bit perfect exclusive"),
    SettingsEntry("Prefer USB DAC", "Audio Lab", "audio_pass", "usb dac headphones output external audio"),
    SettingsEntry("USB DAC passthrough", "Audio Lab", "audio_pass", "bit perfect bitperfect passthrough sample rate dac usb resample"),
    SettingsEntry("Hi-res output", "Audio Lab", "audio_pass", "hi res 24 bit float precision lossless flac quality"),
    SettingsEntry("Hi-fi resampler", "Audio Lab", "audio_pass", "resample sample rate 44.1 48 khz sinc src quality filter"),
    SettingsEntry("Reverb", "Audio Lab", "audio_fx", "room hall echo space fx effect"),
    SettingsEntry("Loudness", "Audio Lab", "audio_fx", "boost louder compressor limiter night mode fx"),
    SettingsEntry("Audio Normalization", "Audio Lab", "audio_fx", "even out songs loudness volume gain balance replaygain"),
    SettingsEntry("Spatial audio", "Audio Lab", "audio_fx", "crossfeed binaural hrtf virtual speakers 3d headphones"),
    SettingsEntry("Dolby Atmos", "Audio Lab", "audio_fx", "atmos spatial surround 3d immersive"),
    SettingsEntry("Playback speed", "Audio Lab", "audio_fx", "tempo pitch slowed nightcore preserve pitch"),
    SettingsEntry("Exponential volume", "Audio Lab", "audio_fx", "volume curve logarithmic limit"),
    SettingsEntry("Modules", "Modules", "modules", "apple music source module plugin extension stream subscription"),
    SettingsEntry("Apple Music", "Modules", "module/applemusic", "apple music itunes subscription stream aac"),
    SettingsEntry("Monochrome", "Modules", "module/monochrome", "monochrome lossless flac hi-res tidal"),
    SettingsEntry("Provider order", "Audio Lab", "audio_quality", "source order apple music monochrome youtube priority"),
    SettingsEntry("YouTube Music as a source", "Audio Lab", "audio_quality", "youtube turn off disable provider only monochrome apple music", Res.string.settings_index_youtube_source),

    // Integrations
    SettingsEntry("Android Auto", "Integrations", AndroidAutoSettingsRoute, "car carplay driving upcoming recommendations voice artwork lyrics mobile data"),
    SettingsEntry("Songs on this device", "Integrations", AndroidAutoSettingsRoute, "car library folder local downloaded files phone"),
    SettingsEntry("Stream on mobile data", "Integrations", AndroidAutoSettingsRoute, "car metered mobile data cellular streaming offline saved cached"),
    SettingsEntry("Pinned playlists in the car", "Integrations", AndroidAutoSettingsRoute, "car library pin playlists order first top"),
    SettingsEntry("Discord", "Integrations", DiscordRoute, "discord rich presence rpc status activity listening playing profile behaviour pause clear"),
    SettingsEntry("Last.fm", "Integrations", LastFmRoute, "lastfm scrobble scrobbling history stats account sign in"),
    SettingsEntry("Libre.fm", "Integrations", LibreFmRoute, "librefm scrobble scrobbling history stats account sign in"),
    SettingsEntry("Now Playing", "Integrations", LastFmRoute, "scrobble now playing status profile lastfm librefm"),
    SettingsEntry("Send likes", "Integrations", LastFmRoute, "love loved songs lastfm librefm scrobble"),
    SettingsEntry("YouTube Music sync", "Integrations", YouTubeSyncRoute, "youtube ytm sync account sign in likes library playlists history", Res.string.settings_index_youtube_sync),
    SettingsEntry("Account switcher", "Integrations", AccountSwitcherRoute, "account switcher switch account multiple accounts saved accounts add remove youtube"),

    // Storage
    SettingsEntry("Storage", "Storage", "storage_settings", "space disk size cache downloads clear free junk delete"),
    SettingsEntry("Music cache limit", "Storage", "storage_settings", "cache size limit space song stream"),
    SettingsEntry("Image cache limit", "Storage", "storage_settings", "image cache size limit covers pictures artwork space"),
    SettingsEntry("Manage downloads", "Storage", "storage_settings", "downloads delete remove biggest offline space"),
    SettingsEntry("Remove all downloads", "Storage", "storage_settings", "delete clear downloads offline space"),

    // Experimental
    SettingsEntry("Device integration", "Import & sync", DeviceIntegrationRoute, "tv television android tv google tv phone tablet car head unit pair link sync stream downloads big screen remote control devices"),
    SettingsEntry("Import & sync", "Experimental", "experimental_settings", "import spotify apple music deezer tidal tunemymusic csv youtube sync export backup transfer"),
    SettingsEntry("Big disc cover styles", "Experimental", "experimental_settings", "disc vinyl cover"),
    SettingsEntry("Music haptics", "Experimental", "experimental_settings", "vibration haptic"),
    SettingsEntry("Cipher", "Experimental", "experimental_settings", "player script signature refresh"),
    SettingsEntry("Playback diagnostics", "Experimental", "experimental_settings", "logs potoken stream client debug"),
    SettingsEntry("Repeat initial setup", "Experimental", "experimental_settings", "onboarding wizard setup"),
    SettingsEntry("Show where songs play from", "Experimental", "experimental_settings", "source banner toast apple music monochrome youtube"),
    SettingsEntry("Force playback", "Experimental", "experimental_settings", "fallback skip error stream failed youtube"),
)

// pages only the app can open yet; desktop and iOS leave their rows and their search hits out
private fun SettingsEntry.availableOn(platform: SettingsListPlatform): Boolean = when (page) {
    "Audio Lab" -> platform.audioLab
    "Customisation" -> platform.customisation
    "Modules" -> platform.modules
    "Import & sync" -> title != "Device integration" || platform.devices
    else -> true
}

val BubbleColor = Color(0xFF1C1C1E)
private val SelectedBubbleColor = Color(0xFF3A3A3E)

// the header holds the mochy mark with the big "Settings" under it, taller than search's title-only header
private val SettingsTitleBlockHeight = 88.dp
private val SettingsMarkSize = 32.dp
val IconBgColor = Color(0xFF333338)

@Composable
fun MochySettingsScreen(
    navController: NavController,
    selectedRoute: String? = null,
    chrome: MochySettingsChrome = PlainMochySettingsChrome,
    platform: SettingsListPlatform = SettingsListPlatform(),
    /** the page the list opened, beside it on a wide screen; null navigates the single screen instead */
    onOpenPage: ((String) -> Unit)? = null,
    /** the app's own tips card; absent on desktop and iOS */
    recommendations: (@Composable (open: (String) -> Unit) -> Unit)? = null,
    /** the app's module store sheet; absent where the module system doesn't run */
    onOpenModuleStore: (() -> Unit)? = null,
    /** the module pages switched-on modules put under the modules row */
    modulePages: (@Composable (open: (String) -> Unit, selected: String?) -> Unit)? = null,
    /** the device integration sheet, opened from its row instead of navigating */
    onOpenDevices: (() -> Unit)? = null,
    /** Android goes back to the tab page it was opened from; other platforms navigate to settings */
    onProfileClick: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    // on a wide screen the list stays put and its pages open beside it; device integration is a sheet over either
    val open: (String) -> Unit = { route ->
        when {
            route == DeviceIntegrationRoute && onOpenDevices != null -> onOpenDevices.invoke()
            onOpenPage != null -> onOpenPage.invoke(route)
            else -> navController.navigate(route)
        }
    }
    val selected = selectedRoute
    val scrollState = rememberScrollState()
    val glass = chrome.rememberGlass()

    val (profilePicUri) = rememberPreference(ProfilePicUriKey, "")

    var query by remember { mutableStateOf(TextFieldValue()) }
    var isSearchFocused by remember { mutableStateOf(false) }

    val isSearching = isSearchFocused || query.text.isNotBlank()

    BackPressHandler(enabled = isSearching) {
        query = TextFieldValue()
        isSearchFocused = false
        focusManager.clearFocus()
    }

    val index = remember(platform) { SettingsIndex.filter { it.availableOn(platform) } }
    // rows keep their english ids; these are what's shown and what a search matches, in the app's language
    val titleTexts = mutableMapOf<String, String>()
    for (entry in SettingsIndex) {
        val res = entry.titleRes ?: LocalizedSettingsTitles[entry.title]
        if (res != null) titleTexts[entry.title] = stringResource(res)
    }
    val pageTexts = mutableMapOf<String, String>()
    for (entry in SettingsIndex) {
        val res = entry.pageRes ?: LocalizedSettingsPages[entry.page]
        if (res != null) pageTexts[entry.page] = stringResource(res)
    }
    val results = remember(query.text, index, titleTexts, pageTexts) {
        val q = query.text.trim()
        if (q.isBlank()) {
            emptyList()
        } else {
            index.filter {
                it.title.contains(q, ignoreCase = true) ||
                    titleTexts[it.title]?.contains(q, ignoreCase = true) == true ||
                    pageTexts[it.page]?.contains(q, ignoreCase = true) == true ||
                    it.keywords.contains(q, ignoreCase = true) ||
                    it.page.contains(q, ignoreCase = true)
            }
        }
    }

    // Same latch-and-settle collapse as Library/Search: crossing the threshold always finishes
    // the morph, even after the user stops scrolling mid-gesture.
    val collapseProgress by rememberScrollCollapseProgress(scrollState)

    // Animated fade for content when search bar is tapped
    val contentAlpha by animateFloatAsState(
        targetValue = if (isSearching) 0f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "contentAlpha"
    )
    // flips only at the ends of the fade, so the page isn't recomposed on every frame of it
    val showContent by remember { derivedStateOf { contentAlpha > 0.001f } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(chrome.backdrop(glass, isActive = { !scrollState.isScrollInProgress && collapseProgress > 0f })),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(chrome.topBarHeight(SettingsTitleBlockHeight)))

            // Search results view when user types
            AnimatedVisibility(
                visible = query.text.isNotBlank(),
                enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(160, easing = FastOutSlowInEasing)) { 20 },
                exit = fadeOut(tween(120, easing = FastOutSlowInEasing))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (results.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.settings_nothing_matches, query.text),
                            fontFamily = InterFontFamily,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
                        )
                    } else {
                        results.forEach { entry ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(settingsBubbleColor(selected = false))
                                    .clickable {
                                        SettingsHighlight.pending = entry.highlightKey
                                        open(entry.route)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(IconBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.settings),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(11.dp))
                                    Column {
                                        Text(
                                            text = titleTexts[entry.title] ?: entry.title,
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = pageTexts[entry.page] ?: entry.page,
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.55f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Normal Settings Content (fades smoothly to black when search is focused)
            if (showContent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha }
                ) {
                    recommendations?.invoke(open)

                    val moduleStore = onOpenModuleStore?.takeIf { platform.modules }
                    if (moduleStore != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        ModuleStoreCard(
                            onClick = moduleStore,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    if (recommendations != null || moduleStore != null) {
                        Spacer(modifier = Modifier.height(18.dp))

                        // Divider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(0.8.dp)
                                .background(Color.White.copy(alpha = 0.1f))
                        )

                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // Separate Bubble Pills for Categories (No outlines, individually bubble-wrapped)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // in the order of the design: sound first, then how the app acts and looks, then the rest
                        if (platform.audioLab) {
                            CategoryBubble(
                                title = stringResource(Res.string.settings_audio_lab_category),
                                icon = painterResource(Res.drawable.equalizer),
                                selected = selected == "audio_lab",
                                onClick = { open("audio_lab") }
                            )
                        }

                        CategoryBubble(
                            title = stringResource(Res.string.settings_playback_category),
                            icon = painterResource(Res.drawable.play),
                            selected = selected == PlaybackSettingsRoute,
                            onClick = { open(PlaybackSettingsRoute) }
                        )

                        CategoryBubble(
                            title = stringResource(Res.string.settings_now_playing),
                            icon = painterResource(Res.drawable.lyrics),
                            selected = selected == NowPlayingSettingsRoute,
                            onClick = { open(NowPlayingSettingsRoute) }
                        )

                        CategoryBubble(
                            title = stringResource(Res.string.settings_app_behaviour),
                            icon = painterResource(Res.drawable.settings),
                            selected = selected == "general_settings",
                            onClick = { open("general_settings") }
                        )

                        CategoryBubble(
                            title = stringResource(Res.string.settings_appearance),
                            icon = painterResource(Res.drawable.contrast),
                            selected = selected == "appearance_settings",
                            onClick = { open("appearance_settings") }
                        )

                        if (platform.customisation) {
                            CategoryBubble(
                                title = stringResource(Res.string.settings_customisation_category),
                                icon = painterResource(Res.drawable.palette),
                                selected = selected == "customisation_settings",
                                onClick = { open("customisation_settings") }
                            )
                        }

                        CategoryBubble(
                            title = stringResource(Res.string.storage),
                            icon = painterResource(Res.drawable.storage),
                            selected = selected == "storage_settings",
                            onClick = { open("storage_settings") }
                        )

                        CategoryBubble(
                            title = stringResource(Res.string.internet),
                            icon = painterResource(Res.drawable.language),
                            selected = selected == InternetSettingsRoute,
                            onClick = { open(InternetSettingsRoute) }
                        )

                        CategoryBubble(
                            title = stringResource(Res.string.integrations),
                            icon = painterResource(Res.drawable.integration),
                            selected = selected == IntegrationsRoute,
                            onClick = { open(IntegrationsRoute) }
                        )

                        if (platform.devices && onOpenDevices != null) {
                            CategoryBubble(
                                title = stringResource(Res.string.devices_title),
                                icon = painterResource(Res.drawable.cast_connected),
                                selected = false,
                                onClick = { open(DeviceIntegrationRoute) }
                            )
                        }

                        if (platform.modules) {
                            CategoryBubble(
                                title = stringResource(Res.string.settings_modules_category),
                                icon = painterResource(Res.drawable.library_add),
                                selected = selected == "modules",
                                onClick = { open("modules") }
                            )

                            // pages the modules that are on bring to settings, right under modules
                            modulePages?.invoke(open, selected)
                        }
                    }
                }
            }

            val bottomInset = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            val bottomPadding = bottomInset.calculateBottomPadding()
            val bottomSpacerHeight = maxOf(340.dp, bottomPadding + 160.dp)

            // Headroom spacer for smooth scrolling past collapse threshold and bottom bar
            Spacer(modifier = Modifier.height(bottomSpacerHeight))
        }
        }

        // Top bar: Mochy mark + "Settings" title slide away on scroll (progressive glass blur
        // behind the gradient, matching Library/Search) while the search field glides up to sit
        // beside the profile avatar.
        chrome.TopBar(
            glass = glass,
            collapseProgress = { collapseProgress },
            title = stringResource(Res.string.settings),
            titleBlockHeight = SettingsTitleBlockHeight,
            titleContent = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_mochy_mark),
                        contentDescription = "Mochy",
                        tint = Color.White,
                        modifier = Modifier.size(SettingsMarkSize),
                    )
                    Text(
                        text = stringResource(Res.string.settings),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            },
            profilePicUri = profilePicUri,
            onAvatarClick = {
                val click = onProfileClick
                if (click != null) click() else navController.navigate("settings")
            },
            query = query,
            onQueryChange = { query = it },
            isSearchFocused = isSearchFocused,
            onFocusChanged = { isSearchFocused = it },
        )
    }
}

@Composable
private fun settingsBubbleColor(selected: Boolean): Color = when {
    LocalMochyAccent.current != null && selected -> MaterialTheme.colorScheme.primaryContainer
    LocalMochyAccent.current != null -> MaterialTheme.colorScheme.surfaceContainer
    selected -> SelectedBubbleColor
    else -> BubbleColor
}

@Composable
fun CategoryBubble(
    title: String,
    icon: Painter,
    onClick: () -> Unit,
    // lit while its page is open beside the list on a wide screen
    selected: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(settingsBubbleColor(selected))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = Color.White,
            )
        }
    }
}
