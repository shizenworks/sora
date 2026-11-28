package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.AudioOffload
import com.mochyy.mochymusic.constants.DontSkipOnFailureKey
import com.mochyy.mochymusic.constants.EfficientDecodingKey
import com.mochyy.mochymusic.constants.InfiniteQueueKey
import com.mochyy.mochymusic.constants.KeepCpuAwakeKey
import com.mochyy.mochymusic.constants.QueueAddInOrderKey
import com.mochyy.mochymusic.constants.QueueEditLockKey
import com.mochyy.mochymusic.constants.QueuePreventDuplicatesKey
import com.mochyy.mochymusic.constants.SponsorBlockCategoriesKey
import com.mochyy.mochymusic.constants.SponsorBlockDefaultCategories
import com.mochyy.mochymusic.constants.SponsorBlockEnabledKey
import com.mochyy.mochymusic.constants.VideoPictureInPictureKey
import com.mochyy.mochymusic.constants.MixWithOtherAppsKey
import com.mochyy.mochymusic.constants.PauseOnMute
import com.mochyy.mochymusic.constants.PauseOnMuteRecoveryDefault
import com.mochyy.mochymusic.constants.PauseOnMuteRecoveryPercentKey
import com.mochyy.mochymusic.constants.PersistentQueueKey
import com.mochyy.mochymusic.constants.PersistentShuffleAcrossQueuesKey
import com.mochyy.mochymusic.constants.PreloadNextTrackKey
import com.mochyy.mochymusic.constants.PreloadQueueLyricsKey
import com.mochyy.mochymusic.constants.ResumeOnBluetoothConnectKey
import com.mochyy.mochymusic.constants.SkipSilenceKey
import com.mochyy.mochymusic.constants.StopPlaybackOnTaskRemovedKey
import com.mochyy.mochymusic.constants.StreamFailureTimeoutKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.audiopass_hub_title
import com.mochyy.mochymusic.shared.resources.mute_recovery_off
import com.mochyy.mochymusic.shared.resources.pause_music_when_media_is_muted
import com.mochyy.mochymusic.shared.resources.pause_music_when_muted_desc
import com.mochyy.mochymusic.shared.resources.playback_audio_offload
import com.mochyy.mochymusic.shared.resources.playback_audio_offload_desc
import com.mochyy.mochymusic.shared.resources.playback_efficient_decoding
import com.mochyy.mochymusic.shared.resources.playback_efficient_decoding_desc
import com.mochyy.mochymusic.shared.resources.playback_infinite_queue
import com.mochyy.mochymusic.shared.resources.playback_infinite_queue_desc
import com.mochyy.mochymusic.shared.resources.playback_infinite_queue_detail
import com.mochyy.mochymusic.shared.resources.playback_keep_playing
import com.mochyy.mochymusic.shared.resources.playback_keep_playing_desc
import com.mochyy.mochymusic.shared.resources.playback_keep_playing_detail
import com.mochyy.mochymusic.shared.resources.playback_mix_other_apps
import com.mochyy.mochymusic.shared.resources.playback_mix_other_apps_desc
import com.mochyy.mochymusic.shared.resources.playback_mix_other_apps_detail
import com.mochyy.mochymusic.shared.resources.playback_pip
import com.mochyy.mochymusic.shared.resources.playback_pip_desc
import com.mochyy.mochymusic.shared.resources.playback_preload_lyrics
import com.mochyy.mochymusic.shared.resources.playback_preload_lyrics_desc
import com.mochyy.mochymusic.shared.resources.playback_preload_next
import com.mochyy.mochymusic.shared.resources.playback_preload_next_desc
import com.mochyy.mochymusic.shared.resources.playback_resume_bluetooth
import com.mochyy.mochymusic.shared.resources.playback_resume_bluetooth_desc
import com.mochyy.mochymusic.shared.resources.playback_save_state
import com.mochyy.mochymusic.shared.resources.playback_save_state_desc
import com.mochyy.mochymusic.shared.resources.playback_save_state_detail
import com.mochyy.mochymusic.shared.resources.playback_skip_silence
import com.mochyy.mochymusic.shared.resources.playback_skip_silence_desc
import com.mochyy.mochymusic.shared.resources.settings_audio_pass_desc
import com.mochyy.mochymusic.shared.resources.settings_dont_skip_failure
import com.mochyy.mochymusic.shared.resources.settings_dont_skip_failure_desc
import com.mochyy.mochymusic.shared.resources.settings_dont_skip_failure_detail
import com.mochyy.mochymusic.shared.resources.settings_keep_cpu_awake
import com.mochyy.mochymusic.shared.resources.settings_keep_cpu_awake_desc
import com.mochyy.mochymusic.shared.resources.settings_mute_recovery_volume
import com.mochyy.mochymusic.shared.resources.settings_persistent_shuffle
import com.mochyy.mochymusic.shared.resources.settings_persistent_shuffle_desc
import com.mochyy.mochymusic.shared.resources.settings_playback_category
import com.mochyy.mochymusic.shared.resources.settings_queue_add_in_order
import com.mochyy.mochymusic.shared.resources.settings_queue_add_in_order_desc
import com.mochyy.mochymusic.shared.resources.settings_queue_lock
import com.mochyy.mochymusic.shared.resources.settings_queue_lock_desc
import com.mochyy.mochymusic.shared.resources.settings_queue_prevent_duplicates
import com.mochyy.mochymusic.shared.resources.settings_queue_prevent_duplicates_desc
import com.mochyy.mochymusic.shared.resources.settings_related
import com.mochyy.mochymusic.shared.resources.settings_seconds_short
import com.mochyy.mochymusic.shared.resources.settings_section_failures
import com.mochyy.mochymusic.shared.resources.settings_section_listening
import com.mochyy.mochymusic.shared.resources.settings_section_sponsorblock
import com.mochyy.mochymusic.shared.resources.settings_skip_timeout
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_desc
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_detail
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_intro
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_intro_desc
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_offtopic
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_offtopic_desc
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_outro
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_outro_desc
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_selfpromo
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_selfpromo_desc
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_sponsor
import com.mochyy.mochymusic.shared.resources.settings_sponsorblock_sponsor_desc
import com.mochyy.mochymusic.shared.resources.settings_transition_link_desc
import com.mochyy.mochymusic.shared.resources.transition_title
import com.mochyy.mochymusic.shared.resources.bluetooth
import com.mochyy.mochymusic.shared.resources.cached
import com.mochyy.mochymusic.shared.resources.clear_all
import com.mochyy.mochymusic.shared.resources.download
import com.mochyy.mochymusic.shared.resources.graphic_eq
import com.mochyy.mochymusic.shared.resources.material_rounded_graphic_eq
import com.mochyy.mochymusic.shared.resources.linear_scale
import com.mochyy.mochymusic.shared.resources.lock
import com.mochyy.mochymusic.shared.resources.lyrics
import com.mochyy.mochymusic.shared.resources.playlist_play
import com.mochyy.mochymusic.shared.resources.shuffle
import com.mochyy.mochymusic.shared.resources.skip_next
import com.mochyy.mochymusic.shared.resources.sleep_timer
import com.mochyy.mochymusic.shared.resources.slow_motion_video
import com.mochyy.mochymusic.shared.resources.speed
import com.mochyy.mochymusic.shared.resources.volume_off
import com.mochyy.mochymusic.shared.resources.volume_up
import com.mochyy.mochymusic.ui.component.DefaultPlayerSeekBarSlider
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.SubSettingsSearchBar
import com.mochyy.mochymusic.utils.rememberPreference
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

