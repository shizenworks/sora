// PlayerConnection.kt

package com.mochyy.mochymusic.playback

import com.mochyy.mochymusic.constants.QueueEditLockKey
import com.mochyy.mochymusic.utils.dataStore
import com.mochyy.mochymusic.utils.get
import android.content.Context
import com.mochyy.mochymusic.ui.component.TopToaster
import com.mochyy.mochymusic.R
import androidx.compose.runtime.Stable
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Timeline
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.mochyy.mochymusic.db.MusicDatabase
import com.mochyy.mochymusic.extensions.currentMetadata
import com.mochyy.mochymusic.lyrics.LyricsUtils
import com.mochyy.mochymusic.lyrics.VideoLyricsSync
import com.mochyy.mochymusic.extensions.getCurrentQueueIndex
import com.mochyy.mochymusic.extensions.getQueueWindows
import com.mochyy.mochymusic.extensions.isAutoplay
import com.mochyy.mochymusic.extensions.isManualQueued
import com.mochyy.mochymusic.extensions.withAutoplay
import com.mochyy.mochymusic.extensions.metadata
import com.mochyy.mochymusic.models.MediaMetadata
import com.mochyy.mochymusic.models.toMediaMetadata
import com.mochyy.mochymusic.extensions.toMediaItem
import com.mochyy.mochymusic.songedit.SongEdits
import com.mochyy.mochymusic.playback.MusicService.MusicBinder
import com.mochyy.mochymusic.link.isTelevision
import com.mochyy.mochymusic.playback.queues.Queue
import com.mochyy.mochymusic.playback.queues.ListQueue
import com.mochyy.mochymusic.playback.queues.YouTubeQueue
import com.mochyy.mochymusic.ui.player.models.ProgressState
import com.mochyy.mochymusic.shared.playback.PlaybackConnection as SharedPlaybackConnection
import com.mochyy.mochymusic.shared.playback.PlaybackQueueItem
import com.mochyy.mochymusic.shared.playback.PlaybackQueueState
import com.mochyy.mochymusic.shared.playback.PlaybackRepeatMode
import com.mochyy.mochymusic.shared.playback.PlayerQueueWindow
import com.mochyy.mochymusic.shared.playback.PlayerState
import com.mochyy.mochymusic.shared.playback.Queue as PlaybackQueue
import com.music.innertube.models.Album as YouTubeAlbum
import com.music.innertube.models.Artist as YouTubeArtist
import com.music.innertube.models.SongItem
import com.mochyy.mochymusic.utils.NotRecommended
import com.mochyy.mochymusic.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Stable
@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    val context: Context,
    binder: MusicBinder,
    val database: MusicDatabase,
    parentScope: CoroutineScope,
) : Player.Listener, SharedPlaybackConnection {
    private companion object {
        private const val TAG = "PlayerConnection"
        private const val PLAYER_INIT_TIMEOUT_MS = 5000L
        private const val VideoLyricsSyncDelayMs = 1_500L
    }

    // its own job under the parent's, so dispose() can stop the pollers and flows this connection started
    val scope: CoroutineScope =
        CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))

    val service = binder.service
    // mainactivity turns this off while the app is hidden, the poll and lyric warmers below rest then instead of working for nobody
    val isUiVisible = MutableStateFlow(true)
    private val playerReadinessFlow = service.isPlayerReady

    private fun getPlayerSafe(): ExoPlayer {
        return try {
            if (!playerReadinessFlow.value) {
                Timber.tag(TAG).w("Player accessed before service initialization complete; returning best-effort reference")
            }
            service.player
        } catch (e: UninitializedPropertyAccessException) {
            Timber.tag(TAG).e(e, "Fatal: player property accessed but not initialized")
            throw IllegalStateException("MusicService.player not initialized; possible race condition in service startup", e)
        }
    }

    val player: ExoPlayer
        get() = getPlayerSafe()

    private val isPlayerInitialized = MutableStateFlow(service.isPlayerReady.value)

    val playbackState: MutableStateFlow<Int>
    private val playWhenReady: MutableStateFlow<Boolean>
    override val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean>

    init {
        Timber.tag(TAG).d("PlayerConnection init: playerReady=${playerReadinessFlow.value}")

        val initialState = try {
            val initialPlayer = getPlayerSafe()
            Triple(initialPlayer.playbackState, initialPlayer.playWhenReady,
                   initialPlayer.playWhenReady && initialPlayer.playbackState != STATE_ENDED)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during PlayerConnection initialization, using defaults")
            Triple(Player.STATE_IDLE, false, false)
        }

        playbackState = MutableStateFlow(initialState.first)
        playWhenReady = MutableStateFlow(initialState.second)
        // a pause or play fade shows what you pressed straight away, not when the player catches up
        isPlaying = combine(playbackState, playWhenReady, service.transport.playIntent) { state, ready, intent ->
            (intent ?: ready) && state != STATE_ENDED
        }.stateIn(
            scope,
            SharingStarted.Lazily,
            initialState.third
        )

        scope.launch {
            playerReadinessFlow.collect { ready ->
                isPlayerInitialized.value = ready
                if (ready) {
                    Timber.tag(TAG).d("Service player initialization detected by PlayerConnection")
                }
            }
        }

        Timber.tag(TAG).d("PlayerConnection state flows initialized successfully")
    }

    val isEffectivelyPlaying = combine(
        isPlaying,
        service.castConnectionHandler?.isCasting ?: MutableStateFlow(false),
        service.castConnectionHandler?.castIsPlaying ?: MutableStateFlow(false)
    ) { localPlaying, isCasting, castPlaying ->
        if (isCasting) castPlaying else localPlaying
    }.stateIn(
        scope,
        SharingStarted.Lazily,
        player.playbackState != STATE_ENDED && player.playWhenReady
    )

    val effectivePlaybackState: StateFlow<Int> = combine(
        playbackState,
        service.castConnectionHandler?.isCasting ?: MutableStateFlow(false),
        service.castConnectionHandler?.castIsBuffering ?: MutableStateFlow(false),
    ) { localState, casting, castBuffering ->
        if (casting) {
            if (castBuffering) Player.STATE_BUFFERING else Player.STATE_READY
        } else {
            localState
        }
    }.stateIn(scope, SharingStarted.Lazily, player.playbackState)

    /** Current playback position in ms, following the cast receiver while casting. */
    val effectivePositionMs: Long
        get() {
            val cast = service.castConnectionHandler
            if (cast?.isCasting?.value == true) return cast.currentPositionNow().coerceAtLeast(0L)
            return attachedPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L
        }

    /** Current track duration in ms, following the cast receiver (or track metadata) while casting. */
    val effectiveDurationMs: Long
        get() {
            val cast = service.castConnectionHandler
            if (cast?.isCasting?.value == true) {
                val castDur = cast.castDuration.value
                if (castDur > 0L) return castDur
                val metaDur = mediaMetadata.value?.duration?.toLong()?.times(1000L) ?: 0L
                if (metaDur > 0L) return metaDur
            }
            val localDur = attachedPlayer?.duration ?: C.TIME_UNSET
            if (localDur != C.TIME_UNSET && localDur > 0L) return localDur
            return (mediaMetadata.value?.duration?.toLong()?.times(1000L) ?: 0L).coerceAtLeast(0L)
        }

    /** Whether playback is actively advancing right now (not paused, not buffering). */
    val isEffectivelyPlayingNow: Boolean
        get() {
            val cast = service.castConnectionHandler
            if (cast?.isCasting?.value == true) {
                return cast.castIsPlaying.value && !cast.castIsBuffering.value
            }
            return attachedPlayer?.isPlaying == true
        }

    // the shared contract's position and length follow the cast receiver too
    override val positionMs: Long get() = effectivePositionMs
    override val durationMs: Long get() = effectiveDurationMs

    val effectivePlaybackSpeed: Float
        get() {
            val cast = service.castConnectionHandler
            if (cast?.isCasting?.value == true) return 1f
            return attachedPlayer?.playbackParameters?.speed ?: 1f
        }

    // the queue's own copy of the playing song, before the hand edits from its edit sheet go over it
    @Volatile private var queueMetadata: MediaMetadata? = player.currentMetadata
    val mediaMetadata = MutableStateFlow(queueMetadata?.let(SongEdits::applyTo))

    init {
        // a song edited while it's already in the queue shows the edit straight away
        scope.launch { SongEdits.all.collect { mediaMetadata.value = queueMetadata?.let(SongEdits::applyTo) } }
    }

    private fun showMetadata(metadata: MediaMetadata?) {
        queueMetadata = metadata
        mediaMetadata.value = metadata?.let(SongEdits::applyTo)
    }

    /** The playing song in the model shared screens show; the app's own callers keep using [mediaMetadata]. */
    override val nowPlaying: StateFlow<SongItem?> = mediaMetadata
        .map { it?.toSongItem() }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), mediaMetadata.value?.toSongItem())

    val currentSong =
        mediaMetadata.flatMapLatest {
            database.song(it?.id)
        }

    /**
     * How many ms the current song's lyrics are shifted. Positive shows each line sooner, so it is
     * added to the playback position wherever lyrics are timed.
     */
    val lyricsOffsetMs: StateFlow<Int> = currentSong
        .map { it?.song?.lyricsOffset ?: 0 }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val currentLyrics = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.lyrics(mediaMetadata?.id)
    }

    init {
        // parse off the main thread only while the ui is visible, so background song changes stay quiet
        scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            isUiVisible
                .flatMapLatest { visible ->
                    if (visible) currentLyrics.map { it?.lyrics }.distinctUntilChanged() else kotlinx.coroutines.flow.emptyFlow()
                }
                .collect(LyricsUtils::warmParsedLyrics)
        }
        // a music video shows its lyrics straight away, then they move onto the video's own timing once it's been listened to
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isUiVisible
                .flatMapLatest { visible ->
                    if (visible) combine(mediaMetadata, currentLyrics) { metadata, lyrics -> metadata to lyrics }.distinctUntilChanged()
                    else kotlinx.coroutines.flow.emptyFlow()
                }
                .collectLatest { (metadata, lyrics) ->
                    if (metadata == null || lyrics == null) return@collectLatest
                    // a song skipped straight past never asks youtube anything
                    delay(VideoLyricsSyncDelayMs)
                    runCatching {
                        VideoLyricsSync.syncIfNeeded(context, database, metadata, lyrics) { albumSongId ->
                            service.matchSongToVideo(albumSongId, metadata.id)
                        }
                    }
                        .onFailure { if (it is kotlinx.coroutines.CancellationException) throw it else reportException(it) }
                }
        }
    }
    val currentFormat =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    /**
     * What [mediaId] is playing as, for the format badge: the service's facts (the decoder's
     * format, a module's codec name) with YouTube's record of its format filling in until the
     * decoder has it, unless a module plays it, whose stream that record doesn't describe.
     */
    fun streamFacts(mediaId: String): kotlinx.coroutines.flow.Flow<StreamFacts?> =
        combine(
            service.streamFacts.map { it[mediaId] }.distinctUntilChanged(),
            database.format(mediaId),
        ) { live, format ->
            val recorded = format?.takeIf { live?.fromModule != true }?.let(StreamFacts::of)
            when {
                live == null -> recorded
                recorded == null -> live
                else -> live.copy(codec = live.codec ?: recorded.codec, sampleRate = live.sampleRate ?: recorded.sampleRate)
            }
        }.distinctUntilChanged()

    val queueTitle = MutableStateFlow<String?>(null)
    val queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    /**
     * The queue's items and the current song's position in them, published as one value from the
     * same player callback. queueItems and currentWindowIndex arrive separately (one goes through
     * a map on another dispatcher), so a reader pairing them saw the new index against the old
     * list for a frame whenever both changed at once - clearing history, toggling shuffle.
     */
    val queueSnapshot = MutableStateFlow(com.mochyy.mochymusic.ui.player.models.QueueState())
    // reuse snapshot items instead of mapping a second copy of the whole queue
    val queueItems: kotlinx.coroutines.flow.StateFlow<List<com.mochyy.mochymusic.ui.player.models.QueueItemData>> = queueSnapshot
        .map { it.items }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), emptyList())
    private var snapshotWindows: List<Timeline.Window>? = null
    private var snapshotItems: List<com.mochyy.mochymusic.ui.player.models.QueueItemData> = emptyList()

    private fun toQueueItem(window: Timeline.Window): com.mochyy.mochymusic.ui.player.models.QueueItemData {
        val mediaItem = window.mediaItem
        return com.mochyy.mochymusic.ui.player.models.QueueItemData(
            uid = window.uid.hashCode(),
            mediaId = mediaItem.mediaId,
            title = mediaItem.mediaMetadata.title?.toString() ?: "",
            artist = mediaItem.mediaMetadata.artist?.toString() ?: "",
            artworkUri = mediaItem.mediaMetadata.artworkUri,
            duration = window.durationMs,
            isAutoplay = mediaItem.isAutoplay,
            isManual = mediaItem.isManualQueued,
        )
    }

    private fun publishQueueSnapshot() {
        val windows = queueWindows.value
        // A song change moves the index but not the list: only remap when the windows changed.
        if (windows !== snapshotWindows) {
            snapshotItems = windows.map(::toQueueItem)
            snapshotWindows = windows
        }
        queueSnapshot.value = com.mochyy.mochymusic.ui.player.models.QueueState(
            items = snapshotItems,
            currentIndex = currentWindowIndex.value,
            title = queueTitle.value.orEmpty(),
        )
    }

    /** The songs either side of the playing one, in play order, for the shared player UI. */
    override val queueWindow: StateFlow<PlayerQueueWindow> =
        combine(queueWindows, currentWindowIndex) { windows, index ->
            PlayerQueueWindow(
                previous = windows.getOrNull(index - 1)?.mediaItem?.metadata?.toSongItem(),
                current = windows.getOrNull(index)?.mediaItem?.metadata?.toSongItem()
                    ?: mediaMetadata.value?.toSongItem(),
                next = windows.getOrNull(index + 1)?.mediaItem?.metadata?.toSongItem(),
            )
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(5_000L),
            PlayerQueueWindow(current = mediaMetadata.value?.toSongItem()),
        )

    private val isPositionAdvancing = MutableStateFlow(false)
    private val progress = MutableStateFlow(ProgressState())
    val progressState: StateFlow<ProgressState> = progress.asStateFlow()

    private fun publishProgress(player: Player) {
        // while casting the receiver owns the position, the local player sits paused
        val cast = service.castConnectionHandler
        if (cast?.isCasting?.value == true) {
            val position = cast.currentPositionNow().coerceAtLeast(0L)
            val duration = effectiveDurationMs
            progress.value = ProgressState(
                position = position,
                duration = duration,
                percentage = if (duration > 0L) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            )
            return
        }
        val position = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)
        progress.value = ProgressState(
            position = position,
            duration = duration,
            percentage = if (duration > 0L) position.toFloat() / duration else 0f,
        )
    }

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)
    val isMuted = service.isMuted

    val waitingForNetworkConnection = service.waitingForNetworkConnection

    /** A failed stream counting down to its skip, for the seek bars */
    val streamFailure = service.streamFailure

    /** Crossmix has the next song playing over the end of this one */
    val isCrossmixing = service.crossmixMixing

    /** asks the player to open, then open its studio (the selector); from "Customize player" in settings */
    val studioRequests = kotlinx.coroutines.flow.MutableStateFlow(0L)

    fun requestPlayerStudio() {
        studioRequests.value = System.nanoTime()
    }

    /** Crossmix's last decision, for its test tools */
    val crossmixStatus = service.crossmixStatus

    fun testCrossmix() = service.testCrossmix()

    var onSkipPrevious: (() -> Unit)? = null
    var onSkipNext: (() -> Unit)? = null

    private var attachedPlayer: Player? = null

    init {
        try {
            scope.launch {
                // Player events handle paused seeks and track changes without a periodic wakeup.
                combine(isUiVisible, isPositionAdvancing) { visible, advancing -> visible && advancing }
                    .distinctUntilChanged()
                    .collectLatest { poll ->
                        if (!poll) return@collectLatest
                        while (true) {
                            attachedPlayer?.let(::publishProgress)
                            delay(66L)
                        }
                    }
            }

            scope.launch {
                service.playerFlow.collect { newPlayer ->
                    if (newPlayer != null && newPlayer != attachedPlayer) {
                        updateAttachedPlayer(newPlayer)
                    }
                }
            }

            if (attachedPlayer == null && service.isPlayerReady.value) {
                 updateAttachedPlayer(player)
            }

            // wake the progress poller whenever cast playback starts, pauses, buffers, seeks or changes duration
            service.castConnectionHandler?.let { cast ->
                scope.launch {
                    combine(
                        cast.isCasting,
                        cast.castIsPlaying,
                        cast.castIsBuffering,
                        cast.castDuration,
                        cast.castPosition,
                    ) { _, _, _, _, _ -> }.collect {
                        attachedPlayer?.let { player ->
                            publishProgress(player)
                            refreshAdvancing(player)
                        }
                    }
                }
            }

            Timber.tag(TAG).d("PlayerConnection flow observer registered")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize PlayerConnection listener or state")

            throw e
        }
    }

    private fun updateAttachedPlayer(newPlayer: Player) {
        attachedPlayer?.removeListener(this)
        attachedPlayer = newPlayer
        newPlayer.addListener(this)

        publishProgress(newPlayer)
        refreshAdvancing(newPlayer)
        playbackState.value = newPlayer.playbackState
        playWhenReady.value = newPlayer.playWhenReady
        showMetadata(newPlayer.currentMetadata)
        queueTitle.value = service.queueTitle
        queueWindows.value = newPlayer.getQueueWindows()
        currentWindowIndex.value = newPlayer.getCurrentQueueIndex()
        currentMediaItemIndex.value = newPlayer.currentMediaItemIndex
        shuffleModeEnabled.value = newPlayer.shuffleModeEnabled
        repeatMode.value = newPlayer.repeatMode
        publishQueueSnapshot()

        Timber.tag(TAG).d("Attached to new player instance: $newPlayer")
    }

    // the position poll runs while the local player, or the cast receiver while casting, is moving
    private fun refreshAdvancing(player: Player) {
        val cast = service.castConnectionHandler
        isPositionAdvancing.value = if (cast?.isCasting?.value == true) {
            cast.castIsPlaying.value || cast.castIsBuffering.value
        } else {
            player.isPlaying
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (player !== attachedPlayer) return
        if (events.containsAny(
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_POSITION_DISCONTINUITY,
            )
        ) {
            publishProgress(player)
            refreshAdvancing(player)
        }
    }

    // a device this phone steers takes the songs picked here; never on a tv, which steers nothing
    private val remote: com.mochyy.mochymusic.link.remote.RemoteControl? by lazy {
        if (context.isTelevision()) null
        else dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.mochyy.mochymusic.link.remote.RemoteControlEntryPoint::class.java,
        ).remoteControl()
    }

    fun playQueue(queue: Queue) {
        if (remote?.takeQueue(queue) == true) return
        if (!playerReadinessFlow.value) {
            Timber.tag(TAG).w("playQueue called before player ready; delegating to service")
        }
        try {
            service.playQueue(queue)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in playQueue")
            throw e
        }
    }

    /**
     * A portable queue from shared code: its first page becomes a media3 ListQueue, which the service
     * reads exactly like any other. Later pages are not wired yet (no shared queue has any this wave).
     */
    override fun playQueue(queue: PlaybackQueue) {
        scope.launch {
            val status = try {
                queue.getInitialStatus()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Timber.tag(TAG).e(e, "Error reading shared queue")
                return@launch
            }
            if (status.items.isEmpty()) return@launch
            playQueue(
                ListQueue(
                    title = status.title,
                    items = status.items.map { it.toMediaItem() },
                    startIndex = status.mediaItemIndex.coerceIn(status.items.indices),
                    position = status.position,
                )
            )
        }
    }

    fun startRadioSeamlessly() {

        if (!playerReadinessFlow.value) {
            Timber.tag(TAG).w("startRadioSeamlessly called before player ready; delegating to service")
        }
        try {
            service.startRadioSeamlessly()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in startRadioSeamlessly")
            throw e
        }
    }

    fun setInfiniteQueueEnabled(enabled: Boolean) = service.setInfiniteQueueEnabled(enabled)

    fun playNext(item: MediaItem) = playNext(listOf(item))

    override fun playNext(song: SongItem) = playNext(song.toMediaItem())

    fun playNext(items: List<MediaItem>) {
        if (remote?.takeNext(items) == true) return
        try {
            service.playNext(items)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in playNext")
            throw e
        }
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    override fun addToQueue(song: SongItem) = addToQueue(song.toMediaItem())

    fun addToQueue(items: List<MediaItem>) {
        if (remote?.takeQueueEnd(items) == true) return
        try {
            service.addToQueue(items)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in addToQueue")
            throw e
        }
        val first = items.firstOrNull() ?: return
        TopToaster.show(
            text = if (items.size == 1) "Added ${first.mediaMetadata.title ?: "song"} to Queue" else "Added ${items.size} songs to Queue",
            thumbnail = first.mediaMetadata.artworkUri?.toString(),
            iconRes = R.drawable.queue_music,
        )
    }

    // Queue page operations. Items are addressed by the uid the queue list was built from
    // (QueueItemData.uid) and looked up again at call time, so a timeline change between the
    // tap and the call can't land the action on the wrong song.

    private fun mediaIndexOf(uid: Int): Int {
        val timeline = player.currentTimeline
        val window = Timeline.Window()
        for (i in 0 until timeline.windowCount) {
            if (timeline.getWindow(i, window).uid.hashCode() == uid) return i
        }
        return C.INDEX_UNSET
    }

    /** Media item indices in the order they will play, shuffle included. */
    private fun playOrder(): MutableList<Int> {
        val timeline = player.currentTimeline
        val order = ArrayList<Int>(timeline.windowCount)
        if (timeline.isEmpty) return order
        val shuffle = player.shuffleModeEnabled
        var index = timeline.getFirstWindowIndex(shuffle)
        while (index != C.INDEX_UNSET && order.size < timeline.windowCount) {
            order += index
            index = timeline.getNextWindowIndex(index, REPEAT_MODE_OFF, shuffle)
        }
        return order
    }

    // a tap in the queue is picked like a song from a list, or blended in; the uid is looked up again when the song really changes
    fun playQueueItem(uid: Int) {
        if (mediaIndexOf(uid) == C.INDEX_UNSET) return
        try {
            service.transport.pickFromQueue { mediaIndexOf(uid) }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in playQueueItem")
        }
    }

    /** Moves a song to play straight after [afterUid]. */
    /** The queue lock: while on, songs can't be removed, moved or played next from the queue. */
    private fun queueLocked(): Boolean = service.dataStore.get(QueueEditLockKey, false)

    fun moveQueueItem(uid: Int, afterUid: Int) {
        if (queueLocked()) return
        val from = mediaIndexOf(uid)
        val after = mediaIndexOf(afterUid)
        if (from == C.INDEX_UNSET || after == C.INDEX_UNSET || from == after) return
        try {
            if (!player.shuffleModeEnabled) {
                // moveMediaItem takes the final index, counted once the song is out of the way.
                val to = if (from < after) after else after + 1
                if (to != from) player.moveMediaItem(from, to)
            } else {
                // Shuffled, the play order is the shuffle order, not the item order: reorder that.
                val order = playOrder()
                order.remove(from)
                order.add(order.indexOf(after) + 1, from)
                player.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), System.currentTimeMillis()))
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in moveQueueItem")
        }
    }

    /** Takes a song out of wherever it is in the queue and puts it next, as one the user queued. */
    fun playQueueItemNext(uid: Int) {
        if (queueLocked()) return
        val index = mediaIndexOf(uid)
        if (index == C.INDEX_UNSET || index == player.currentMediaItemIndex) return
        try {
            val item = player.getMediaItemAt(index).withAutoplay(false)
            player.removeMediaItem(index)
            service.playNext(listOf(item))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in playQueueItemNext")
        }
    }

    /** Removes songs from the queue. The one playing is never removed. */
    fun removeQueueItems(uids: Collection<Int>) {
        if (uids.isEmpty() || queueLocked()) return
        val wanted = uids.toHashSet()
        val timeline = player.currentTimeline
        val window = Timeline.Window()
        val current = player.currentMediaItemIndex
        val indices = (0 until timeline.windowCount).filter {
            it != current && timeline.getWindow(it, window).uid.hashCode() in wanted
        }
        if (indices.isEmpty()) return
        // kept for the Undo, which puts each one back where it was
        val removed = indices.map { it to player.getMediaItemAt(it) }
        try {
            // Back to front in contiguous runs: one timeline change per run instead of per song,
            // and earlier indices stay valid while later ones go.
            var end = indices.size - 1
            while (end >= 0) {
                var start = end
                while (start > 0 && indices[start - 1] == indices[start] - 1) start--
                player.removeMediaItems(indices[start], indices[end] + 1)
                end = start - 1
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in removeQueueItems")
            return
        }
        val first = removed.first().second
        TopToaster.show(
            text = if (removed.size == 1) "Removed ${first.mediaMetadata.title ?: "song"} from queue" else "Removed ${removed.size} songs from queue",
            thumbnail = first.mediaMetadata.artworkUri?.toString(),
            iconRes = R.drawable.queue_music,
            actionLabel = "Undo",
            onAction = {
                // ascending, so each lands at its old index once the ones before it are back
                removed.forEach { (index, item) -> player.addMediaItem(index.coerceAtMost(player.mediaItemCount), item) }
            },
        )
    }

    // the song playing now stops coming back in autoplay, radios and home, and we skip past it right away
    fun stopRecommendingCurrent() {
        val metadata = player.currentMetadata ?: return
        NotRecommended.add(metadata.id)
        seekToNext()
        TopToaster.show(
            text = context.getString(R.string.not_recommended_done),
            thumbnail = metadata.thumbnailUrl,
            iconRes = R.drawable.thumb_down,
            actionLabel = context.getString(R.string.undo),
            onAction = { NotRecommended.remove(metadata.id) },
        )
    }

    fun toggleLike() {
        try {
            service.toggleLike()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in toggleLike")
        }
    }

    fun toggleMute() {
        service.toggleMute()
    }

    fun setMuted(muted: Boolean) {
        service.setMuted(muted)
    }

    fun toggleLibrary() {
        try {
            service.toggleLibrary()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in toggleLibrary")
        }
    }

    override fun togglePlayPause() {
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                if (castHandler.castIsPlaying.value) {
                    castHandler.pause()
                } else {
                    castHandler.play()
                }
            } else if (service.partyLockedTransport(!service.player.isPlaying)) {
                // a Listen Together room locked to its host: the press moved this
                // device alone, through the ported engine's door, and nothing else
            } else {
                service.transport.togglePlayPause()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in togglePlayPause")
        }
    }

    override fun play() {
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.play()
            } else {
                service.transport.play()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in play")
        }
    }

    override fun pause() {
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.pause()
            } else {
                service.transport.pause()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in pause")
        }
    }

    override fun seekTo(positionMs: Long) {
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.seekTo(positionMs)
            } else {
                service.transport.cancel()
                player.seekTo(positionMs)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in seekTo")
        }
    }

    fun seekToNext() {
        try {

            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.skipToNext()
                return
            }
            service.transport.skipNext()
            onSkipNext?.invoke()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in seekToNext")
        }
    }

    var onRestartSong: (() -> Unit)? = null

    // a swipe back: always the song before, never a restart, faded or blended like a skip
    fun skipToPreviousSong() {
        try {
            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                castHandler.skipToPrevious()
                return
            }
            service.transport.jumpTo { player.previousMediaItemIndex }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in skipToPreviousSong")
        }
    }

    fun seekToPrevious() {
        try {

            val castHandler = service.castConnectionHandler
            if (castHandler?.isCasting?.value == true) {
                if (effectivePositionMs > 3000L || !player.hasPreviousMediaItem()) {
                    castHandler.seekTo(0L)
                    attachedPlayer?.let(::publishProgress)
                    onRestartSong?.invoke()
                } else {
                    castHandler.skipToPrevious()
                }
                return
            }

            val restarts = player.currentPosition > 3000 || !player.hasPreviousMediaItem()
            service.transport.skipPrevious()
            if (restarts) onRestartSong?.invoke() else onSkipPrevious?.invoke()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error in seekToPrevious")
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        playbackState.value = state
        error.value = player.playerError
    }

    override fun onPlayWhenReadyChanged(
        newPlayWhenReady: Boolean,
        reason: Int,
    ) {
        playWhenReady.value = newPlayWhenReady
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        showMetadata(mediaItem?.metadata)
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        publishQueueSnapshot()
        updateCanSkipPreviousAndNext()
    }

    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
        super.onTracksChanged(tracks)
        val format = player.audioFormat ?: return
        val mediaItem = player.currentMediaItem ?: return
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val isLocal = database.song(mediaItem.mediaId).first()?.song?.isLocal == true
                if (isLocal) {
                    database.query {
                        upsert(
                            com.mochyy.mochymusic.db.entities.FormatEntity(
                                id = mediaItem.mediaId,
                                itag = -1,
                                mimeType = format.sampleMimeType?.split(";")?.get(0) ?: "audio/flac",
                                codecs = format.codecs ?: "",
                                bitrate = format.bitrate,
                                sampleRate = format.sampleRate,
                                contentLength = 0L,
                                loudnessDb = null,
                                perceptualLoudnessDb = null,
                                playbackUrl = null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error updating local format entity")
            }
        }
    }

    override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        val artworkData = mediaMetadata.artworkData ?: return
        val mediaItem = player.currentMediaItem ?: return

        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val songEntity = database.song(mediaItem.mediaId).first() ?: return@launch
                val thumbnailUrl = songEntity.song.thumbnailUrl
                // a cover file that was cleared away counts as none, so playing the song brings it back
                if (songEntity.song.isLocal &&
                    (thumbnailUrl == null || com.mochyy.mochymusic.importer.LocalArtwork.needsRepair(context, thumbnailUrl))
                ) {
                    val newThumbnailUrl = com.mochyy.mochymusic.importer.LocalArtwork.save(context, artworkData)
                        ?: return@launch
                    database.query {
                        upsert(songEntity.song.copy(thumbnailUrl = newThumbnailUrl))
                    }
                    Timber.tag(TAG).d("Extracted local artwork to $newThumbnailUrl")

                    val currentMetadata = queueMetadata
                    if (currentMetadata?.id == mediaItem.mediaId) {
                        showMetadata(currentMetadata.copy(thumbnailUrl = newThumbnailUrl))
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error extracting local artwork")
            }
        }
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        queueWindows.value = player.getQueueWindows()
        queueTitle.value = service.queueTitle
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        publishQueueSnapshot()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
        queueWindows.value = player.getQueueWindows()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        publishQueueSnapshot()
        updateCanSkipPreviousAndNext()
    }

    override fun onRepeatModeChanged(mode: Int) {
        repeatMode.value = mode
        updateCanSkipPreviousAndNext()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        error.value = playbackError
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            val window =
                player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                    !window.isLive ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            canSkipNext.value = window.isLive &&
                    window.isDynamic ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    val uiState: com.mochyy.mochymusic.ui.player.PlayerUiState by lazy { com.mochyy.mochymusic.ui.player.PlayerUiState(this) }

    override val state: StateFlow<PlayerState> = combine(
        mediaMetadata,
        isPlaying,
        progressState,
        playbackState,
        error,
    ) { metadata, playing, progress, playerState, playbackError ->
        val song = metadata?.toSongItem()
        PlayerState(
            song = song,
            isPlaying = playing,
            isLoading = playerState == Player.STATE_BUFFERING,
            hasEnded = playerState == STATE_ENDED,
            positionMs = progress.position,
            durationMs = progress.duration.takeIf { it > 0L } ?: ((song?.duration ?: 0) * 1000L),
            error = playbackError?.localizedMessage,
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000L), PlayerState())

    override val queueState: StateFlow<PlaybackQueueState> = combine(
        queueSnapshot,
        shuffleModeEnabled,
        repeatMode,
        canSkipNext,
        canSkipPrevious,
    ) { snapshot, shuffle, repeat, canNext, canPrevious ->
        PlaybackQueueState(
            title = snapshot.title,
            items = snapshot.items.map { item ->
                PlaybackQueueItem(
                    queueId = item.uid.toString(),
                    songId = item.mediaId,
                    title = item.title,
                    artist = item.artist,
                    artworkUrl = item.artworkUri?.toString(),
                    durationMs = item.duration,
                    isAutoplay = item.isAutoplay,
                    isManual = item.isManual,
                )
            },
            currentIndex = snapshot.currentIndex,
            shuffleEnabled = shuffle,
            repeatMode = repeat.toPlaybackRepeatMode(),
            canSkipNext = canNext,
            canSkipPrevious = canPrevious,
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000L), PlaybackQueueState())

    override fun play(song: SongItem) {
        playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
    }

    override fun playQueue(songs: List<SongItem>, startIndex: Int, title: String?) {
        if (songs.isEmpty()) return
        playQueue(
            ListQueue(
                title = title,
                items = songs.map { it.toMediaItem() },
                startIndex = startIndex.coerceIn(songs.indices),
            ),
        )
    }

    override fun queueSongs(songs: List<SongItem>) {
        if (songs.isNotEmpty()) addToQueue(songs.map { it.toMediaItem() })
    }

    override fun queueNext(songs: List<SongItem>) {
        if (songs.isNotEmpty()) playNext(songs.map { it.toMediaItem() })
    }

    override fun skipToNext() = seekToNext()

    override fun skipToPrevious() = seekToPrevious()

    override fun playQueueItem(queueId: String) {
        queueId.toIntOrNull()?.let { playQueueItem(it) }
    }

    override fun playQueueItemNext(queueId: String) {
        queueId.toIntOrNull()?.let { playQueueItemNext(it) }
    }

    override fun removeQueueItem(queueId: String) {
        queueId.toIntOrNull()?.let { removeQueueItems(listOf(it)) }
    }

    override fun moveQueueItem(queueId: String, afterQueueId: String) {
        val uid = queueId.toIntOrNull() ?: return
        val afterUid = afterQueueId.toIntOrNull() ?: return
        moveQueueItem(uid, afterUid)
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
    }

    override fun setRepeatMode(mode: PlaybackRepeatMode) {
        player.repeatMode = when (mode) {
            PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
            PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    // the service and MainActivity own this connection on android; a shared screen leaving must not tear it down
    override fun release() {}

    fun dispose() {
        try {
            attachedPlayer?.removeListener(this)
            attachedPlayer = null
            scope.cancel()
            Timber.tag(TAG).d("PlayerConnection disposed successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during PlayerConnection disposal")
        }
    }
}

private fun Int.toPlaybackRepeatMode(): PlaybackRepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> PlaybackRepeatMode.ONE
    Player.REPEAT_MODE_ALL -> PlaybackRepeatMode.ALL
    else -> PlaybackRepeatMode.OFF
}

private fun MediaMetadata.toSongItem() = SongItem(
    id = id,
    title = title,
    artists = artists.map { YouTubeArtist(name = it.name, id = it.id) },
    album = album?.let { YouTubeAlbum(name = it.title, id = it.id) },
    duration = duration.takeIf { it >= 0 },
    musicVideoType = musicVideoType,
    thumbnail = thumbnailUrl.orEmpty(),
    explicit = explicit,
    setVideoId = setVideoId,
    libraryAddToken = libraryAddToken,
    libraryRemoveToken = libraryRemoveToken,
)
