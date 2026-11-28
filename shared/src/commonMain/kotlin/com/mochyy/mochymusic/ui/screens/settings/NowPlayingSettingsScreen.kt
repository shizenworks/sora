package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.CanvasBestQualityKey
import com.mochyy.mochymusic.constants.CanvasOrder
import com.mochyy.mochymusic.constants.CanvasOrderKey
import com.mochyy.mochymusic.constants.CanvasSource
import com.mochyy.mochymusic.constants.CanvasThumbnailAnimationKey
import com.mochyy.mochymusic.constants.CanvasWifiOnlyKey
import com.mochyy.mochymusic.constants.DisableVideoAutoplayKey
import com.mochyy.mochymusic.constants.HidePlayerThumbnailKey
import com.mochyy.mochymusic.constants.LyricsClickKey
import com.mochyy.mochymusic.constants.LyricsHighBloomKey
import com.mochyy.mochymusic.constants.LyricsInterludeDotsKey
import com.mochyy.mochymusic.constants.LyricsLineBounceKey
import com.mochyy.mochymusic.constants.LyricsMotionStyle
import com.mochyy.mochymusic.constants.LyricsMotionStyleKey
import com.mochyy.mochymusic.constants.LyricsRomanizeAsMainKey
import com.mochyy.mochymusic.constants.LyricsRomanizeBelarusianKey
import com.mochyy.mochymusic.constants.LyricsRomanizeBulgarianKey
import com.mochyy.mochymusic.constants.LyricsRomanizeChineseKey
import com.mochyy.mochymusic.constants.LyricsRomanizeHindiKey
import com.mochyy.mochymusic.constants.LyricsRomanizeJapaneseKey
import com.mochyy.mochymusic.constants.LyricsRomanizeKoreanKey
import com.mochyy.mochymusic.constants.LyricsRomanizeKyrgyzKey
import com.mochyy.mochymusic.constants.LyricsRomanizeMacedonianKey
import com.mochyy.mochymusic.constants.LyricsRomanizePunjabiKey
import com.mochyy.mochymusic.constants.LyricsRomanizeRussianKey
import com.mochyy.mochymusic.constants.LyricsRomanizeSerbianKey
import com.mochyy.mochymusic.constants.LyricsRomanizeUkrainianKey
import com.mochyy.mochymusic.constants.LyricsScrollKey
import com.mochyy.mochymusic.constants.LyricsSoftLightKey
import com.mochyy.mochymusic.constants.PlayVideoBackgroundKey
import com.mochyy.mochymusic.constants.ShowFormatBadgeKey
import com.mochyy.mochymusic.constants.ShowFormatBitDepthKey
import com.mochyy.mochymusic.constants.ShowFormatSampleRateKey
import com.mochyy.mochymusic.constants.SpotifyCanvasKey
import com.mochyy.mochymusic.constants.StudioForVideosKey
import com.mochyy.mochymusic.constants.SyncLyricsToVideoKey
import com.mochyy.mochymusic.constants.VideoQuality
import com.mochyy.mochymusic.constants.VideoQualityKey
import com.mochyy.mochymusic.constants.YouTubeCaptionTranslationKey
import com.mochyy.mochymusic.constants.YtVideoBackgroundLyricsSyncKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.appearance_bit_depth
import com.mochyy.mochymusic.shared.resources.appearance_bit_depth_desc
import com.mochyy.mochymusic.shared.resources.appearance_bloom
import com.mochyy.mochymusic.shared.resources.appearance_bloom_desc
import com.mochyy.mochymusic.shared.resources.appearance_canvas
import com.mochyy.mochymusic.shared.resources.appearance_canvas_desc
import com.mochyy.mochymusic.shared.resources.appearance_canvas_wifi
import com.mochyy.mochymusic.shared.resources.appearance_canvas_wifi_desc
import com.mochyy.mochymusic.shared.resources.appearance_format_badge
import com.mochyy.mochymusic.shared.resources.appearance_format_badge_desc
import com.mochyy.mochymusic.shared.resources.appearance_lyrics_animation
import com.mochyy.mochymusic.shared.resources.appearance_lyrics_soft_light
import com.mochyy.mochymusic.shared.resources.appearance_lyrics_soft_light_desc
import com.mochyy.mochymusic.shared.resources.appearance_sample_rate
import com.mochyy.mochymusic.shared.resources.appearance_sample_rate_desc
import com.mochyy.mochymusic.shared.resources.appearance_timestamp_matching
import com.mochyy.mochymusic.shared.resources.appearance_timestamp_matching_desc
import com.mochyy.mochymusic.shared.resources.appearance_youtube_background
import com.mochyy.mochymusic.shared.resources.appearance_youtube_background_desc
import com.mochyy.mochymusic.shared.resources.canvas_best_quality
import com.mochyy.mochymusic.shared.resources.canvas_best_quality_desc
import com.mochyy.mochymusic.shared.resources.canvas_order
import com.mochyy.mochymusic.shared.resources.canvas_source_apple
import com.mochyy.mochymusic.shared.resources.canvas_source_spotify
import com.mochyy.mochymusic.shared.resources.canvas_source_tidal
import com.mochyy.mochymusic.shared.resources.graphic_eq
import com.mochyy.mochymusic.shared.resources.material_rounded_high_quality
import com.mochyy.mochymusic.shared.resources.hide_image
import com.mochyy.mochymusic.shared.resources.lyrics
import com.mochyy.mochymusic.shared.resources.lyrics_auto_scroll
import com.mochyy.mochymusic.shared.resources.lyrics_auto_scroll_desc
import com.mochyy.mochymusic.shared.resources.lyrics_click_change
import com.mochyy.mochymusic.shared.resources.lyrics_click_change_desc
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
import com.mochyy.mochymusic.shared.resources.music_note
import com.mochyy.mochymusic.shared.resources.palette
import com.mochyy.mochymusic.shared.resources.settings_ai_translator
import com.mochyy.mochymusic.shared.resources.settings_ai_translator_desc
import com.mochyy.mochymusic.shared.resources.settings_disable_video_autoplay
import com.mochyy.mochymusic.shared.resources.settings_disable_video_autoplay_desc
import com.mochyy.mochymusic.shared.resources.settings_floating_lyrics
import com.mochyy.mochymusic.shared.resources.settings_floating_lyrics_desc
import com.mochyy.mochymusic.shared.resources.settings_floating_lyrics_detail
import com.mochyy.mochymusic.shared.resources.settings_hide_bottom_bar
import com.mochyy.mochymusic.shared.resources.settings_hide_bottom_bar_desc
import com.mochyy.mochymusic.shared.resources.settings_hide_player_thumbnail
import com.mochyy.mochymusic.shared.resources.settings_hide_player_thumbnail_desc
import com.mochyy.mochymusic.shared.resources.settings_hide_top_bar
import com.mochyy.mochymusic.shared.resources.settings_hide_top_bar_desc
import com.mochyy.mochymusic.shared.resources.settings_lyrics_animation_link_desc
import com.mochyy.mochymusic.shared.resources.settings_lyrics_interlude_dots
import com.mochyy.mochymusic.shared.resources.settings_lyrics_interlude_dots_desc
import com.mochyy.mochymusic.shared.resources.settings_lyrics_line_bounce
import com.mochyy.mochymusic.shared.resources.settings_lyrics_line_bounce_desc
import com.mochyy.mochymusic.shared.resources.settings_now_playing
import com.mochyy.mochymusic.shared.resources.settings_on_the_player
import com.mochyy.mochymusic.shared.resources.settings_related
import com.mochyy.mochymusic.shared.resources.settings_romanize_as_main
import com.mochyy.mochymusic.shared.resources.settings_romanize_as_main_desc
import com.mochyy.mochymusic.shared.resources.settings_romanize_language_desc
import com.mochyy.mochymusic.shared.resources.settings_section_canvas
import com.mochyy.mochymusic.shared.resources.settings_section_lyrics
import com.mochyy.mochymusic.shared.resources.settings_section_videos
import com.mochyy.mochymusic.shared.resources.settings_studio_for_videos
import com.mochyy.mochymusic.shared.resources.settings_studio_for_videos_desc
import com.mochyy.mochymusic.shared.resources.settings_sync_lyrics_video
import com.mochyy.mochymusic.shared.resources.settings_sync_lyrics_video_desc
import com.mochyy.mochymusic.shared.resources.settings_sync_lyrics_video_detail
import com.mochyy.mochymusic.shared.resources.settings_youtube_translations
import com.mochyy.mochymusic.shared.resources.settings_youtube_translations_desc
import com.mochyy.mochymusic.shared.resources.settings_youtube_translations_detail
import com.mochyy.mochymusic.shared.resources.slow_motion_video
import com.mochyy.mochymusic.shared.resources.sparks
import com.mochyy.mochymusic.shared.resources.speed
import com.mochyy.mochymusic.shared.resources.spotify_canvas
import com.mochyy.mochymusic.shared.resources.spotify_canvas_row_off
import com.mochyy.mochymusic.shared.resources.spotify_canvas_row_on
import com.mochyy.mochymusic.shared.resources.translate
import com.mochyy.mochymusic.shared.resources.tune
import com.mochyy.mochymusic.shared.resources.video_quality
import com.mochyy.mochymusic.shared.resources.video_quality_4k
import com.mochyy.mochymusic.shared.resources.video_quality_auto
import com.mochyy.mochymusic.shared.resources.video_quality_settings_desc
import com.mochyy.mochymusic.shared.resources.video_quality_size
import com.mochyy.mochymusic.shared.resources.wifi_proxy
import com.mochyy.mochymusic.ui.component.SettingsHighlight
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.SubSettingsSearchBar
import com.mochyy.mochymusic.utils.rememberEnumPreference
import com.mochyy.mochymusic.utils.rememberPreference
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

