// The desktop PlayerController: InnerTubeX resolves streams and FFmpeg/Java Sound play them.
package com.mochyy.mochymusic.shared.desktop.playback

import com.mochyy.mochymusic.shared.playback.PlayerController
import com.mochyy.mochymusic.shared.playback.PlayerState
import com.mochyy.mochymusic.shared.playback.StreamFormat
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DesktopPlayerController(
    private val resolver: DesktopStreamResolver = DesktopStreamResolver(),
    private val playbackFailedText: () -> String,
) : PlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(PlayerState())
    override val state = mutableState.asStateFlow()

    private var engine: DesktopAudioEngine? = null
    private var loadJob: Job? = null
    private data class Preload(val songId: String, val resolved: Deferred<ResolvedStream?>, val createdAtNanos: Long)
    private var preload: Preload? = null
    private var activePreload: Preload? = null
    private var released = false
    private var generation = 0L

    /** Resolve one upcoming URL in the background; this deliberately does not open an audio engine. */
    override fun preloadNext(song: SongItem?) {
        if (released) return
        val candidate = song?.takeIf { it.id != state.value.song?.id }
        if (candidate == null) {
            clearPreload()
            return
        }
        if (preload?.songId == candidate.id) return

        clearPreload()
        val deferred = scope.async(Dispatchers.IO) { resolver.resolve(candidate.id) }
        preload = Preload(candidate.id, deferred, System.nanoTime())
    }

    override fun play(song: SongItem) {
        if (released) return
        val request = ++generation
        loadJob?.cancel()
        activePreload?.resolved?.cancel()
        activePreload = null
        retireEngine()
        val queuedPreload = preload?.takeIf { it.songId == song.id }
        val prepared = queuedPreload?.takeIf {
            System.nanoTime() - it.createdAtNanos <= PreloadedStreamMaxAgeNanos
        }
        if (queuedPreload != null) {
            preload = null
            if (prepared == null) queuedPreload.resolved.cancel()
            else activePreload = prepared
        }
        mutableState.value = PlayerState(song = song, isLoading = true, durationMs = (song.duration ?: 0) * 1000L)
        loadJob = scope.launch {
            val next = DesktopAudioEngine(
                onEnded = {
                    scope.launch {
                        if (generation == request) {
                            retireEngine()
                            val current = state.value
                            mutableState.value = current.copy(
                                isPlaying = false, hasEnded = true, positionMs = current.durationMs,
                            )
                        }
                    }
                },
                onError = { scope.launch { if (generation == request) failPlayback() } },
            )
            var adopted = false
            try {
                val stream = checkNotNull(resolveForPlayback(song.id, prepared))
                if (activePreload === prepared) activePreload = null
                val opened = withContext(Dispatchers.IO) { next.open(stream.url, stream.headers) }
                engine = next
                adopted = true
                mutableState.value = state.value.copy(
                    isLoading = false,
                    isPlaying = true,
                    durationMs = opened.durationMs.takeIf { it > 0 } ?: state.value.durationMs,
                    format = StreamFormat(opened.codec, opened.sampleRate, stream.bitrate),
                )
                next.start()
                while (engine === next) {
                    val current = state.value
                    if (current.isPlaying) {
                        val position = next.positionMs
                        mutableState.value = current.copy(
                            positionMs = position.coerceAtMost(current.durationMs.takeIf { it > 0 } ?: position),
                        )
                    }
                    delay(250)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == request) failPlayback()
            } finally {
                if (!adopted) withContext(NonCancellable + Dispatchers.IO) { next.close() }
            }
        }
    }

    override fun togglePlayPause() {
        val current = state.value
        val active = engine
        if (active == null) {
            if (current.hasEnded || current.error != null) current.song?.let(::play)
            return
        }
        if (current.isPlaying) active.pause() else active.resume()
        mutableState.value = current.copy(isPlaying = !current.isPlaying)
    }

    override fun seekTo(positionMs: Long) {
        val active = engine ?: return
        val current = state.value
        val target = positionMs.coerceIn(0L, current.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE)
        active.seekTo(target)
        mutableState.value = current.copy(positionMs = target)
    }

    override fun release() {
        if (released) return
        released = true
        generation++
        loadJob?.cancel()
        loadJob = null
        activePreload?.resolved?.cancel()
        activePreload = null
        clearPreload()
        retireEngine()
        resolver.close()
        scope.cancel()
    }

    private fun failPlayback() {
        retireEngine()
        mutableState.value = state.value.copy(isLoading = false, isPlaying = false, error = playbackFailedText())
    }

    private fun retireEngine() {
        val active = engine ?: return
        engine = null
        scope.launch(NonCancellable + Dispatchers.IO) { active.close() }
    }

    private fun clearPreload() {
        preload?.resolved?.cancel()
        preload = null
    }

    private suspend fun resolveForPlayback(songId: String, prepared: Preload?): ResolvedStream? {
        if (prepared != null) {
            try {
                prepared.resolved.await()?.let { return it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A prefetch is only an optimisation; retry resolution for the track being played.
            }
        }
        return resolver.resolve(songId)
    }

    private companion object {
        const val PreloadedStreamMaxAgeNanos = 30L * 60L * 1_000_000_000L
    }
}
