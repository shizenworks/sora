// DesktopAudioEngine.kt
// Decodes one stream with FFmpeg (through JavaCV, org.bytedeco) and plays the PCM on Java Sound, the
// approach Metrolist's desktop player takes (github.com/MetrolistGroup/Metrolist, GPL-3.0), cut down to
// one track at a time: open, play, pause, seek, close.

package com.mochyy.mochymusic.shared.desktop.playback

import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegLogCallback
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/** What the decoder found in the stream, once it is open. */
data class OpenedStream(
    val codec: String,
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long,
)

/**
 * One open stream. [open] blocks while FFmpeg probes it, so call it off the UI thread. The decode loop runs
 * on its own daemon thread after [start].
 */
class DesktopAudioEngine(
    private val onEnded: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private val lock = java.lang.Object()
    private var grabber: FFmpegFrameGrabber? = null
    private var line: SourceDataLine? = null
    private var thread: Thread? = null

    @Volatile private var paused = false
    @Volatile private var closed = false
    // Guarded by [lock]. Keep the display value until the decoder applies the latest queued seek;
    // otherwise the controller's position poll can briefly move the UI back to the old position.
    private var pendingSeekMs: Long? = null
    private var seekingPositionMs: Long? = null

    // the played position is the line's frame count since the last seek, plus where that seek landed
    @Volatile private var seekBaseMs = 0L
    @Volatile private var seekBaseFrames = 0L
    private var sampleRate = 0

    fun open(url: String, headers: Map<String, String>): OpenedStream {
        FFmpegLogCallback.setLevel(avutil.AV_LOG_ERROR)
        val g = FFmpegFrameGrabber(url).apply {
            // googlevideo drops long-lived connections; FFmpeg reconnects and resumes where it was
            setOption("reconnect", "1")
            setOption("reconnect_streamed", "1")
            setOption("reconnect_delay_max", "5")
            setOption("rw_timeout", "15000000")
            headers.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.let { setOption("user_agent", it.value) }
            headers.filterKeys { !it.equals("User-Agent", ignoreCase = true) }
                .takeIf { it.isNotEmpty() }
                ?.entries?.joinToString(separator = "\r\n", postfix = "\r\n") { "${it.key}: ${it.value}" }
                ?.let { setOption("headers", it) }
            sampleFormat = avutil.AV_SAMPLE_FMT_S16
        }
        var output: SourceDataLine? = null
        try {
            g.start()
            val rate = g.sampleRate.takeIf { it > 0 } ?: 48_000
            val channels = g.audioChannels
            check(channels > 0) { "No audio channel" }
            val format = AudioFormat(rate.toFloat(), 16, channels, true, false)
            val l = AudioSystem.getSourceDataLine(format)
            output = l
            // 250 ms keeps the line responsive while the decoder fetches the next frame.
            l.open(format, rate * channels * 2 / 4)
            val opened = OpenedStream(
                codec = (g.audioCodecName ?: "").uppercase().ifBlank { "AUDIO" },
                sampleRate = rate,
                channels = channels,
                durationMs = (g.lengthInTime / 1000L).coerceAtLeast(0L),
            )
            grabber = g
            line = l
            sampleRate = rate
            return opened
        } catch (error: Throwable) {
            runCatching { output?.close() }
            runCatching { g.release() }
            throw error
        }
    }

    fun start() {
        val worker = Thread(::decodeLoop, "mochy-desktop-audio").apply { isDaemon = true }
        thread = worker
        try {
            worker.start()
        } catch (error: Throwable) {
            thread = null
            throw error
        }
    }

    fun pause() {
        paused = true
        line?.stop()
    }

    fun resume() {
        paused = false
        line?.start()
        synchronized(lock) { lock.notifyAll() }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceAtLeast(0L)
        synchronized(lock) {
            pendingSeekMs = target
            seekingPositionMs = target
            lock.notifyAll()
        }
    }

    val positionMs: Long
        get() {
            synchronized(lock) { seekingPositionMs?.let { return it } }
            val l = line ?: return 0L
            val rate = sampleRate.takeIf { it > 0 } ?: return 0L
            val frames = (l.longFramePosition - seekBaseFrames).coerceAtLeast(0L)
            return seekBaseMs + frames * 1000L / rate
        }

    fun close() {
        closed = true
        synchronized(lock) { lock.notifyAll() }
        // Closing the Java Sound line also unblocks a write or drain in progress.
        runCatching { line?.close() }
        // Once started, the decoder thread alone owns the native grabber.
        if (thread == null) runCatching { grabber?.release() }
    }

    private fun decodeLoop() {
        val g = grabber ?: return
        val l = line ?: return
        try {
            l.start()
            while (!closed) {
                val seek = synchronized(lock) {
                    pendingSeekMs.also { pendingSeekMs = null }
                }
                if (seek != null) {
                    l.flush()
                    g.setTimestamp(seek * 1000L, true)
                    seekBaseFrames = l.longFramePosition
                    seekBaseMs = seek
                    synchronized(lock) {
                        // A newer request may have arrived while FFmpeg was seeking. Leave its
                        // optimistic position in place until that request is applied as well.
                        if (pendingSeekMs == null) seekingPositionMs = null
                    }
                }
                if (paused) {
                    synchronized(lock) { if (paused && !closed && pendingSeekMs == null) lock.wait(200) }
                    continue
                }
                val frame = g.grabSamples() ?: break
                val samples = frame.samples?.firstOrNull() ?: continue
                val bytes = pcmBytes(samples) ?: continue
                l.write(bytes, 0, bytes.size)
            }
            if (!closed) {
                l.drain()
                onEnded()
            }
        } catch (error: Throwable) {
            if (!closed) onError(error)
        } finally {
            runCatching { l.close() }
            runCatching { g.release() }
        }
    }

    private fun pcmBytes(buffer: java.nio.Buffer): ByteArray? = when (buffer) {
        is ShortBuffer -> ByteArray(buffer.remaining() * 2).also {
            ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buffer)
        }
        is ByteBuffer -> ByteArray(buffer.remaining()).also { buffer.get(it) }
        else -> null
    }
}
