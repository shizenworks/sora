package com.mochyy.mochymusic.playback

import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegFallbackRenderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink

// jellyfin's ffmpeg build carries aac but neither opus nor vorbis; opus goes to InProcessOpusRenderer
private val LossyTypes = setOf(MimeTypes.AUDIO_AAC)

/**
 * Decodes youtube's aac inside the app with ffmpeg, so each buffer skips the trip through the
 * phone's codec service. Only takes aac; everything else (flac, alac, video) still goes to the
 * phone's codec first. Lossy audio is always handed to the sink as 16-bit, never float.
 */
@UnstableApi
class InProcessLossyRenderer(
    eventHandler: Handler?,
    eventListener: AudioRendererEventListener?,
    audioSink: AudioSink,
    private val onSource: (Format) -> Unit,
    private val screenOn: () -> Boolean,
    private val wakeCapUs: Long,
) : FfmpegFallbackRenderer(eventHandler, eventListener, audioSink) {

    override fun getName() = "InProcessLossyRenderer"

    override fun supportsFormatInternal(format: Format): Int =
        if (format.sampleMimeType in LossyTypes) super.supportsFormatInternal(format) else C.FORMAT_UNSUPPORTED_TYPE

    override fun onSourceFormat(format: Format) = onSource(format)

    override fun shouldOutputFloat(format: Format): Boolean = false

    // same wake-up cap as the other audio renderers: the position loop wakes every 100 ms only while the screen is on
    override fun getDurationToProgressUs(positionUs: Long, elapsedRealtimeUs: Long): Long {
        val wait = super.getDurationToProgressUs(positionUs, elapsedRealtimeUs)
        return if (screenOn()) minOf(wait, wakeCapUs) else wait
    }
}
