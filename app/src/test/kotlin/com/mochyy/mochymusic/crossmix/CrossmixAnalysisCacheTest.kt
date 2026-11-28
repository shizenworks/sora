package com.mochyy.mochymusic.crossmix

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ShortBuffer
import kotlin.math.PI
import kotlin.math.sin

class CrossmixAnalysisCacheTest {

    // -- which cached analysis answers which request ------------------------------------------

    @Test
    fun completeAnalysisAnswersBothRoles() {
        assertTrue(usableFor(complete = true, wholeTrack = true))
        assertTrue(usableFor(complete = true, wholeTrack = false))
    }

    @Test
    fun partialAnalysisOnlyAnswersTheNextSong() {
        assertFalse(usableFor(complete = false, wholeTrack = true))
        assertTrue(usableFor(complete = false, wholeTrack = false))
    }

    @Test
    fun completeResultIsNeverOverwrittenWithAPartialOne() {
        assertTrue(shouldReplace(existingComplete = null, newComplete = false))
        assertTrue(shouldReplace(existingComplete = null, newComplete = true))
        assertTrue(shouldReplace(existingComplete = false, newComplete = true))
        assertTrue(shouldReplace(existingComplete = false, newComplete = false))
        assertTrue(shouldReplace(existingComplete = true, newComplete = true))
        assertFalse(shouldReplace(existingComplete = true, newComplete = false))
    }

    // -- the resampler's bulk read gives exactly what the per-sample one did ------------------

    /** the resampler as it was before the bulk copy, reading one sample at a time */
    private class ReferenceResampler {
        private val out = FloatArray(8_192)
        private var count = 0
        private var position = 0.0
        private var previous = 0f

        fun push(pcm: ShortBuffer, channels: Int, inputRate: Int, sink: (FloatArray, Int) -> Unit) {
            val step = inputRate.toDouble() / StreamingTrackAnalyzer.SampleRate
            val frames = pcm.remaining() / channels
            for (frame in 0 until frames) {
                var sum = 0f
                for (c in 0 until channels) sum += pcm.get() / 32_768f
                val current = sum / channels
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

    private fun stereoTone(rate: Int, frames: Int): ShortArray = ShortArray(frames * 2) { i ->
        val frame = i / 2
        val hz = if (i % 2 == 0) 440.0 else 97.0
        (sin(2 * PI * hz * frame / rate) * 20_000).toInt().toShort()
    }

    private fun collect(push: (ShortBuffer, (FloatArray, Int) -> Unit) -> Unit, pcm: ShortArray, blockFrames: Int): FloatArray {
        val result = ArrayList<Float>()
        var offset = 0
        while (offset < pcm.size) {
            val length = minOf(blockFrames * 2, pcm.size - offset)
            push(ShortBuffer.wrap(pcm, offset, length).slice()) { samples, count ->
                for (i in 0 until count) result += samples[i]
            }
            offset += length
        }
        return result.toFloatArray()
    }

    @Test
    fun bulkResamplerMatchesPerSampleOne() {
        for (rate in intArrayOf(48_000, 44_100, 22_050)) {
            val pcm = stereoTone(rate, rate * 3)
            val reference = ReferenceResampler()
            val bulk = MonoResampler()
            // odd block sizes, so the phase carried between calls is exercised
            val expected = collect({ buffer, sink -> reference.push(buffer, 2, rate, sink) }, pcm, 1_021)
            val actual = collect({ buffer, sink -> bulk.push(buffer, 2, rate, sink) }, pcm, 1_021)
            assertArrayEquals("rate $rate", expected, actual, 0f)
        }
    }

    @Test
    fun resamplerOutputsTheAnalyserRate() {
        val rate = 48_000
        val pcm = stereoTone(rate, rate * 2)
        val out = collect({ buffer, sink -> MonoResampler().push(buffer, 2, rate, sink) }, pcm, rate * 2)
        // two seconds in, two seconds of the analyser's rate out (give or take one sample)
        assertEquals(2.0 * StreamingTrackAnalyzer.SampleRate, out.size.toDouble(), 1.0)
    }
}
