package com.mochyy.mochymusic.ui.player

import com.mochyy.mochymusic.playback.StreamFacts
import com.mochyy.mochymusic.shared.ui.player.formatSampleRateLabel
import com.mochyy.mochymusic.ui.player.info.LosslessCodecs

/**
 * The badge's words for [facts]: nothing until the codec is known. "FLAC · 24-bit · 96,0 KHz" with
 * the bit depth and sample rate switched on; the bit depth only ever names a lossless codec.
 */
fun streamBadgeText(facts: StreamFacts?, showSampleRate: Boolean, showBitDepth: Boolean = false): String? {
    val codec = facts?.codec ?: return null
    val bits = facts.bitDepth?.takeIf { showBitDepth && it > 0 && codec in LosslessCodecs }
    val rate = facts.sampleRate?.takeIf { showSampleRate }?.let(::formatSampleRateLabel)
    if (bits == null && rate == null) return codec
    val parts = mutableListOf(codec)
    bits?.let { parts += "$it-bit" }
    // written as the design writes it, "44,1 KHz", whatever the phone's language
    rate?.let(parts::add)
    return parts.joinToString(" · ")
}
