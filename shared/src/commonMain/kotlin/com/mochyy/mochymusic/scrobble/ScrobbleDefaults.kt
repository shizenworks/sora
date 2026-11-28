// ScrobbleDefaults.kt
//
// The rule a play must meet before it counts, in one place: the Android ScrobbleTimer enforces it,
// the settings sliders set it, and both live on different sides of the platform line now.

package com.mochyy.mochymusic.scrobble

object ScrobbleDefaults {
    const val DEFAULT_MIN_SONG_SECONDS = 30
    const val DEFAULT_PLAYED_FRACTION = 0.5f
    const val DEFAULT_MAX_WAIT_SECONDS = 240
}
