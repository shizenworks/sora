package com.mochyy.mochymusic.crossmix

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import com.mochyy.mochymusic.playback.audio.StreamPcmDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * analyses tracks for crossmix and keeps them on disk. Every song is decoded whole, once: as the
 * next song its opening is planned around, and a few minutes later it is the playing song whose
 * ending is mixed out of, which needs the whole of it. Analysing only the next song's head (as this
 * used to) meant every song was decoded twice, its first 150 s and then all of it.
 * A failed decode is never cached since an expired stream url may work next time.
 */
@UnstableApi
@OptIn(ExperimentalCoroutinesApi::class)
class CrossmixAnalysisRepository(
    private val context: Context,
    private val dataSourceFactory: () -> DataSource.Factory,
) {
    // bounded memory cache because every analysis is already persisted on disk
    private val memory = android.util.LruCache<String, TrackAnalysis>(16)
    private val failures = ConcurrentHashMap<String, String>()
    private val cacheDir = File(context.cacheDir, "crossmix-analysis-v2").also { it.mkdirs() }

    // one quiet thread: analysis must never compete with decoding the song that's playing. Java's
    // MIN_PRIORITY only sets the nice value; the background scheduling group is what keeps it on
    // the small cores and behind playback and the screen on most phones
    private val dispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(
            {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            },
            "crossmix-analysis",
        ).apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val queue = AnalysisQueue<TrackAnalysis?>(scope)

    // stops the background analysis thread when the service destroys
    fun release() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        dispatcher.close()
        memory.evictAll()
    }

    // keeps a few nearby track analyses hot in ram and drops older ones that are already saved on disk
    fun trimMemory() {
        memory.trimToSize(4)
        failures.clear()
    }

    /**
     * Calls off analyses still waiting or running for songs other than [items] (the playing song and
     * the next one). Skipped songs would otherwise stay queued on the one analysis thread, ahead of the
     * pair that's actually about to mix. Finished results stay cached; an empty list stops everything.
     */
    fun keepOnly(items: Collection<MediaItem>) {
        queue.keepOnly(items.mapNotNullTo(HashSet()) { keyOf(it) })
    }

    private fun keyOf(item: MediaItem): String? {
        val uri = item.localConfiguration?.uri ?: return null
        return item.mediaId.ifBlank { uri.toString() }
    }

    /**
     * wholeTrack: the song that's playing, whose ending we mix out of, needs a complete analysis;
     * the next song makes do with a partial one (an older version's head-only result, or a stream
     * that ended early). Either way a fresh decode reads the whole song, so it never has to be
     * decoded again. Waiting gives up after a while, but the decode itself carries on and is kept.
     */
    suspend fun analyze(item: MediaItem, wholeTrack: Boolean): TrackAnalysis? {
        val uri = item.localConfiguration?.uri ?: return null
        val mediaId = keyOf(item) ?: return null
        val cacheKey = item.localConfiguration?.customCacheKey?.takeIf { it.isNotBlank() } ?: mediaId
        usable(memory.get(mediaId), wholeTrack)?.let { return it }
        // the disk is read off the analysis thread, which may be busy decoding another song
        val stored = withContext(Dispatchers.IO) { read(mediaId) }
        stored?.let { remember(mediaId, it) }
        usable(memory.get(mediaId), wholeTrack)?.let { return it }

        val job = queue.submit(mediaId) {
                val progress = Progress()
                try {
                    withTimeoutOrNull(MaxDecodeMs) { decodeAndAnalyze(uri, cacheKey, progress) }
                        ?.also { analysis ->
                            failures.remove(mediaId)
                            android.util.Log.i(
                                "Crossmix",
                                "analysed $mediaId: ${analysis.analyzedMs / 1_000} s of audio in " +
                                    "${(System.currentTimeMillis() - progress.startedAt) / 1_000} s, " +
                                    "bpm ${analysis.bpm}, ${analysis.beatsMs.size} beats, complete=${analysis.complete}",
                            )
                            if (remember(mediaId, analysis)) write(mediaId, analysis)
                        }
                        ?: null.also {
                            failures[mediaId] = "gave up after ${MaxDecodeMs / 1_000} s with " +
                                "${progress.decodedUs / 1_000_000} s of audio decoded"
                        }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    failures[mediaId] = error.message ?: error.javaClass.simpleName
                    null
                }
        }
        val result = withTimeoutOrNull(WaitMs) { job.await() }
        when {
            result == null && job.isActive ->
                failures[mediaId] = "still analysing after ${WaitMs / 1_000} s; the result is kept for next time"
            wholeTrack && result != null && !result.complete ->
                failures[mediaId] = "the stream ended after ${result.analyzedMs / 1_000} s"
        }
        return usable(result ?: memory.get(mediaId), wholeTrack)
    }

    /** what's already known, without decoding anything */
    fun cached(mediaId: String): TrackAnalysis? = memory.get(mediaId)

    /** why the last analysis of mediaId came back empty, if it did */
    fun lastFailure(mediaId: String): String? = failures[mediaId]

    private class Progress {
        val startedAt = System.currentTimeMillis()
        @Volatile var decodedUs = 0L
    }

    private fun usable(analysis: TrackAnalysis?, wholeTrack: Boolean): TrackAnalysis? =
        analysis?.takeIf { usableFor(it.complete, wholeTrack) }

    /** keeps [analysis] unless a better one is known already; true when it was kept */
    private fun remember(mediaId: String, analysis: TrackAnalysis): Boolean {
        synchronized(memory) {
            if (!shouldReplace(memory.get(mediaId)?.complete, analysis.complete)) return false
            memory.put(mediaId, analysis)
            return true
        }
    }

    private suspend fun decodeAndAnalyze(uri: Uri, mediaId: String, progress: Progress): TrackAnalysis {
        val analyzer = StreamingTrackAnalyzer()
        val resampler = MonoResampler()
        val outcome = StreamPcmDecoder.decode(dataSourceFactory(), uri, mediaId, MaxTrackMs * 1_000L) { pcm, channels, sampleRate, presentationUs ->
            resampler.push(pcm, channels, sampleRate, analyzer::feed)
            progress.decodedUs = presentationUs
        }
        val lastPresentationUs = outcome.lastPresentationUs
        val durationUs = outcome.durationUs
        val wholeStream = outcome.reachedEnd && (durationUs <= 0L || lastPresentationUs >= durationUs - 3_000_000L)
        val complete = wholeStream || lastPresentationUs >= MaxTrackMs * 1_000L
        // a stream that stopped early still says how the song opens: kept as a partial result, which
        // a playing song never settles for, so its whole is tried again next time
        return analyzer.finish(complete = complete)
            ?: error("only ${lastPresentationUs / 1_000_000} s of audio came through")
    }

    private fun read(mediaId: String): TrackAnalysis? = runCatching {
        val file = cacheFile(mediaId)
        if (!file.exists()) return null
        DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
            if (input.readInt() != CacheVersion) return null
            TrackAnalysis(
                analyzedMs = input.readLong(),
                complete = input.readBoolean(),
                bpm = input.readFloat().takeUnless { it.isNaN() },
                bpmConfidence = input.readFloat(),
                beatsMs = input.readLongArray(),
                barBeats = input.readIntArray(),
                sectionBars = input.readIntArray(),
                key = input.readUTF().ifBlank { null },
                keyConfidence = input.readFloat(),
                leadingSilenceEndMs = input.readLong(),
                trailingSilenceStartMs = input.readLong(),
                energy = input.readFloatArray(),
                bass = input.readFloatArray(),
                brightness = input.readFloatArray(),
                vibe = Vibe(
                    energy = input.readFloat(),
                    danceability = input.readFloat(),
                    brightness = input.readFloat(),
                    bassWeight = input.readFloat(),
                    minor = input.readBoolean(),
                ),
                vocal = input.readFloatArray(),
            ).takeIf { analysis -> analysis.barBeats.all { it in analysis.beatsMs.indices } }
        }
    }.getOrNull()

    private fun write(mediaId: String, analysis: TrackAnalysis) = runCatching {
        val target = cacheFile(mediaId)
        val temp = File(target.parentFile, target.name + ".tmp")
        DataOutputStream(BufferedOutputStream(temp.outputStream())).use { out ->
            out.writeInt(CacheVersion)
            out.writeLong(analysis.analyzedMs)
            out.writeBoolean(analysis.complete)
            out.writeFloat(analysis.bpm ?: Float.NaN)
            out.writeFloat(analysis.bpmConfidence)
            out.writeLongArray(analysis.beatsMs)
            out.writeIntArray(analysis.barBeats)
            out.writeIntArray(analysis.sectionBars)
            out.writeUTF(analysis.key.orEmpty())
            out.writeFloat(analysis.keyConfidence)
            out.writeLong(analysis.leadingSilenceEndMs)
            out.writeLong(analysis.trailingSilenceStartMs)
            out.writeFloatArray(analysis.energy)
            out.writeFloatArray(analysis.bass)
            out.writeFloatArray(analysis.brightness)
            out.writeFloat(analysis.vibe.energy)
            out.writeFloat(analysis.vibe.danceability)
            out.writeFloat(analysis.vibe.brightness)
            out.writeFloat(analysis.vibe.bassWeight)
            out.writeBoolean(analysis.vibe.minor)
            out.writeFloatArray(analysis.vocal)
        }
        // written whole, then swapped in: a crash mid-write can't leave a half file to misread
        temp.renameTo(target)
    }

    private fun cacheFile(mediaId: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(mediaId.toByteArray())
        return File(cacheDir, digest.joinToString("") { "%02x".format(it) })
    }

    private fun DataInputStream.readLongArray(): LongArray = LongArray(readInt().coerceIn(0, MaxCachedArray)) { readLong() }
    private fun DataInputStream.readFloatArray(): FloatArray = FloatArray(readInt().coerceIn(0, MaxCachedArray)) { readFloat() }
    private fun DataInputStream.readIntArray(): IntArray = IntArray(readInt().coerceIn(0, MaxCachedArray)) { readInt() }

    private fun DataOutputStream.writeLongArray(values: LongArray) {
        writeInt(values.size)
        values.forEach(::writeLong)
    }

    private fun DataOutputStream.writeFloatArray(values: FloatArray) {
        writeInt(values.size)
        values.forEach(::writeFloat)
    }

    private fun DataOutputStream.writeIntArray(values: IntArray) {
        writeInt(values.size)
        values.forEach(::writeInt)
    }

    private companion object {
        // 3: analyses cut short by a one-chunk stream were stored as whole ones. 4: the voice curve. 5: bars from chords and drums, and section starts
        const val CacheVersion = 5
        const val MaxCachedArray = 200_000
        const val MaxTrackMs = 15 * 60_000L
        /** how long the planner waits for an analysis (the playing song's, then the next one's) */
        const val WaitMs = 60_000L
        /** a decode that hasn't finished by then is stuck (a stalled stream): the thread is freed for the next song */
        const val MaxDecodeMs = 4 * 60_000L
    }
}

