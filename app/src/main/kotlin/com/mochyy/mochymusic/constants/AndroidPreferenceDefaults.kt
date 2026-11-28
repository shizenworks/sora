package com.mochyy.mochymusic.constants

import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneOffset

/** how the passthrough switch reaches the dac; minSdk is the android each one needs */
enum class AudioPassMode(val minSdk: Int) {
    SONG_RATE(34),
    BIT_PERFECT(34),
    DIRECT_USB(24),
    EXCLUSIVE_USB(27),
    EXCLUSIVE_DAC(27),
    ;

    /** whether songs go to an output only mochy plays to, instead of through android's mixer */
    val exclusive: Boolean get() = this != SONG_RATE

    companion object {
        /** android 14 brought the dac's own mixer; before it only mochy's own driver reaches the dac */
        val Default: AudioPassMode
            get() = if (android.os.Build.VERSION.SDK_INT >= 34) SONG_RATE else DIRECT_USB
    }
}


enum class MyTopFilter {
    ALL_TIME,
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ;

    fun toTimeMillis(): Long =
        when (this) {
            DAY ->
                LocalDateTime
                    .now()
                    .minusDays(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            WEEK ->
                LocalDateTime
                    .now()
                    .minusWeeks(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            MONTH ->
                LocalDateTime
                    .now()
                    .minusMonths(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            YEAR ->
                LocalDateTime
                    .now()
                    .minusMonths(12)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            ALL_TIME -> 0
        }
}


/**
 * Cover style to use when the user has not picked one.
 *
 * Edge-to-edge art leans on the backdrop blur behind it to separate the cover from the controls,
 * and that blur needs API 31. Below that the full-bleed art runs straight under the chrome with
 * nothing softening it, so squared reads far better there. This is only the *default* - an
 * explicit choice in Player customise still wins on every device.
 */
val DefaultPlayerCoverStyle: PlayerCoverStyle
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PlayerCoverStyle.EDGE_TO_EDGE
    } else {
        PlayerCoverStyle.SQUARED
    }