const val PlaybackSettingsRoute = "playback_settings"

// settings > playback: how music keeps going, shares the speaker, and what happens when a song won't load
@Composable
fun PlaybackSettingsScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }

    SubSettingsScaffold(
        title = stringResource(Res.string.settings_playback_category),
        onBack = { navController.navigateUp() },
        searchBar = { SubSettingsSearchBar(query = query, onQueryChange = { query = it }) },
    ) {
        SettingsSection(stringResource(Res.string.settings_section_listening), listeningItems().matching(query))
        SettingsSection(stringResource(Res.string.settings_section_failures), failureItems().matching(query))
        SettingsSection(stringResource(Res.string.settings_section_sponsorblock), sponsorBlockItems().matching(query))
        SettingsSection(stringResource(Res.string.settings_related), relatedItems(navController).matching(query))
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun listeningItems(): List<SettingsItem> {
    val (stopOnTaskRemoved, onStopOnTaskRemoved) = rememberPreference(StopPlaybackOnTaskRemovedKey, defaultValue = false)
    val (mixWithOtherApps, onMixWithOtherAppsChange) = rememberPreference(MixWithOtherAppsKey, defaultValue = false)
    val (resumeOnBluetooth, onResumeOnBluetoothChange) = rememberPreference(ResumeOnBluetoothConnectKey, defaultValue = false)
    val (pauseOnMute, onPauseOnMuteChange) = rememberPreference(PauseOnMute, defaultValue = false)
    val (muteRecoveryPercent, onMuteRecoveryPercentChange) = rememberPreference(PauseOnMuteRecoveryPercentKey, defaultValue = PauseOnMuteRecoveryDefault)
    val (preloadQueueLyrics, onPreloadQueueLyricsChange) = rememberPreference(PreloadQueueLyricsKey, defaultValue = false)
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(PersistentQueueKey, defaultValue = true)
    val (persistentShuffle, onPersistentShuffleChange) = rememberPreference(PersistentShuffleAcrossQueuesKey, defaultValue = true)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(SkipSilenceKey, defaultValue = false)
    val (preloadNextTrack, onPreloadNextTrackChange) = rememberPreference(PreloadNextTrackKey, defaultValue = true)
    val (audioOffload, onAudioOffloadChange) = rememberPreference(AudioOffload, defaultValue = false)
    val (efficientDecoding, onEfficientDecodingChange) = rememberPreference(EfficientDecodingKey, defaultValue = true)
    val (infiniteQueue, onInfiniteQueueChange) = rememberPreference(InfiniteQueueKey, defaultValue = true)
    val (keepCpuAwake, onKeepCpuAwakeChange) = rememberPreference(KeepCpuAwakeKey, defaultValue = false)
    val (preventDuplicates, onPreventDuplicatesChange) = rememberPreference(QueuePreventDuplicatesKey, defaultValue = false)
    val (videoPip, onVideoPipChange) = rememberPreference(VideoPictureInPictureKey, defaultValue = false)
    val (addInOrder, onAddInOrderChange) = rememberPreference(QueueAddInOrderKey, defaultValue = true)
    val (queueLocked, onQueueLockedChange) = rememberPreference(QueueEditLockKey, defaultValue = false)
    return listOf(
        switchItem(
            key = "Infinite queue",
            title = stringResource(Res.string.playback_infinite_queue),
            description = stringResource(Res.string.playback_infinite_queue_desc),
            detail = stringResource(Res.string.playback_infinite_queue_detail),
            icon = painterResource(Res.drawable.playlist_play),
            checked = infiniteQueue,
            onCheckedChange = onInfiniteQueueChange,
        ),
        switchItem(
            key = "Keep CPU awake during playback",
            title = stringResource(Res.string.settings_keep_cpu_awake),
            description = stringResource(Res.string.settings_keep_cpu_awake_desc),
            icon = painterResource(Res.drawable.sleep_timer),
            checked = keepCpuAwake,
            onCheckedChange = onKeepCpuAwakeChange,
        ),
        switchItem(
            key = "Prevent duplicates in queue",
            title = stringResource(Res.string.settings_queue_prevent_duplicates),
            description = stringResource(Res.string.settings_queue_prevent_duplicates_desc),
            icon = painterResource(Res.drawable.playlist_play),
            checked = preventDuplicates,
            onCheckedChange = onPreventDuplicatesChange,
        ),
        switchItem(
            key = "Add to queue in order",
            title = stringResource(Res.string.settings_queue_add_in_order),
            description = stringResource(Res.string.settings_queue_add_in_order_desc),
            icon = painterResource(Res.drawable.playlist_play),
            checked = addInOrder,
            onCheckedChange = onAddInOrderChange,
        ),
        switchItem(
            key = "Lock queue",
            title = stringResource(Res.string.settings_queue_lock),
            description = stringResource(Res.string.settings_queue_lock_desc),
            icon = painterResource(Res.drawable.lock),
            checked = queueLocked,
            onCheckedChange = onQueueLockedChange,
        ),
        switchItem(
            key = "Keep playing even when closed",
            title = stringResource(Res.string.playback_keep_playing),
            description = stringResource(Res.string.playback_keep_playing_desc),
            detail = stringResource(Res.string.playback_keep_playing_detail),
            icon = painterResource(Res.drawable.clear_all),
            checked = !stopOnTaskRemoved,
            onCheckedChange = { onStopOnTaskRemoved(!it) },
        ),
        switchItem(
            key = "Play alongside other apps",
            title = stringResource(Res.string.playback_mix_other_apps),
            description = stringResource(Res.string.playback_mix_other_apps_desc),
            detail = stringResource(Res.string.playback_mix_other_apps_detail),
            icon = painterResource(Res.drawable.volume_up),
            checked = mixWithOtherApps,
            onCheckedChange = onMixWithOtherAppsChange,
        ),
        switchItem(
            key = "Resume on Bluetooth connection",
            title = stringResource(Res.string.playback_resume_bluetooth),
            description = stringResource(Res.string.playback_resume_bluetooth_desc),
            icon = painterResource(Res.drawable.bluetooth),
            checked = resumeOnBluetooth,
            onCheckedChange = onResumeOnBluetoothChange,
        ),
        switchItem(
            key = "Picture-in-picture",
            title = stringResource(Res.string.playback_pip),
            description = stringResource(Res.string.playback_pip_desc),
            icon = painterResource(Res.drawable.slow_motion_video),
            checked = videoPip,
            onCheckedChange = onVideoPipChange,
        ),
        switchItem(
            key = "Pause when muted",
            title = stringResource(Res.string.pause_music_when_media_is_muted),
            description = stringResource(Res.string.pause_music_when_muted_desc),
            icon = painterResource(Res.drawable.volume_off),
            checked = pauseOnMute,
            onCheckedChange = onPauseOnMuteChange,
        ),
        SettingsItem(
            title = { ValueTitle(stringResource(Res.string.settings_mute_recovery_volume), if (muteRecoveryPercent == 0) stringResource(Res.string.mute_recovery_off) else "$muteRecoveryPercent%") },
            highlightKey = "Mute recovery volume",
            searchText = stringResource(Res.string.settings_mute_recovery_volume),
            description = {
                DefaultPlayerSeekBarSlider(
                    value = muteRecoveryPercent.toFloat(),
                    onValueChange = { onMuteRecoveryPercentChange(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 9,
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
            icon = painterResource(Res.drawable.volume_up),
            iconShape = CircleShape,
            isVisible = pauseOnMute,
            isSubOption = true,
        ),
        switchItem(
            key = "Save Player's last state",
            title = stringResource(Res.string.playback_save_state),
            description = stringResource(Res.string.playback_save_state_desc),
            detail = stringResource(Res.string.playback_save_state_detail),
            icon = painterResource(Res.drawable.cached),
            checked = persistentQueue,
            onCheckedChange = onPersistentQueueChange,
        ),
        // shuffle stays on from one playlist to the next, like spotify, until it's turned off
        switchItem(
            key = "Keep shuffle on",
            title = stringResource(Res.string.settings_persistent_shuffle),
            description = stringResource(Res.string.settings_persistent_shuffle_desc),
            icon = painterResource(Res.drawable.shuffle),
            checked = persistentShuffle,
            onCheckedChange = onPersistentShuffleChange,
        ),
        switchItem(
            key = "Preload next song",
            title = stringResource(Res.string.playback_preload_next),
            description = stringResource(Res.string.playback_preload_next_desc),
            icon = painterResource(Res.drawable.download),
            checked = preloadNextTrack,
            onCheckedChange = onPreloadNextTrackChange,
        ),
        switchItem(
            key = "Preload queue lyrics",
            title = stringResource(Res.string.playback_preload_lyrics),
            description = stringResource(Res.string.playback_preload_lyrics_desc),
            icon = painterResource(Res.drawable.lyrics),
            checked = preloadQueueLyrics,
            onCheckedChange = onPreloadQueueLyricsChange,
        ),
        switchItem(
            key = "Audio offload",
            title = stringResource(Res.string.playback_audio_offload),
            description = stringResource(Res.string.playback_audio_offload_desc),
            icon = painterResource(Res.drawable.speed),
            checked = audioOffload,
            onCheckedChange = onAudioOffloadChange,
        ),
        switchItem(
            key = "Efficient decoding",
            title = stringResource(Res.string.playback_efficient_decoding),
            description = stringResource(Res.string.playback_efficient_decoding_desc),
            icon = painterResource(Res.drawable.graphic_eq),
            checked = efficientDecoding,
            onCheckedChange = onEfficientDecodingChange,
        ),
        switchItem(
            key = "Skip Silence",
            title = stringResource(Res.string.playback_skip_silence),
            description = stringResource(Res.string.playback_skip_silence_desc),
            icon = painterResource(Res.drawable.skip_next),
            checked = skipSilence,
            onCheckedChange = onSkipSilenceChange,
        ),
    )
}

@Composable
private fun sponsorBlockItems(): List<SettingsItem> {
    val (enabled, onEnabledChange) = rememberPreference(SponsorBlockEnabledKey, defaultValue = false)
    val (categories, onCategoriesChange) = rememberPreference(SponsorBlockCategoriesKey, defaultValue = SponsorBlockDefaultCategories)
    fun setCategory(name: String, on: Boolean) = onCategoriesChange(if (on) categories + name else categories - name)
    return listOf(
        switchItem(
            key = "SponsorBlock",
            title = stringResource(Res.string.settings_sponsorblock),
            description = stringResource(Res.string.settings_sponsorblock_desc),
            detail = stringResource(Res.string.settings_sponsorblock_detail),
            icon = painterResource(Res.drawable.skip_next),
            checked = enabled,
            onCheckedChange = onEnabledChange,
        ),
        switchItem(
            key = "SponsorBlock non-music sections",
            title = stringResource(Res.string.settings_sponsorblock_offtopic),
            description = stringResource(Res.string.settings_sponsorblock_offtopic_desc),
            icon = painterResource(Res.drawable.skip_next),
            checked = "music_offtopic" in categories,
            onCheckedChange = { setCategory("music_offtopic", it) },
            subOptionOf = enabled,
            visible = enabled,
        ),
        switchItem(
            key = "SponsorBlock sponsor reads",
            title = stringResource(Res.string.settings_sponsorblock_sponsor),
            description = stringResource(Res.string.settings_sponsorblock_sponsor_desc),
            icon = painterResource(Res.drawable.skip_next),
            checked = "sponsor" in categories,
            onCheckedChange = { setCategory("sponsor", it) },
            subOptionOf = enabled,
            visible = enabled,
        ),
        switchItem(
            key = "SponsorBlock self-promotion",
            title = stringResource(Res.string.settings_sponsorblock_selfpromo),
            description = stringResource(Res.string.settings_sponsorblock_selfpromo_desc),
            icon = painterResource(Res.drawable.skip_next),
            checked = "selfpromo" in categories,
            onCheckedChange = { setCategory("selfpromo", it) },
            subOptionOf = enabled,
            visible = enabled,
        ),
        switchItem(
            key = "SponsorBlock intros",
            title = stringResource(Res.string.settings_sponsorblock_intro),
            description = stringResource(Res.string.settings_sponsorblock_intro_desc),
            icon = painterResource(Res.drawable.skip_next),
            checked = "intro" in categories,
            onCheckedChange = { setCategory("intro", it) },
            subOptionOf = enabled,
            visible = enabled,
        ),
        switchItem(
            key = "SponsorBlock outros",
            title = stringResource(Res.string.settings_sponsorblock_outro),
            description = stringResource(Res.string.settings_sponsorblock_outro_desc),
            icon = painterResource(Res.drawable.skip_next),
            checked = "outro" in categories,
            onCheckedChange = { setCategory("outro", it) },
            subOptionOf = enabled,
            visible = enabled,
        ),
    )
}

@Composable
private fun failureItems(): List<SettingsItem> {
    val (dontSkipOnFailure, onDontSkipOnFailureChange) = rememberPreference(DontSkipOnFailureKey, defaultValue = false)
    val (failureTimeout, onFailureTimeoutChange) = rememberPreference(StreamFailureTimeoutKey, defaultValue = 6)
    return listOf(
        switchItem(
            key = "Don't skip on failure",
            title = stringResource(Res.string.settings_dont_skip_failure),
            description = stringResource(Res.string.settings_dont_skip_failure_desc),
            detail = stringResource(Res.string.settings_dont_skip_failure_detail),
            icon = painterResource(Res.drawable.skip_next),
            checked = dontSkipOnFailure,
            onCheckedChange = onDontSkipOnFailureChange,
        ),
        SettingsItem(
            title = { ValueTitle(stringResource(Res.string.settings_skip_timeout), stringResource(Res.string.settings_seconds_short, failureTimeout)) },
            highlightKey = "Skip timeout failure",
            searchText = stringResource(Res.string.settings_skip_timeout),
            description = {
                DefaultPlayerSeekBarSlider(
                    value = failureTimeout.toFloat(),
                    onValueChange = { onFailureTimeoutChange(it.toInt()) },
                    valueRange = 5f..20f,
                    steps = 14,
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
            icon = painterResource(Res.drawable.speed),
            iconShape = CircleShape,
            isVisible = !dontSkipOnFailure,
            isSubOption = true,
        ),
    )
}

// the sound side of playback lives in audio lab, these take people there
@Composable
private fun relatedItems(navController: NavController): List<SettingsItem> = listOf(
    linkItem(
        key = "Transition crossfade",
        title = stringResource(Res.string.transition_title),
        description = stringResource(Res.string.settings_transition_link_desc),
        icon = painterResource(Res.drawable.linear_scale),
        onClick = { navController.navigate("crossmix_settings") },
    ),
    linkItem(
        key = "Audio Pass output usb dac",
        title = stringResource(Res.string.audiopass_hub_title),
        description = stringResource(Res.string.settings_audio_pass_desc),
        icon = painterResource(Res.drawable.material_rounded_graphic_eq),
        onClick = { navController.navigate("audio_pass") },
    ),
)
