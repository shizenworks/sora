// QueuePlayerConnection.kt
// PlaybackConnection for the platforms whose player is a shared PlayerController with a SharedQueue
// behind it (desktop today, iOS the same way): the queue owns Up next and the history, so play next,
// skip and the queue view all go through it instead of being copied into the player.

package com.mochyy.mochymusic.shared.playback

import com.music.innertube.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class QueuePlayerConnection(
    private val player: PlayerController,
    private val queue: SharedQueue,
    private val scope: CoroutineScope,
) : PlaybackConnection {
    private var pendingQueue: Job? = null

    override val state: StateFlow<PlayerState> get() = player.state

    override val nowPlaying: StateFlow<SongItem?> = player.state
        .map { it.song }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), player.state.value.song)

    override val isPlaying: StateFlow<Boolean> = player.state
        .map { it.isPlaying }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), player.state.value.isPlaying)

    override val queueWindow: StateFlow<PlayerQueueWindow> =
        combine(player.state, queue.upcoming) { playback, upcoming ->
            PlayerQueueWindow(current = playback.song, next = upcoming.firstOrNull())
        }.stateIn(
            scope,
            SharingStarted.WhileSubscribed(5_000L),
            PlayerQueueWindow(current = player.state.value.song),
        )

    // the shared queue has no title of its own and one current song: the window is it plus Up next
    override val queueState: StateFlow<PlaybackQueueState> =
        combine(player.state, queue.upcoming, queue.ready) { playback, upcoming, ready ->
            val current = playback.song
            PlaybackQueueState(
                items = (listOfNotNull(current) + upcoming).map { it.asQueueItem() },
                currentIndex = if (current == null) -1 else 0,
                canSkipNext = ready && upcoming.isNotEmpty(),
                canSkipPrevious = current != null,
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000L), PlaybackQueueState())

    override fun play(song: SongItem) {
        pendingQueue?.cancel()
        queue.play(song)
    }

    override fun togglePlayPause() = player.togglePlayPause()

    override fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    override fun skipToNext() = queue.playNext()

    override fun skipToPrevious() = queue.playPrevious()

    override fun playQueue(queue: Queue) {
        pendingQueue?.cancel()
        pendingQueue = scope.launch {
            val status = try {
                queue.getInitialStatus()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@launch
            }
            ensureActive()
            // the chosen song plays now from the queue's start position; the songs after it stay in
            // order as Up next and the ones before it are what Previous walks back through (jarvis on #102)
            this@QueuePlayerConnection.queue.playList(status.items, status.mediaItemIndex, status.position)
        }
    }

    override fun addToQueue(song: SongItem) = queue.add(song)

    override fun playNext(song: SongItem) {
        queue.replace(listOf(song) + queue.upcoming.value.filterNot { it.id == song.id })
    }

    override fun queueSongs(songs: List<SongItem>) = songs.forEach(queue::add)

    override fun queueNext(songs: List<SongItem>) {
        if (songs.isEmpty()) return
        val ids = songs.mapTo(HashSet()) { it.id }
        queue.replace(songs + queue.upcoming.value.filterNot { it.id in ids })
    }

    override fun playQueueItem(queueId: String) {
        queue.upcoming.value.firstOrNull { it.id == queueId }?.let(::play)
    }

    override fun playQueueItemNext(queueId: String) {
        queue.upcoming.value.firstOrNull { it.id == queueId }?.let(::playNext)
    }

    override fun removeQueueItem(queueId: String) {
        queue.upcoming.value.firstOrNull { it.id == queueId }?.let(queue::remove)
    }

    // desktop and iOS stay single-queue and unshuffled for now, as their players already are
    override fun setShuffleEnabled(enabled: Boolean) {}

    override fun setRepeatMode(mode: PlaybackRepeatMode) {}

    // the platform root owns the player and the queue, so a screen leaving must not tear them down
    override fun release() {}
}

private fun SongItem.asQueueItem() = PlaybackQueueItem(
    queueId = id,
    songId = id,
    title = title,
    artist = artists.joinToString { it.name },
    artworkUrl = thumbnail,
    durationMs = (duration ?: 0) * 1000L,
)
