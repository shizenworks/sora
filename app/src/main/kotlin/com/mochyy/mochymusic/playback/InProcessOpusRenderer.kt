package com.mochyy.mochymusic.playback

import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.TraceUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.decoder.CryptoConfig
import androidx.media3.decoder.opus.OpusDecoder
import androidx.media3.decoder.opus.OpusLibrary
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DecoderAudioRenderer
import androidx.media3.extractor.VorbisUtil

private const val BufferCount = 16
private const val DefaultInputBufferSize = 960 * 6
private const val OpusSampleRate = 48_000

/**
 * Media3's libopus renderer rebuilt (the stock one's createDecoder is final): youtube serves opus, and the
 * phone's codec service decodes it out of process with a binder round trip per 20 ms frame. Hands the
 * sink 16-bit pcm like InProcessLossyRenderer, and tells the router the source first.
 */
@UnstableApi
class InProcessOpusRenderer(
    eventHandler: Handler?,
    eventListener: AudioRendererEventListener?,
    audioSink: AudioSink,
    private val onSource: (Format) -> Unit,
    private val screenOn: () -> Boolean,
    private val wakeCapUs: Long,
) : DecoderAudioRenderer<OpusDecoder>(eventHandler, eventListener, audioSink) {

    override fun getName() = "InProcessOpusRenderer"

    override fun supportsFormatInternal(format: Format): Int = when {
        !OpusLibrary.isAvailable() || !MimeTypes.AUDIO_OPUS.equals(format.sampleMimeType, ignoreCase = true) -> C.FORMAT_UNSUPPORTED_TYPE
        !sinkSupportsFormat(Util.getPcmFormat(C.ENCODING_PCM_16BIT, format.channelCount, format.sampleRate)) -> C.FORMAT_UNSUPPORTED_SUBTYPE
        !OpusLibrary.supportsCryptoType(format.cryptoType) -> C.FORMAT_UNSUPPORTED_DRM
        else -> C.FORMAT_HANDLED
    }

    override fun createDecoder(format: Format, cryptoConfig: CryptoConfig?): OpusDecoder {
        onSource(format)
        TraceUtil.beginSection("createOpusDecoder")
        try {
            val inputSize = format.maxInputSize.takeIf { it != Format.NO_VALUE } ?: DefaultInputBufferSize
            return OpusDecoder(BufferCount, BufferCount, inputSize, format.initializationData, cryptoConfig, false)
        } finally {
            TraceUtil.endSection()
        }
    }

    override fun getOutputFormat(decoder: OpusDecoder): Format =
        Util.getPcmFormat(C.ENCODING_PCM_16BIT, decoder.channelCount, OpusSampleRate)

    override fun getChannelMapping(decoder: OpusDecoder): IntArray? =
        VorbisUtil.getVorbisToAndroidChannelLayoutMapping(decoder.channelCount)

    override fun getDurationToProgressUs(positionUs: Long, elapsedRealtimeUs: Long): Long {
        val wait = super.getDurationToProgressUs(positionUs, elapsedRealtimeUs)
        return if (screenOn()) minOf(wait, wakeCapUs) else wait
    }
}