/**
 * Work on the one analysis thread, one job per song: asking again for a song already queued or running
 * joins that job, and [keepOnly] calls off the rest. A job called off before it started never runs its
 * own cleanup, so the queue drops it itself and a later request starts afresh.
 */
internal class AnalysisQueue<V>(private val scope: CoroutineScope) {
    private val jobs = ConcurrentHashMap<String, Deferred<V>>()

    fun submit(key: String, block: suspend () -> V): Deferred<V> {
        // in the map before it starts, so a quick finish can't remove itself first and leave a stale entry
        val job = jobs.compute(key) { _, existing ->
            existing?.takeIf { !it.isCancelled } ?: scope.async(start = CoroutineStart.LAZY) { block() }
        }!!
        job.invokeOnCompletion { jobs.remove(key, job) }
        job.start()
        return job
    }

    fun keepOnly(keys: Set<String>) {
        for ((key, job) in jobs) {
            if (key !in keys) {
                job.cancel()
                jobs.remove(key, job)
            }
        }
    }

    fun pending(): Set<String> = jobs.keys.toSet()
}

/** a complete analysis answers every request, a partial one only the next song's */
internal fun usableFor(complete: Boolean, wholeTrack: Boolean): Boolean = complete || !wholeTrack

/** a new result replaces what's known unless that's complete and the new one isn't: a whole-track result is never overwritten with less */
internal fun shouldReplace(existingComplete: Boolean?, newComplete: Boolean): Boolean =
    existingComplete == null || newComplete || !existingComplete

/** downmixes pcm 16 to mono and resamples it to the analyser's rate, carrying phase between calls */
internal class MonoResampler {
    private val out = FloatArray(8_192)
    private var input = ShortArray(0)
    private var count = 0
    private var position = 0.0
    private var previous = 0f

    fun push(pcm: java.nio.ShortBuffer, channels: Int, inputRate: Int, sink: (FloatArray, Int) -> Unit) {
        val step = inputRate.toDouble() / StreamingTrackAnalyzer.SampleRate
        val frames = pcm.remaining() / channels
        // one bulk copy out of the decoder's buffer: reading it a sample at a time cost a call per sample
        val samples = frames * channels
        if (input.size < samples) input = ShortArray(samples)
        pcm.get(input, 0, samples)
        var read = 0
        for (frame in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) sum += input[read++] / 32_768f
            val current = sum / channels
            // emit every output sample that falls between the previous input sample and this one
            while (position <= 1.0) {
                out[count++] = previous + (current - previous) * position.toFloat()
                if (count == out.size) { sink(out, count); count = 0 }
                position += step
            }
            position -= 1.0
            previous = current
        }
        if (count > 0) { sink(out, count); count = 0 }
    }
}
