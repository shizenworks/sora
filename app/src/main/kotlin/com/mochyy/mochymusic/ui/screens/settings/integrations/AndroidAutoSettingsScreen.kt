package com.mochyy.mochymusic.ui.screens.settings.integrations

import androidx.car.app.connection.CarConnection
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mochyy.mochymusic.LocalDatabase
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.CarDeviceSongsKey
import com.mochyy.mochymusic.constants.CarLyricsKey
import com.mochyy.mochymusic.constants.CarMeteredStreamingKey
import com.mochyy.mochymusic.constants.CarPinnedPlaylistIdsKey
import com.mochyy.mochymusic.constants.CarMeteredArtworkKey
import com.mochyy.mochymusic.constants.CarOnlineRecommendationsKey
import com.mochyy.mochymusic.constants.CarOnlineVoiceSearchKey
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.screens.settings.SettingsSection
import com.mochyy.mochymusic.ui.screens.settings.switchItem
import com.mochyy.mochymusic.utils.rememberPreference

const val AndroidAutoSettingsRoute = "integrations_settings/android_auto"

@Composable
fun AndroidAutoSettingsScreen(navController: NavController) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val connection = remember(context) { CarConnection(context).type }
    var connectionType by remember { mutableStateOf(CarConnection.CONNECTION_TYPE_NOT_CONNECTED) }
    DisposableEffect(connection, lifecycleOwner) {
        val observer = Observer<Int> { type -> connectionType = type }
        connection.observe(lifecycleOwner, observer)
        onDispose { connection.removeObserver(observer) }
    }
    val status = stringResource(when (connectionType) {
        CarConnection.CONNECTION_TYPE_PROJECTION -> R.string.car_settings_projected
        CarConnection.CONNECTION_TYPE_NATIVE -> R.string.car_settings_native
        else -> R.string.car_settings_disconnected
    })
    val (recommendations, setRecommendations) = rememberPreference(CarOnlineRecommendationsKey, defaultValue = true)
    val (onlineVoice, setOnlineVoice) = rememberPreference(CarOnlineVoiceSearchKey, defaultValue = true)
    val (meteredArtwork, setMeteredArtwork) = rememberPreference(CarMeteredArtworkKey, defaultValue = true)
    val (meteredStreaming, setMeteredStreaming) = rememberPreference(CarMeteredStreamingKey, defaultValue = true)
    val (lyrics, setLyrics) = rememberPreference(CarLyricsKey, defaultValue = true)
    val (pinnedRaw, setPinnedRaw) = rememberPreference(CarPinnedPlaylistIdsKey, defaultValue = "")
    val database = LocalDatabase.current
    val playlistsFlow = remember(database) { database.playlistsByCreateDateAsc() }
    val playlists by playlistsFlow.collectAsState(initial = emptyList())
    // ids in the order they were picked; a playlist deleted since then just drops out of the list
    val pinnedIds = pinnedRaw.split(",").filter { it.isNotBlank() }
    val pinned = pinnedIds.mapNotNull { id -> playlists.find { it.id == id } }
    val others = playlists.filter { playlist -> pinnedIds.none { it == playlist.id } }
    val ids = pinned.map { it.id }
    val savePinned = { list: List<String> -> setPinnedRaw(list.joinToString(",")) }
    val (deviceSongs, setDeviceSongs) = rememberPreference(CarDeviceSongsKey, defaultValue = true)

    SubSettingsScaffold(
        title = stringResource(R.string.car_settings_title),
        onBack = { navController.navigateUp() },
    ) {
        SettingsSection(stringResource(R.string.car_settings_connection), listOf(SettingsItem(title = { Text(status) })))
        SettingsSection(stringResource(R.string.car_settings_content), listOf(
            switchItem(
                key = "Online recommendations",
                title = stringResource(R.string.car_settings_recommendations),
                description = stringResource(R.string.car_settings_recommendations_desc),
                icon = painterResource(R.drawable.home),
                checked = recommendations,
                onCheckedChange = setRecommendations,
            ),
            switchItem(
                key = "Songs on this device",
                title = stringResource(R.string.car_settings_device_songs),
                description = stringResource(R.string.car_settings_device_songs_desc),
                icon = painterResource(R.drawable.smartphone),
                checked = deviceSongs,
                onCheckedChange = setDeviceSongs,
            ),
            switchItem(
                key = "Online voice search",
                title = stringResource(R.string.car_settings_voice),
                description = stringResource(R.string.car_settings_voice_desc),
                icon = painterResource(R.drawable.search_outline),
                checked = onlineVoice,
                onCheckedChange = setOnlineVoice,
            ),
            switchItem(
                key = "Lyrics on the car player",
                title = stringResource(R.string.car_settings_lyrics),
                description = stringResource(R.string.car_settings_lyrics_desc),
                icon = painterResource(R.drawable.lyrics),
                checked = lyrics,
                onCheckedChange = setLyrics,
            ),
        ))
        SettingsSection(stringResource(R.string.car_pinned_playlists), pinned.mapIndexed { index, playlist ->
            SettingsItem(
                title = { Text(playlist.playlist.name) },
                trailingContent = {
                    Row {
                        IconButton(enabled = index > 0, onClick = {
                            savePinned(ids.toMutableList().apply { add(index - 1, removeAt(index)) })
                        }) {
                            Icon(painterResource(R.drawable.arrow_upward), contentDescription = stringResource(R.string.car_pinned_move_up))
                        }
                        IconButton(enabled = index < ids.lastIndex, onClick = {
                            savePinned(ids.toMutableList().apply { add(index + 1, removeAt(index)) })
                        }) {
                            Icon(painterResource(R.drawable.arrow_downward), contentDescription = stringResource(R.string.car_pinned_move_down))
                        }
                        IconButton(onClick = { savePinned(ids - playlist.id) }) {
                            Icon(painterResource(R.drawable.close), contentDescription = stringResource(R.string.car_pinned_remove))
                        }
                    }
                },
            )
        })
        SettingsSection(stringResource(R.string.car_settings_pinned_other), others.map { playlist ->
            SettingsItem(
                title = { Text(playlist.playlist.name) },
                trailingContent = {
                    IconButton(onClick = { savePinned(ids + playlist.id) }) {
                        Icon(painterResource(R.drawable.playlist_add), contentDescription = stringResource(R.string.car_pinned_add))
                    }
                },
            )
        })
        SettingsSection(stringResource(R.string.car_settings_mobile_data), listOf(
            switchItem(
                key = "Artwork on metered networks",
                title = stringResource(R.string.car_settings_artwork),
                description = stringResource(R.string.car_settings_artwork_desc),
                icon = painterResource(R.drawable.wifi),
                checked = meteredArtwork,
                onCheckedChange = setMeteredArtwork,
            ),
            switchItem(
                key = "Stream on mobile data",
                title = stringResource(R.string.car_settings_metered_streaming),
                description = stringResource(R.string.car_settings_metered_streaming_desc),
                icon = painterResource(R.drawable.wifi),
                checked = meteredStreaming,
                onCheckedChange = setMeteredStreaming,
            ),
        ))
        Spacer(Modifier.height(140.dp))
    }
}