const val NowPlayingSettingsRoute = "now_playing_settings"

// what customize answers to when a search or the related row asks for its top bar card
const val TopBarHighlightKey = "Top Bar on player"

// the same for its bottom bar card
const val BottomBarHighlightKey = "Bottom Bar on player"

// settings > now playing: what the open player shows around the song, its badge, lyrics, videos and canvas
//
// videoQualitySheet, canvasOrderSelect and floatingLyrics are the pieces that stay in :app: the video size
// sheet is the app's popup engine, the canvas order row its LabSelect, and the floating lyrics switch needs
// android's draw-over-other-apps permission. Each is passed in, and a platform that can't draw one leaves
// that row out rather than showing a control that does nothing. softLightSupported is android 10's blend
// layer, read by the app because the version check sits beside the lyrics renderer it belongs to.
@Composable
fun NowPlayingSettingsScreen(
    navController: NavController,
    videoQualitySheet: ((VideoQuality, (VideoQuality) -> Unit) -> Unit)? = null,
    canvasOrderSelect: (@Composable (options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, contentDescription: String) -> Unit)? = null,
    floatingLyrics: Pair<Boolean, (Boolean) -> Unit>? = null,
    softLightSupported: Boolean = false,
) {
    var query by remember { mutableStateOf("") }

    SubSettingsScaffold(
        title = stringResource(Res.string.settings_now_playing),
        onBack = { navController.navigateUp() },
        searchBar = { SubSettingsSearchBar(query = query, onQueryChange = { query = it }) },
    ) {
        SettingsSection(stringResource(Res.string.settings_on_the_player), badgeItems().matching(query))
        SettingsSection(
            stringResource(Res.string.settings_section_lyrics),
            lyricsItems(navController, floatingLyrics, softLightSupported).matching(query),
        )
        SettingsSection(stringResource(Res.string.settings_section_videos), videoItems(videoQualitySheet).matching(query))
        SettingsSection(stringResource(Res.string.settings_section_canvas), canvasItems(navController, canvasOrderSelect).matching(query))
        SettingsSection(stringResource(Res.string.settings_related), relatedItems(navController).matching(query))
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun badgeItems(): List<SettingsItem> {
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, defaultValue = false)
    val (formatBadge, onFormatBadgeChange) = rememberPreference(ShowFormatBadgeKey, defaultValue = true)
    val (formatSampleRate, onFormatSampleRateChange) = rememberPreference(ShowFormatSampleRateKey, defaultValue = false)
    val (formatBitDepth, onFormatBitDepthChange) = rememberPreference(ShowFormatBitDepthKey, defaultValue = false)
    return listOf(
        switchItem(
            key = "Hide player thumbnail",
            title = stringResource(Res.string.settings_hide_player_thumbnail),
            description = stringResource(Res.string.settings_hide_player_thumbnail_desc),
            icon = painterResource(Res.drawable.hide_image),
            checked = hidePlayerThumbnail,
            onCheckedChange = onHidePlayerThumbnailChange,
        ),
        switchItem(
            key = "Audio format badge",
            title = stringResource(Res.string.appearance_format_badge),
            description = stringResource(Res.string.appearance_format_badge_desc),
            icon = painterResource(Res.drawable.graphic_eq),
            checked = formatBadge,
            onCheckedChange = onFormatBadgeChange,
        ),
        switchItem(
            key = "Show sample rate",
            title = stringResource(Res.string.appearance_sample_rate),
            description = stringResource(Res.string.appearance_sample_rate_desc),
            icon = painterResource(Res.drawable.speed),
            checked = formatSampleRate,
            onCheckedChange = onFormatSampleRateChange,
            subOptionOf = formatBadge,
        ),
        switchItem(
            key = "Show bit depth",
            title = stringResource(Res.string.appearance_bit_depth),
            description = stringResource(Res.string.appearance_bit_depth_desc),
            icon = painterResource(Res.drawable.graphic_eq),
            checked = formatBitDepth,
            onCheckedChange = onFormatBitDepthChange,
            subOptionOf = formatBadge,
        ),
    )
}

@Composable
private fun lyricsItems(
    navController: NavController,
    floatingLyrics: Pair<Boolean, (Boolean) -> Unit>?,
    softLightSupported: Boolean,
): List<SettingsItem> {
    val (youtubeTranslations, onYoutubeTranslationsChange) = rememberPreference(YouTubeCaptionTranslationKey, defaultValue = true)
    val (syncLyricsToVideo, onSyncLyricsToVideoChange) = rememberPreference(SyncLyricsToVideoKey, defaultValue = true)
    val (romanizeAsMain, onRomanizeAsMainChange) = rememberPreference(LyricsRomanizeAsMainKey, defaultValue = false)
    val (tapToSeek, onTapToSeekChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (autoScroll, onAutoScrollChange) = rememberPreference(LyricsScrollKey, defaultValue = true)
    val (highBloom, onHighBloomChange) = rememberPreference(LyricsHighBloomKey, defaultValue = true)
    val (interludeDots, onInterludeDotsChange) = rememberPreference(LyricsInterludeDotsKey, defaultValue = true)
    val (lineBounce, onLineBounceChange) = rememberPreference(LyricsLineBounceKey, defaultValue = true)
    val (softLight, onSoftLightChange) = rememberPreference(LyricsSoftLightKey, defaultValue = true)
    val (lyricsStyle) = rememberEnumPreference(LyricsMotionStyleKey, defaultValue = LyricsMotionStyle.MOTION)
    return listOf(
        SettingsItem(
            title = { Text(stringResource(Res.string.settings_ai_translator)) },
            descriptionText = stringResource(Res.string.settings_ai_translator_desc),
            icon = painterResource(Res.drawable.translate),
            onClick = { navController.navigate(AiTranslatorSettingsRoute) },
        ),
        switchItem(
            key = "Translations from YouTube",
            title = stringResource(Res.string.settings_youtube_translations),
            description = stringResource(Res.string.settings_youtube_translations_desc),
            detail = stringResource(Res.string.settings_youtube_translations_detail),
            icon = painterResource(Res.drawable.translate),
            checked = youtubeTranslations,
            onCheckedChange = onYoutubeTranslationsChange,
        ),
        switchItem(
            key = "Sync lyrics to music videos",
            title = stringResource(Res.string.settings_sync_lyrics_video),
            description = stringResource(Res.string.settings_sync_lyrics_video_desc),
            detail = stringResource(Res.string.settings_sync_lyrics_video_detail),
            icon = painterResource(Res.drawable.lyrics),
            checked = syncLyricsToVideo,
            onCheckedChange = onSyncLyricsToVideoChange,
        ),
        switchItem(
            key = "Romanisation as the main line",
            title = stringResource(Res.string.settings_romanize_as_main),
            description = stringResource(Res.string.settings_romanize_as_main_desc),
            icon = painterResource(Res.drawable.lyrics),
            checked = romanizeAsMain,
            onCheckedChange = onRomanizeAsMainChange,
        ),
        *romanizeLanguageItems().toTypedArray(),
        switchItem(
            key = "Change lyrics on click",
            title = stringResource(Res.string.lyrics_click_change),
            description = stringResource(Res.string.lyrics_click_change_desc),
            icon = painterResource(Res.drawable.lyrics),
            checked = tapToSeek,
            onCheckedChange = onTapToSeekChange,
        ),
        switchItem(
            key = "Auto scroll lyrics",
            title = stringResource(Res.string.lyrics_auto_scroll),
            description = stringResource(Res.string.lyrics_auto_scroll_desc),
            icon = painterResource(Res.drawable.lyrics),
            checked = autoScroll,
            onCheckedChange = onAutoScrollChange,
        ),
        switchItem(
            key = "Show instrumental dots",
            title = stringResource(Res.string.settings_lyrics_interlude_dots),
            description = stringResource(Res.string.settings_lyrics_interlude_dots_desc),
            icon = painterResource(Res.drawable.lyrics),
            checked = interludeDots,
            onCheckedChange = onInterludeDotsChange,
        ),
        switchItem(
            key = "Animate line-synced lyrics",
            title = stringResource(Res.string.settings_lyrics_line_bounce),
            description = stringResource(Res.string.settings_lyrics_line_bounce_desc),
            icon = painterResource(Res.drawable.lyrics),
            checked = lineBounce,
            onCheckedChange = onLineBounceChange,
            visible = lyricsStyle.risesWords,
        ),
        // only the shaded lyric styles have a bloom to make softer
        switchItem(
            key = "High quality bloom",
            title = stringResource(Res.string.appearance_bloom),
            description = stringResource(Res.string.appearance_bloom_desc),
            icon = painterResource(Res.drawable.sparks),
            checked = highBloom,
            onCheckedChange = onHighBloomChange,
            visible = lyricsStyle.shaded,
        ),
        // the blend needs android 10, older phones never see it
        switchItem(
            key = "Tinted lyrics",
            title = stringResource(Res.string.appearance_lyrics_soft_light),
            description = stringResource(Res.string.appearance_lyrics_soft_light_desc),
            icon = painterResource(Res.drawable.palette),
            checked = softLight,
            onCheckedChange = onSoftLightChange,
            visible = softLightSupported,
        ),
        switchItem(
            key = "Floating lyrics overlay bubble",
            title = stringResource(Res.string.settings_floating_lyrics),
            description = stringResource(Res.string.settings_floating_lyrics_desc),
            detail = stringResource(Res.string.settings_floating_lyrics_detail),
            icon = painterResource(Res.drawable.lyrics),
            checked = floatingLyrics?.first == true,
            onCheckedChange = { on -> floatingLyrics?.second?.invoke(on) },
            visible = floatingLyrics != null,
        ),
    )
}

@Composable
private fun videoItems(videoQualitySheet: ((VideoQuality, (VideoQuality) -> Unit) -> Unit)?): List<SettingsItem> {
    val (playVideoBackground, onPlayVideoBackgroundChange) = rememberPreference(PlayVideoBackgroundKey, defaultValue = false)
    val (lyricsSync, onLyricsSyncChange) = rememberPreference(YtVideoBackgroundLyricsSyncKey, defaultValue = true)
    val (disableVideoAutoplay, onDisableVideoAutoplayChange) = rememberPreference(DisableVideoAutoplayKey, defaultValue = false)
    val (studioForVideos, onStudioForVideosChange) = rememberPreference(StudioForVideosKey, defaultValue = false)
    return listOf(
        switchItem(
            key = "Always enable Youtube background",
            title = stringResource(Res.string.appearance_youtube_background),
            description = stringResource(Res.string.appearance_youtube_background_desc),
            icon = painterResource(Res.drawable.slow_motion_video),
            checked = playVideoBackground,
            onCheckedChange = onPlayVideoBackgroundChange,
        ),
        // right under the background switch it belongs to, a sub row only joins the row just above it
        switchItem(
            key = "Timestamp matching",
            title = stringResource(Res.string.appearance_timestamp_matching),
            description = stringResource(Res.string.appearance_timestamp_matching_desc),
            icon = painterResource(Res.drawable.lyrics),
            checked = lyricsSync,
            onCheckedChange = onLyricsSyncChange,
            subOptionOf = playVideoBackground,
        ),
        videoQualityItem(videoQualitySheet),
        switchItem(
            key = "Disable auto-playing YouTube video",
            title = stringResource(Res.string.settings_disable_video_autoplay),
            description = stringResource(Res.string.settings_disable_video_autoplay_desc),
            icon = painterResource(Res.drawable.slow_motion_video),
            checked = disableVideoAutoplay,
            onCheckedChange = onDisableVideoAutoplayChange,
        ),
        switchItem(
            key = "Studio version for music videos",
            title = stringResource(Res.string.settings_studio_for_videos),
            description = stringResource(Res.string.settings_studio_for_videos_desc),
            icon = painterResource(Res.drawable.music_note),
            checked = studioForVideos,
            onCheckedChange = onStudioForVideosChange,
        ),
    )
}

// how sharp youtube videos play, on the cover and in the full video mode
@Composable
private fun videoQualityItem(videoQualitySheet: ((VideoQuality, (VideoQuality) -> Unit) -> Unit)?): SettingsItem {
    val (quality, onQuality) = rememberEnumPreference(VideoQualityKey, defaultValue = VideoQuality.AUTO)
    val title = stringResource(Res.string.video_quality)
    return SettingsItem(
        title = { Text(title) },
        highlightKey = "Video quality",
        descriptionText = videoQualityName(quality),
        detail = stringResource(Res.string.video_quality_settings_desc),
        searchText = title,
        // Icons.Rounded.HighQuality, kept as the exact Material vector (the shared module has no icons-extended)
        icon = painterResource(Res.drawable.material_rounded_high_quality),
        iconShape = CircleShape,
        isVisible = videoQualitySheet != null,
        onClick = { videoQualitySheet?.invoke(quality, onQuality) },
    )
}

@Composable
private fun canvasItems(
    navController: NavController,
    canvasOrderSelect: (@Composable (options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, contentDescription: String) -> Unit)?,
): List<SettingsItem> {
    val (canvasEnabled, onCanvasEnabledChange) = rememberPreference(CanvasThumbnailAnimationKey, defaultValue = true)
    val (canvasWifiOnly, onCanvasWifiOnlyChange) = rememberPreference(CanvasWifiOnlyKey, defaultValue = true)
    val (canvasBest, onCanvasBestChange) = rememberPreference(CanvasBestQualityKey, defaultValue = true)
    val (canvasOrder, onCanvasOrderChange) = rememberEnumPreference(CanvasOrderKey, defaultValue = CanvasOrder.APPLE_TIDAL_SPOTIFY)
    val (spotifyCanvas) = rememberPreference(SpotifyCanvasKey, defaultValue = false)
    val canvasSourceNames = mapOf(
        CanvasSource.APPLE_MUSIC to stringResource(Res.string.canvas_source_apple),
        CanvasSource.TIDAL to stringResource(Res.string.canvas_source_tidal),
        CanvasSource.SPOTIFY to stringResource(Res.string.canvas_source_spotify),
    )
    return listOf(
        switchItem(
            key = "Enable animated canvas",
            title = stringResource(Res.string.appearance_canvas),
            description = stringResource(Res.string.appearance_canvas_desc),
            icon = painterResource(Res.drawable.sparks),
            checked = canvasEnabled,
            onCheckedChange = onCanvasEnabledChange,
        ),
        SettingsItem(
            title = { Text(stringResource(Res.string.canvas_order)) },
            highlightKey = "Canvas order",
            searchText = stringResource(Res.string.canvas_order),
            description = {
                if (canvasOrderSelect != null) {
                    canvasOrderSelect(
                        CanvasOrder.entries.map { order -> order.sources.joinToString(" → ") { canvasSourceNames.getValue(it) } },
                        canvasOrder.ordinal,
                        { onCanvasOrderChange(CanvasOrder.entries[it]) },
                        stringResource(Res.string.canvas_order),
                    )
                }
            },
            icon = painterResource(Res.drawable.sparks),
            iconShape = CircleShape,
            isVisible = canvasEnabled && canvasOrderSelect != null,
            isSubOption = true,
        ),
        linkItem(
            key = "Spotify canvas",
            title = stringResource(Res.string.spotify_canvas),
            description = stringResource(if (spotifyCanvas) Res.string.spotify_canvas_row_on else Res.string.spotify_canvas_row_off),
            icon = painterResource(Res.drawable.sparks),
            // the spotify canvas page's own route, as :app registers it (SpotifyRoute there)
            onClick = { navController.navigate("integrations_settings/spotify") },
            visible = canvasEnabled,
            isSubOption = true,
        ),
        switchItem(
            key = "Canvas on Wi-Fi only",
            title = stringResource(Res.string.appearance_canvas_wifi),
            description = stringResource(Res.string.appearance_canvas_wifi_desc),
            icon = painterResource(Res.drawable.wifi_proxy),
            checked = canvasWifiOnly,
            onCheckedChange = onCanvasWifiOnlyChange,
            subOptionOf = canvasEnabled,
        ),
        switchItem(
            key = "Sharpest animated covers",
            title = stringResource(Res.string.canvas_best_quality),
            description = stringResource(Res.string.canvas_best_quality_desc),
            icon = painterResource(Res.drawable.material_rounded_high_quality),
            checked = canvasBest,
            onCheckedChange = onCanvasBestChange,
            subOptionOf = canvasEnabled,
        ),
    )
}

// how the player looks lives in customize, these take people to the right card there
@Composable
private fun relatedItems(navController: NavController): List<SettingsItem> {
    val openCustomise = { key: String ->
        SettingsHighlight.pending = key
        navController.navigate("customisation_settings")
    }
    return listOf(
        linkItem(
            key = "Lyrics animation style",
            title = stringResource(Res.string.appearance_lyrics_animation),
            description = stringResource(Res.string.settings_lyrics_animation_link_desc),
            icon = painterResource(Res.drawable.lyrics),
            onClick = { openCustomise("Lyrics animation") },
        ),
        linkItem(
            key = "Hide Top Bar status bar player",
            title = stringResource(Res.string.settings_hide_top_bar),
            description = stringResource(Res.string.settings_hide_top_bar_desc),
            icon = painterResource(Res.drawable.tune),
            onClick = { openCustomise(TopBarHighlightKey) },
        ),
        linkItem(
            key = "Hide Bottom Bar navigation bar player",
            title = stringResource(Res.string.settings_hide_bottom_bar),
            description = stringResource(Res.string.settings_hide_bottom_bar_desc),
            icon = painterResource(Res.drawable.tune),
            onClick = { openCustomise(BottomBarHighlightKey) },
        ),
    )
}

// one switch per language the readings can romanise, under the switch above; the customise page's master switch sets them all
private val romanizeLanguages = listOf(
    Triple("Romanize Japanese lyrics", Res.string.lyrics_romanize_japanese, LyricsRomanizeJapaneseKey),
    Triple("Romanize Korean lyrics", Res.string.lyrics_romanize_korean, LyricsRomanizeKoreanKey),
    Triple("Romanize Chinese lyrics", Res.string.lyrics_romanize_chinese, LyricsRomanizeChineseKey),
    Triple("Romanize Russian lyrics", Res.string.lyrics_romanize_russian, LyricsRomanizeRussianKey),
    Triple("Romanize Ukrainian lyrics", Res.string.lyrics_romanize_ukrainian, LyricsRomanizeUkrainianKey),
    Triple("Romanize Serbian lyrics", Res.string.lyrics_romanize_serbian, LyricsRomanizeSerbianKey),
    Triple("Romanize Bulgarian lyrics", Res.string.lyrics_romanize_bulgarian, LyricsRomanizeBulgarianKey),
    Triple("Romanize Belarusian lyrics", Res.string.lyrics_romanize_belarusian, LyricsRomanizeBelarusianKey),
    Triple("Romanize Kyrgyz lyrics", Res.string.lyrics_romanize_kyrgyz, LyricsRomanizeKyrgyzKey),
    Triple("Romanize Macedonian lyrics", Res.string.lyrics_romanize_macedonian, LyricsRomanizeMacedonianKey),
    Triple("Romanize Hindi lyrics", Res.string.lyrics_romanize_hindi, LyricsRomanizeHindiKey),
    Triple("Romanize Punjabi lyrics", Res.string.lyrics_romanize_punjabi, LyricsRomanizePunjabiKey),
)

@Composable
private fun romanizeLanguageItems(): List<SettingsItem> {
    val description = stringResource(Res.string.settings_romanize_language_desc)
    return romanizeLanguages.map { (name, titleRes, key) ->
        val state = rememberPreference(key, defaultValue = true)
        switchItem(
            key = name,
            title = stringResource(titleRes),
            description = description,
            icon = painterResource(Res.drawable.lyrics),
            checked = state.value,
            onCheckedChange = { state.value = it },
        )
    }
}
