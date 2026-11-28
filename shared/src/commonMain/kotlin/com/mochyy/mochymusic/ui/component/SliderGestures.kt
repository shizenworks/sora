// SliderGestures.kt
// The one gesture rule every settings slider needs: once a finger that came down on the slider has
// moved at all, the slider's area keeps every further movement to itself. Compose's built-in
// disambiguation gives a mostly-vertical drag to the scrolling column — so a finger adjusting a
// slider that drifted up or down a few pixels used to start the page scrolling mid-drag. Consuming
// the movements here, in the Main pass between the slider and the scroll container, is enough: the
// scrollable's own touch-slop detector only ever sees consumed changes, so it never claims the
// gesture, while the slider underneath has already processed them in the pass before this one.

package com.mochyy.mochymusic.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

/** Blocks the enclosing scroll container from taking over a drag that began on the slider. */
fun Modifier.sliderDragScrollLock(): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var locked = false
        val slop = viewConfiguration.touchSlop
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val pressedChange = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!locked) {
                val moved = event.changes.any { change ->
                    !change.isConsumed && abs(change.positionChange().x) + abs(change.positionChange().y) > slop
                }
                if (moved) locked = true
            }
            // only consume while a finger is down: a tap (down + up, no movement) stays untouched
            // so the slider's own tap-to-seek keeps working
            if (locked && event.changes.any { it.pressed }) {
                event.changes.forEach { it.consume() }
            }
            if (!pressedChange.pressed) break
        }
    }
}
