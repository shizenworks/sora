package com.mochyy.mochymusic.shared.playback

import com.mochyy.mochymusic.shared.library.LocalLibrary
import com.mochyy.mochymusic.shared.settings.PlaybackPreferences
import com.mochyy.mochymusic.shared.settings.UpcomingQueueRead
import com.mochyy.mochymusic.shared.lyrics.suspendRunCatching
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import okio.IOException

private const val PreloadStartDelayMs = 2_000L
private const val PreviousRestartThresholdMs = 3_000L
private const val StartSeekTimeoutMs = 30_000L

/**
 * Up next and everything that moves through it: restoring it from settings, saving each change, the
 * next song when one ends, Previous and its history, recording plays and fetching the next song
 * ahead. One per app, held by the platform (the iOS runtime, the desktop app) rather than a screen,
 * so CarPlay launched without the phone UI, the system media controls and the UI all use the same
 * queue. [scope] runs on the main thread and lives as long as the app.
 */
internal class SharedQueue(
    private val player: PlayerController,
    private val settings: PlaybackPreferences,
    private val library: LocalLibrary,
    private val transport: QueueTransport,
    private val scope: CoroutineScope,
) {
    private val mutableUpcoming = MutableStateFlow<List<SongItem>>(emptyList())
    private val mutableReady = MutableStateFlow(false)
    private val mutableWriteFailed = MutableStateFlow(false)
    private val writeMutex = Mutex()
    private val history = PlaybackHistory()
    private val jobs = mutableListOf<Job>()
    private var unbind: (() -> Unit)? = null
    private val mutableStorageUnavailable = MutableStateFlow(false)
    private var recent: List<SongItem> = emptyList()
    private var lastRecorded: String? = null
    private var started = false
    private var pendingStartSeek: Job? = null

    /** The songs after the current one, in order. */
    val upcoming: StateFlow<List<SongItem>> = mutableUpcoming.asStateFlow()

    /** False until the saved queue has been read (or found unreadable); changes made before then aren't saved. */
    val ready: StateFlow<Boolean> = mutableReady.asStateFlow()

    /** The saved queue couldn't be read, so changes this session aren't saved. */
    val storageUnavailable: StateFlow<Boolean> = mutableStorageUnavailable.asStateFlow()

    /** The last save of Up next failed. */
    val writeFailed: StateFlow<Boolean> = mutableWriteFailed.asStateFlow()

    /** Starts following the player. Safe to call more than once. */
    fun start() {
        if (started) return
        started = true
        unbind = transport.bind(::playNext, ::playPrevious)

        jobs += scope.launch {
            when (val stored = settings.upcomingQueue.first { it != UpcomingQueueRead.Loading }) {
                is UpcomingQueueRead.Loaded -> {
                    mutableUpcoming.value = stored.songs
                    mutableStorageUnavailable.value = false
                }
                else -> mutableStorageUnavailable.value = true
            }
            mutableReady.value = true
        }
        jobs += scope.launch { library.recent.collect { recent = it } }
        jobs += scope.launch {
            player.state.map { it.song }.distinctUntilChanged { a, b -> a?.id == b?.id }
                .collect { song -> song?.let(history::started) }
        }
        jobs += scope.launch {
            player.state.map { it.song?.id to it.isPlaying }.distinctUntilChanged().collect { (id, playing) ->
                val song = player.state.value.song
                if (id != null && playing && song?.id == id && id != lastRecorded) {
                    suspendRunCatching { library.recordPlay(song) }.onSuccess { lastRecorded = id }
                }
            }
        }
        jobs += scope.launch {
            player.state.map { it.hasEnded }.distinctUntilChanged().collect { ended -> if (ended) playNext() }
        }
        jobs += scope.launch {
            combine(mutableReady, mutableUpcoming) { ready, songs -> ready && songs.isNotEmpty() }
                .distinctUntilChanged()
                .collect(transport::setCanGoNext)
        }
        jobs += scope.launch {
            combine(mutableReady, player.state, mutableUpcoming) { ready, state, songs ->
                PreloadTarget(ready && state.isPlaying && !state.isLoading, songs.firstOrNull())
            }.distinctUntilChanged { a, b -> a.go == b.go && a.next?.id == b.next?.id }.collectLatest { target ->
                // A restored queue stays offline until playback actually gets underway; pauses and queue
                // changes also cancel any pending or completed prefetch before choosing the new head.
                player.preloadNext(null)
                val next = target.next
                if (!target.go || next == null) return@collectLatest
                delay(PreloadStartDelayMs)
                player.preloadNext(next)
            }
        }
    }

    fun close() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        pendingStartSeek?.cancel()
        pendingStartSeek = null
        unbind?.invoke()
        unbind = null
        player.preloadNext(null)
        started = false
    }

    fun replace(next: List<SongItem>) {
        if (next == mutableUpcoming.value) return
        mutableUpcoming.value = next
        if (!mutableReady.value || mutableStorageUnavailable.value) return
        scope.launch {
            writeMutex.withLock {
                mutableWriteFailed.value = try {
                    settings.setUpcomingQueue(next)
                    false
                } catch (_: IOException) {
                    true
                }
            }
        }
    }

    /** Plays [song] now, taking it out of Up next if it was there. */
    fun play(song: SongItem) {
        replace(mutableUpcoming.value.filterNot { it.id == song.id })
        startSong(song)
    }

    /**
     * Plays a whole list the way Android's queue does: [items] at [startIndex] starts now (from
     * [startPositionMs]), everything after it becomes Up next in the same order, and the songs before
     * it are where Previous goes back to.
     */
    fun playList(items: List<SongItem>, startIndex: Int, startPositionMs: Long) {
        if (items.isEmpty()) return
        val start = startIndex.coerceIn(items.indices)
        val first = items[start]
        items.subList(0, start).forEach(history::started)
        replace(items.subList(start + 1, items.size))
        startSong(first)
        if (startPositionMs > 0L) {
            // the platform players open the stream in the background and drop a seek sent before
            // that, so the start position is applied once this song has actually loaded
            pendingStartSeek = scope.launch {
                val loaded = withTimeoutOrNull(StartSeekTimeoutMs) {
                    player.state.first { it.song?.id != first.id || (!it.isLoading && it.error == null && it.durationMs > 0L) }
                }
                if (loaded?.song?.id == first.id) player.seekTo(startPositionMs)
            }
        }
    }

    fun add(song: SongItem) {
        val songs = mutableUpcoming.value
        if (mutableReady.value && songs.none { it.id == song.id }) replace(songs + song)
    }

    fun remove(song: SongItem) {
        replace(mutableUpcoming.value.filterNot { it.id == song.id })
    }

    fun playNext() {
        val upcoming = mutableUpcoming.value
        val next = upcoming.firstOrNull() ?: return
        replace(upcoming.drop(1))
        startSong(next)
    }

    fun playPrevious() {
        val playback = player.state.value
        val current = playback.song ?: return
        if (playback.positionMs > PreviousRestartThresholdMs) {
            player.seekTo(0L)
            return
        }

        // this session's own order first; past its start, Recently Played minus anything this
        // session played, since a song played again jumps to the top of that list
        val previous = history.stepBack(current)
            ?: recent.firstOrNull { it.id != current.id && !history.playedThisSession(it.id) }
                ?.also(history::startedBefore)
        if (previous == null) {
            player.seekTo(0L)
            return
        }
        // the song being left goes back to the front of Up next, so Next returns to it
        replace(listOf(current) + mutableUpcoming.value.filterNot { it.id == current.id || it.id == previous.id })
        startSong(previous)
    }

    private fun startSong(song: SongItem) {
        pendingStartSeek?.cancel()
        pendingStartSeek = null
        player.play(song)
    }

    private class PreloadTarget(val go: Boolean, val next: SongItem?)
}
