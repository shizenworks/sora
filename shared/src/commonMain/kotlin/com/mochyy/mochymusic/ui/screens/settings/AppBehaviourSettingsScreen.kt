package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.AlbumsFromLikedSongsKey
import com.mochyy.mochymusic.constants.DataSaverKey
import com.mochyy.mochymusic.constants.DataSaverMobileOnlyKey
import com.mochyy.mochymusic.constants.DisableScreenshotKey
import com.mochyy.mochymusic.constants.HideExplicitKey
import com.mochyy.mochymusic.constants.HideSettingsDescriptionsKey
import com.mochyy.mochymusic.constants.HideUpdateNoticeKey
import com.mochyy.mochymusic.constants.HideVideoSongsKey
import com.mochyy.mochymusic.constants.LocalSongAutoMetadataKey
import com.mochyy.mochymusic.constants.OfflineModeKey
import com.mochyy.mochymusic.constants.PauseListenHistoryKey
import com.mochyy.mochymusic.constants.PauseSearchHistoryKey
import com.mochyy.mochymusic.constants.ProxyEnabledKey
import com.mochyy.mochymusic.constants.ProxyUrlKey
import com.mochyy.mochymusic.constants.ShowNavTabLabelsKey
import com.mochyy.mochymusic.constants.YtReportPlaysKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.album
import com.mochyy.mochymusic.shared.resources.cached
import com.mochyy.mochymusic.shared.resources.data_saver_on
import com.mochyy.mochymusic.shared.resources.disable_screenshot
import com.mochyy.mochymusic.shared.resources.disable_screenshot_desc
import com.mochyy.mochymusic.shared.resources.download
import com.mochyy.mochymusic.shared.resources.explicit
import com.mochyy.mochymusic.shared.resources.general_app_settings
import com.mochyy.mochymusic.shared.resources.hide_explicit
import com.mochyy.mochymusic.shared.resources.history
import com.mochyy.mochymusic.shared.resources.info
import com.mochyy.mochymusic.shared.resources.lyrics
import com.mochyy.mochymusic.shared.resources.pause_listen_history
import com.mochyy.mochymusic.shared.resources.pause_search_history
import com.mochyy.mochymusic.shared.resources.play
import com.mochyy.mochymusic.shared.resources.proxy
import com.mochyy.mochymusic.shared.resources.proxy_status_off
import com.mochyy.mochymusic.shared.resources.proxy_status_on
import com.mochyy.mochymusic.shared.resources.screenshot
import com.mochyy.mochymusic.shared.resources.settings_albums_from_likes
import com.mochyy.mochymusic.shared.resources.settings_albums_from_likes_desc
import com.mochyy.mochymusic.shared.resources.settings_albums_from_likes_detail
import com.mochyy.mochymusic.shared.resources.settings_app_behaviour
import com.mochyy.mochymusic.shared.resources.settings_data_saver
import com.mochyy.mochymusic.shared.resources.settings_data_saver_desc
import com.mochyy.mochymusic.shared.resources.settings_data_saver_detail
import com.mochyy.mochymusic.shared.resources.settings_data_saver_mobile
import com.mochyy.mochymusic.shared.resources.settings_data_saver_mobile_desc
import com.mochyy.mochymusic.shared.resources.settings_hide_descriptions
import com.mochyy.mochymusic.shared.resources.settings_hide_descriptions_desc
import com.mochyy.mochymusic.shared.resources.settings_hide_explicit_desc
import com.mochyy.mochymusic.shared.resources.settings_hide_music_videos
import com.mochyy.mochymusic.shared.resources.settings_hide_music_videos_desc
import com.mochyy.mochymusic.shared.resources.settings_local_auto_metadata
import com.mochyy.mochymusic.shared.resources.settings_local_auto_metadata_desc
import com.mochyy.mochymusic.shared.resources.settings_navigation_tab_labels
import com.mochyy.mochymusic.shared.resources.settings_navigation_tab_labels_desc
import com.mochyy.mochymusic.shared.resources.settings_now_playing
import com.mochyy.mochymusic.shared.resources.settings_now_playing_link_desc
import com.mochyy.mochymusic.shared.resources.settings_offline_mode
import com.mochyy.mochymusic.shared.resources.settings_offline_mode_desc
import com.mochyy.mochymusic.shared.resources.settings_pause_listen_history_desc
import com.mochyy.mochymusic.shared.resources.settings_pause_search_history_desc
import com.mochyy.mochymusic.shared.resources.settings_playback_category
import com.mochyy.mochymusic.shared.resources.settings_playback_link_desc
import com.mochyy.mochymusic.shared.resources.settings_related
import com.mochyy.mochymusic.shared.resources.settings_section_content
import com.mochyy.mochymusic.shared.resources.settings_section_library_privacy
import com.mochyy.mochymusic.shared.resources.settings_section_lyrics
import com.mochyy.mochymusic.shared.resources.settings_section_network
import com.mochyy.mochymusic.shared.resources.settings_updates_notice
import com.mochyy.mochymusic.shared.resources.settings_updates_notice_desc
import com.mochyy.mochymusic.shared.resources.settings_youtube_plays
import com.mochyy.mochymusic.shared.resources.settings_youtube_plays_desc
import com.mochyy.mochymusic.shared.resources.settings_youtube_plays_detail
import com.mochyy.mochymusic.shared.resources.signal_cellular_alt
import com.mochyy.mochymusic.shared.resources.slow_motion_video
import com.mochyy.mochymusic.shared.resources.trending_up
import com.mochyy.mochymusic.shared.resources.tune
import com.mochyy.mochymusic.shared.resources.update
import com.mochyy.mochymusic.shared.resources.wifi_proxy
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.SubSettingsSearchBar
import com.mochyy.mochymusic.utils.parseProxyAddress
import com.mochyy.mochymusic.utils.rememberPreference
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The rows the Android app builds itself because they lean on pieces only it has: the larp, app-language
 * and lyrics-order sheets, the lyrics provider registry, and the compact build's language pack. A host
 * that passes none of them gets the page without those rows.
 */
