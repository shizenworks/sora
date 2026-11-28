// AVPlayer integration follows Metrolist's iOS MusicPlayer (GPL-3.0).
package com.mochyy.mochymusic.shared.ios

import com.mochyy.mochymusic.shared.playback.PlayerController
import com.mochyy.mochymusic.shared.playback.PlayerState
import com.mochyy.mochymusic.shared.playback.QueueTransport
import com.mochyy.mochymusic.shared.playback.StreamFormat
import com.music.innertube.models.SongItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusUnknown
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.status
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

/** Owned by the Compose host. Playback actions and AVPlayer state are confined to the main thread. */
@OptIn(ExperimentalForeignApi::class)
internal class IosPlayerController(
    private val youtube: IosYouTubeClient,
    private val queueTransport: QueueTransport,
    private val playbackFailedText: () -> String,
) : PlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player = AVPlayer()
    private val mutableState = MutableStateFlow(PlayerState())
    override val state = mutableState.asStateFlow()
    private var playbackJob: Job? = null
    private var endObserver: NSObjectProtocol? = null
    private var failObserver: NSObjectProtocol? = null
    private val bridgeObservers = mutableListOf<NSObjectProtocol>()
    private var interruptionActive = false
    private var resumeAfterInterruption = false
    private var interruptedSongId: String? = null
    // the user's play/pause intent. Kept even before an item exists, so a pause or route loss
    // during stream resolution stops the song once it loads.
    private var wantsToPlay = false

    init {
        observeBridgeEvent(IosPlaybackBridgeEvents.remoteToggle) { togglePlayPause() }
        observeBridgeEvent(IosPlaybackBridgeEvents.remotePlay) { resumeFromSystemControl() }
        observeBridgeEvent(IosPlaybackBridgeEvents.remotePause) { pauseFromSystemControl() }
        observeBridgeEvent(IosPlaybackBridgeEvents.remoteSeek) { payload ->
            val seconds = payload?.let {
                runCatching {
                    Json.parseToJsonElement(it).jsonObject["positionSeconds"]?.jsonPrimitive?.doubleOrNull
                }.getOrNull()
            }
            if (seconds != null && seconds.isFinite()) seekTo((seconds * 1000).toLong())
        }
        observeBridgeEvent(IosPlaybackBridgeEvents.interruptionBegan) { pauseForInterruption() }
        observeBridgeEvent(IosPlaybackBridgeEvents.interruptionEnded) { payload ->
            val shouldResume = payload?.let {
                runCatching {
                    Json.parseToJsonElement(it).jsonObject["shouldResume"]?.jsonPrimitive?.content == "true"
                }.getOrNull()
            } == true
            finishInterruption(shouldResume)
        }
        observeBridgeEvent(IosPlaybackBridgeEvents.outputRemoved) { pauseFromSystemControl() }
        observeBridgeEvent(IosPlaybackBridgeEvents.remoteNext) { queueTransport.next() }
        observeBridgeEvent(IosPlaybackBridgeEvents.remotePrevious) { if (!queueTransport.previous()) skipToPrevious() }
        // Next-track availability follows the phone queue, so republish when it changes.
        scope.launch { queueTransport.canGoNext.collect { publishNowPlaying() } }
    }

    override fun play(song: SongItem) {
        playbackJob?.cancel()
        clearItem()
        wantsToPlay = true
        resumeAfterInterruption = interruptionActive
        interruptedSongId = song.id.takeIf { interruptionActive }
        mutableState.value = PlayerState(song = song, isLoading = true, durationMs = (song.duration ?: 0) * 1000L)
        publishNowPlaying()
        playbackJob = scope.launch {
            try {
                val stream = checkNotNull(youtube.resolve(song))
                val url = checkNotNull(NSURL.URLWithString(stream.audioUrl))
                ensureActive()
                val item = AVPlayerItem(asset = AVURLAsset(
                    uRL = url,
                    options = mapOf("AVURLAssetHTTPHeaderFieldsKey" to stream.headers),
                ))
                endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = AVPlayerItemDidPlayToEndTimeNotification,
                    `object` = item,
                    queue = NSOperationQueue.mainQueue,
                ) {
                    if (player.currentItem == item) {
                        mutableState.value = mutableState.value.copy(
                            isLoading = false,
                            isPlaying = false,
                            hasEnded = true,
                            positionMs = mutableState.value.durationMs,
                        )
                        deactivateAudioSession()
                        publishNowPlaying()
                    }
                }
                failObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = AVPlayerItemFailedToPlayToEndTimeNotification,
                    `object` = item,
                    queue = NSOperationQueue.mainQueue,
                ) {
                    if (player.currentItem == item) {
                        playbackJob?.cancel()
                        failPlayback()
                    }
                }
                val format = stream.codecs?.takeIf { it.isNotBlank() }?.let { codec ->
                    StreamFormat(
                        codec = if (codec.startsWith("mp4a")) "AAC" else codec.uppercase(),
                        sampleRate = stream.sampleRate ?: 0,
                        bitrate = stream.bitrate ?: 0,
                    )
                }
                player.replaceCurrentItemWithPlayerItem(item)
                // a pause or route loss while resolving clears wantsToPlay: install the item paused
                val playNow = wantsToPlay && !interruptionActive
                mutableState.value = mutableState.value.copy(
                    isPlaying = playNow,
                    isLoading = if (playNow) mutableState.value.isLoading else false,
                    format = format,
                )
                if (playNow) {
                    activateAudioSession()
                    player.play()
                } else {
                    player.pause()
                    if (wantsToPlay && interruptionActive) {
                        resumeAfterInterruption = true
                        interruptedSongId = song.id
                    }
                }
                publishNowPlaying()
                while (isActive) {
                    check(item.status != AVPlayerItemStatusFailed)
                    updatePosition(item)
                    delay(250)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failPlayback()
            }
        }
    }

    override fun togglePlayPause() {
        if (player.currentItem == null) {
            state.value.song?.takeIf { state.value.error != null }?.let(::play)
            return
        }
        if (mutableState.value.isPlaying) {
            wantsToPlay = false
            player.pause()
            mutableState.value = mutableState.value.copy(isPlaying = false, isLoading = false)
            resumeAfterInterruption = false
            interruptedSongId = null
            if (!interruptionActive) deactivateAudioSession()
        } else {
            wantsToPlay = true
            if (mutableState.value.hasEnded) seekTo(0)
            if (interruptionActive) {
                resumeAfterInterruption = true
                interruptedSongId = mutableState.value.song?.id
                publishNowPlaying()
                return
            }
            activateAudioSession()
            player.play()
            mutableState.value = mutableState.value.copy(isPlaying = true)
        }
        publishNowPlaying()
    }

    override fun seekTo(positionMs: Long) {
        if (player.currentItem == null) return
        val duration = mutableState.value.durationMs
        val target = positionMs.coerceIn(0L, duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
        player.seekToTime(CMTimeMakeWithSeconds(target / 1000.0, 1000))
        mutableState.value = mutableState.value.copy(positionMs = target, hasEnded = false)
        publishNowPlaying()
    }

    override fun release() {
        scope.cancel()
        clearItem()
        deactivateAudioSession()
        postBridgeEvent(IosPlaybackBridgeEvents.clearNowPlaying)
        bridgeObservers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        bridgeObservers.clear()
    }

    private fun updatePosition(item: AVPlayerItem) {
        val current = mutableState.value
        val duration = CMTimeGetSeconds(item.duration)
        val durationMs = if (duration.isFinite() && duration > 0) (duration * 1000).toLong() else current.durationMs
        val position = CMTimeGetSeconds(player.currentTime())
        val positionMs = if (position.isFinite()) (position * 1000).toLong().coerceAtLeast(0) else current.positionMs
        mutableState.value = current.copy(
            isLoading = current.isPlaying && (item.status == AVPlayerItemStatusUnknown ||
                player.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate),
            durationMs = durationMs,
            positionMs = if (current.hasEnded && durationMs > 0) durationMs else positionMs,
        )
        // lock-screen duration arrives after the first poll, and a stall starting or ending changes the
        // rate the lock screen counts at; publish on those changes, not on every tick
        val updated = mutableState.value
        if (durationMs != current.durationMs || updated.isLoading != current.isLoading) publishNowPlaying()
    }

    private fun failPlayback() {
        clearItem()
        mutableState.value = mutableState.value.copy(
            isLoading = false, isPlaying = false, error = playbackFailedText(),
        )
        deactivateAudioSession()
        postBridgeEvent(IosPlaybackBridgeEvents.clearNowPlaying)
    }

    private fun observeBridgeEvent(name: String, action: (String?) -> Unit) {
        bridgeObservers += NSNotificationCenter.defaultCenter.addObserverForName(
            name = name,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification -> action(notification?.`object` as? String) }
    }

    private fun postBridgeEvent(name: String, payload: String? = null) {
        NSNotificationCenter.defaultCenter.postNotificationName(name, payload)
    }

    private fun activateAudioSession() = postBridgeEvent(IosPlaybackBridgeEvents.activateAudioSession)

    private fun deactivateAudioSession() = postBridgeEvent(IosPlaybackBridgeEvents.deactivateAudioSession)

    private fun publishNowPlaying() {
        val current = mutableState.value
        val song = current.song
        val payload = buildJsonObject {
            put("available", song != null && player.currentItem != null)
            put("title", song?.title.orEmpty())
            put("artist", song?.artists?.joinToString { it.name }.orEmpty())
            put("album", song?.album?.name.orEmpty())
            put("artworkURL", song?.thumbnail.orEmpty())
            put("durationSeconds", current.durationMs / 1000.0)
            put("elapsedSeconds", current.positionMs / 1000.0)
            put("isPlaying", current.isPlaying)
            // the rate the lock screen advances its clock at: zero while waiting on the network, so it doesn't run ahead of the audio
            put("playbackRate", if (current.isPlaying && !current.isLoading) 1.0 else 0.0)
            put("canGoNext", queueTransport.canGoNext.value)
            // previous always has an action (restart the song, or step back), so it is on whenever a song is loaded
            put("canGoPrevious", song != null && player.currentItem != null)
        }
        postBridgeEvent(IosPlaybackBridgeEvents.publishNowPlaying, payload.toString())
    }

    private fun resumeFromSystemControl() {
        if (player.currentItem == null || mutableState.value.isPlaying) return
        wantsToPlay = true
        if (mutableState.value.hasEnded) seekTo(0)
        if (interruptionActive) {
            resumeAfterInterruption = true
            interruptedSongId = mutableState.value.song?.id
            return
        }
        resumeAfterInterruption = false
        interruptedSongId = null
        activateAudioSession()
        player.play()
        mutableState.value = mutableState.value.copy(isPlaying = true, isLoading = false)
        publishNowPlaying()
    }

    // also the route-loss path (outputRemoved). Clears intent before the item-exists check, so a
    // song still resolving does not start on speakers.
    private fun pauseFromSystemControl() {
        wantsToPlay = false
        resumeAfterInterruption = false
        interruptedSongId = null
        if (player.currentItem == null || !mutableState.value.isPlaying) return
        player.pause()
        mutableState.value = mutableState.value.copy(isPlaying = false, isLoading = false)
        if (!interruptionActive) deactivateAudioSession()
        publishNowPlaying()
    }

    private fun pauseForInterruption() {
        interruptionActive = true
        val current = mutableState.value
        resumeAfterInterruption = current.isPlaying
        interruptedSongId = current.song?.id
        if (current.isPlaying && player.currentItem != null) {
            player.pause()
            mutableState.value = current.copy(isPlaying = false, isLoading = false)
            publishNowPlaying()
        }
    }

    private fun finishInterruption(shouldResume: Boolean) {
        interruptionActive = false
        val shouldPlay = resumeAfterInterruption && shouldResume &&
            mutableState.value.song?.id == interruptedSongId && player.currentItem != null
        resumeAfterInterruption = false
        interruptedSongId = null
        if (shouldPlay) resumeFromSystemControl()
    }

    private fun clearItem() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        failObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        failObserver = null
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }
}

/** Notification names shared with the Swift system-media bridge. */
private object IosPlaybackBridgeEvents {
    private const val prefix = "com.mochyy.mochymusic.ios."
    const val publishNowPlaying = "${prefix}publishNowPlaying"
    const val clearNowPlaying = "${prefix}clearNowPlaying"
    const val activateAudioSession = "${prefix}activateAudioSession"
    const val deactivateAudioSession = "${prefix}deactivateAudioSession"
    const val remoteToggle = "${prefix}remoteToggle"
    const val remotePlay = "${prefix}remotePlay"
    const val remotePause = "${prefix}remotePause"
    const val remoteNext = "${prefix}remoteNext"
    const val remotePrevious = "${prefix}remotePrevious"
    const val remoteSeek = "${prefix}remoteSeek"
    const val interruptionBegan = "${prefix}interruptionBegan"
    const val interruptionEnded = "${prefix}interruptionEnded"
    const val outputRemoved = "${prefix}outputRemoved"
}