class AppBehaviourRows(
    val larp: (@Composable () -> SettingsItem)? = null,
    val language: (@Composable () -> SettingsItem)? = null,
    val lyricsOrder: (@Composable () -> SettingsItem)? = null,
    val japaneseLanguagePack: (@Composable () -> SettingsItem)? = null,
)

// settings > app behaviour: the app itself, its network and your library; playback and the player's looks have their own pages now
@Composable
fun AppBehaviourSettingsScreen(
    navController: NavController,
    rows: AppBehaviourRows = AppBehaviourRows(),
    // the launch check's notification tracks its own switch in :app; the page calls this after saving the preference
    onUpdateNoticeChanged: (Boolean) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }

    SubSettingsScaffold(
        title = stringResource(Res.string.settings_app_behaviour),
        onBack = { navController.navigateUp() },
        searchBar = { SubSettingsSearchBar(query = query, onQueryChange = { query = it }) },
    ) {
        // larpaspriority
        SettingsSection(title = null, items = larpItems(rows).matching(query))
        SettingsSection(stringResource(Res.string.general_app_settings), generalItems(rows.language, onUpdateNoticeChanged).matching(query))
        SettingsSection(stringResource(Res.string.settings_section_network), networkItems(navController).matching(query))
        SettingsSection(stringResource(Res.string.settings_section_content), contentItems().matching(query))
        SettingsSection(stringResource(Res.string.settings_section_library_privacy), libraryItems().matching(query))
        SettingsSection(stringResource(Res.string.settings_section_lyrics), lyricsItems(rows).matching(query))
        SettingsSection(stringResource(Res.string.settings_related), relatedItems(navController).matching(query))
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun larpItems(rows: AppBehaviourRows): List<SettingsItem> = listOfNotNull(rows.larp?.invoke())

@Composable
private fun generalItems(
    languageRow: (@Composable () -> SettingsItem)?,
    onUpdateNoticeChanged: (Boolean) -> Unit,
): List<SettingsItem> {
    val (noticeHidden, onNoticeHidden) = rememberPreference(HideUpdateNoticeKey, defaultValue = false)
    val (hideDescriptions, onHideDescriptions) = rememberPreference(HideSettingsDescriptionsKey, defaultValue = true)
    val (navLabels, onNavLabelsChange) = rememberPreference(ShowNavTabLabelsKey, defaultValue = false)
    // the launch check's notification reads its own switch, so it follows this one too
    val setNoticeHidden = { hidden: Boolean ->
        onNoticeHidden(hidden)
        onUpdateNoticeChanged(hidden)
    }
    return buildList {
        languageRow?.let { add(it()) }
        add(
            switchItem(
                key = "Navigation tab labels",
                title = stringResource(Res.string.settings_navigation_tab_labels),
                description = stringResource(Res.string.settings_navigation_tab_labels_desc),
                icon = painterResource(Res.drawable.tune),
                checked = navLabels,
                onCheckedChange = onNavLabelsChange,
            ),
        )
        add(
            switchItem(
                key = "Disable App Updates notice",
                title = stringResource(Res.string.settings_updates_notice),
                description = stringResource(Res.string.settings_updates_notice_desc),
                icon = painterResource(Res.drawable.update),
                checked = noticeHidden,
                onCheckedChange = setNoticeHidden,
            ),
        )
        add(
            switchItem(
                key = "Disable description on settings",
                title = stringResource(Res.string.settings_hide_descriptions),
                description = stringResource(Res.string.settings_hide_descriptions_desc),
                icon = painterResource(Res.drawable.info),
                checked = hideDescriptions,
                onCheckedChange = onHideDescriptions,
            ),
        )
    }
}

@Composable
private fun networkItems(navController: NavController): List<SettingsItem> {
    val (offlineMode, onOfflineModeChange) = rememberPreference(OfflineModeKey, defaultValue = false)
    val (dataSaver, onDataSaverChange) = rememberPreference(DataSaverKey, defaultValue = false)
    val (dataSaverMobileOnly, onDataSaverMobileOnlyChange) = rememberPreference(DataSaverMobileOnlyKey, defaultValue = true)
    val (proxyOn) = rememberPreference(ProxyEnabledKey, defaultValue = false)
    val (proxyAddress) = rememberPreference(ProxyUrlKey, defaultValue = "")
    return listOf(
        switchItem(
            key = "Offline mode",
            title = stringResource(Res.string.settings_offline_mode),
            description = stringResource(Res.string.settings_offline_mode_desc),
            icon = painterResource(Res.drawable.download),
            checked = offlineMode,
            onCheckedChange = onOfflineModeChange,
        ),
        // lighter streams and still covers, for metered plans; the second switch decides whether wi-fi counts too
        switchItem(
            key = "Data saver",
            title = stringResource(Res.string.settings_data_saver),
            description = stringResource(Res.string.settings_data_saver_desc),
            detail = stringResource(Res.string.settings_data_saver_detail),
            icon = painterResource(Res.drawable.data_saver_on),
            checked = dataSaver,
            onCheckedChange = onDataSaverChange,
        ),
        switchItem(
            key = "Data saver only on mobile data",
            title = stringResource(Res.string.settings_data_saver_mobile),
            description = stringResource(Res.string.settings_data_saver_mobile_desc),
            icon = painterResource(Res.drawable.signal_cellular_alt),
            checked = dataSaverMobileOnly,
            onCheckedChange = onDataSaverMobileOnlyChange,
            subOptionOf = dataSaver,
        ),
        linkItem(
            key = "Proxy",
            title = stringResource(Res.string.proxy),
            description = if (proxyOn && parseProxyAddress(proxyAddress) != null) {
                stringResource(Res.string.proxy_status_on, proxyAddress)
            } else {
                stringResource(Res.string.proxy_status_off)
            },
            icon = painterResource(Res.drawable.wifi_proxy),
            onClick = { navController.navigate(ProxySettingsRoute) },
        ),
    )
}

// these switches only hide songs in the lists; the filters themselves are the ones that already run on home, search and radio
@Composable
private fun contentItems(): List<SettingsItem> {
    val (hideExplicit, onHideExplicitChange) = rememberPreference(HideExplicitKey, defaultValue = false)
    val (hideVideos, onHideVideosChange) = rememberPreference(HideVideoSongsKey, defaultValue = false)
    return listOf(
        switchItem(
            key = "Hide explicit content",
            title = stringResource(Res.string.hide_explicit),
            description = stringResource(Res.string.settings_hide_explicit_desc),
            icon = painterResource(Res.drawable.explicit),
            checked = hideExplicit,
            onCheckedChange = onHideExplicitChange,
        ),
        switchItem(
            key = "Hide music videos",
            title = stringResource(Res.string.settings_hide_music_videos),
            description = stringResource(Res.string.settings_hide_music_videos_desc),
            icon = painterResource(Res.drawable.slow_motion_video),
            checked = hideVideos,
            onCheckedChange = onHideVideosChange,
        ),
    )
}

@Composable
private fun libraryItems(): List<SettingsItem> {
    val (pauseListenHistory, onPauseListenHistoryChange) = rememberPreference(PauseListenHistoryKey, defaultValue = false)
    val (disableScreenshot, onDisableScreenshotChange) = rememberPreference(DisableScreenshotKey, defaultValue = false)
    val (albumsFromLikes, onAlbumsFromLikesChange) = rememberPreference(AlbumsFromLikedSongsKey, defaultValue = false)
    val (autoMetadata, onAutoMetadataChange) = rememberPreference(LocalSongAutoMetadataKey, defaultValue = true)
    val (pauseSearchHistory, onPauseSearchHistoryChange) = rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    val (reportPlays, onReportPlaysChange) = rememberPreference(YtReportPlaysKey, defaultValue = true)
    return listOf(
        switchItem(
            key = "Albums from liked songs",
            title = stringResource(Res.string.settings_albums_from_likes),
            description = stringResource(Res.string.settings_albums_from_likes_desc),
            detail = stringResource(Res.string.settings_albums_from_likes_detail),
            icon = painterResource(Res.drawable.album),
            checked = albumsFromLikes,
            onCheckedChange = onAlbumsFromLikesChange,
        ),
        switchItem(
            key = "Local song auto metadata",
            title = stringResource(Res.string.settings_local_auto_metadata),
            description = stringResource(Res.string.settings_local_auto_metadata_desc),
            icon = painterResource(Res.drawable.cached),
            checked = autoMetadata,
            onCheckedChange = onAutoMetadataChange,
        ),
        // stops new listens from being saved here and sent to YouTube history; the listens already saved stay
        switchItem(
            key = "Pause listen history",
            title = stringResource(Res.string.pause_listen_history),
            description = stringResource(Res.string.settings_pause_listen_history_desc),
            icon = painterResource(Res.drawable.history),
            checked = pauseListenHistory,
            onCheckedChange = onPauseListenHistoryChange,
        ),
        // blocks screenshots and hides the app in Recents; applies right away
        switchItem(
            key = "Disable screenshot",
            title = stringResource(Res.string.disable_screenshot),
            description = stringResource(Res.string.disable_screenshot_desc),
            icon = painterResource(Res.drawable.screenshot),
            checked = disableScreenshot,
            onCheckedChange = onDisableScreenshotChange,
        ),
        switchItem(
            key = "Pause search history",
            title = stringResource(Res.string.pause_search_history),
            description = stringResource(Res.string.settings_pause_search_history_desc),
            icon = painterResource(Res.drawable.history),
            checked = pauseSearchHistory,
            onCheckedChange = onPauseSearchHistoryChange,
        ),
        // the same switch as sync's youtube history one, signed out it still counts the play
        switchItem(
            key = "Count plays on YouTube views",
            title = stringResource(Res.string.settings_youtube_plays),
            description = stringResource(Res.string.settings_youtube_plays_desc),
            detail = stringResource(Res.string.settings_youtube_plays_detail),
            icon = painterResource(Res.drawable.trending_up),
            checked = reportPlays,
            onCheckedChange = onReportPlaysChange,
        ),
    )
}

// the row leans on :app's lyrics registry, so the host builds it; the compact build's language pack sits under it
@Composable
private fun lyricsItems(rows: AppBehaviourRows): List<SettingsItem> {
    val orderSetting = rows.lyricsOrder?.invoke() ?: return emptyList()
    return listOfNotNull(orderSetting, rows.japaneseLanguagePack?.invoke())
}

// most of what used to sit here moved to its own page, these keep it a tap away for anyone who still looks here
@Composable
private fun relatedItems(navController: NavController): List<SettingsItem> = listOf(
    linkItem(
        key = "Playback page",
        title = stringResource(Res.string.settings_playback_category),
        description = stringResource(Res.string.settings_playback_link_desc),
        icon = painterResource(Res.drawable.play),
        onClick = { navController.navigate(PlaybackSettingsRoute) },
    ),
    linkItem(
        key = "Now playing page",
        title = stringResource(Res.string.settings_now_playing),
        description = stringResource(Res.string.settings_now_playing_link_desc),
        icon = painterResource(Res.drawable.lyrics),
        onClick = { navController.navigate("now_playing_settings") },
    ),
)
