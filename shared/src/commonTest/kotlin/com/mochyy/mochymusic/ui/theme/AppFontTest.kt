package com.mochyy.mochymusic.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppFontTest {
    @Test
    fun lookWinsOverEverything() {
        assertEquals("look", pickAppFont("look", "custom", useSystem = true, system = "system", inter = "inter"))
    }

    @Test
    fun customWinsOverSystemAndInter() {
        assertEquals("custom", pickAppFont(null, "custom", useSystem = true, system = "system", inter = "inter"))
        assertEquals("custom", pickAppFont(null, "custom", useSystem = false, system = "system", inter = "inter"))
    }

    @Test
    fun systemThenInter() {
        assertEquals("system", pickAppFont(null, null, useSystem = true, system = "system", inter = "inter"))
        assertEquals("inter", pickAppFont<String>(null, null, useSystem = false, system = "system", inter = "inter"))
    }

    @Test
    fun sizeLimitIsTwentyMegabytes() {
        assertTrue(customFontSizeAllowed(MaxCustomFontBytes))
        assertFalse(customFontSizeAllowed(MaxCustomFontBytes + 1))
        assertTrue(customFontSizeAllowed(0))
        // an unknown size is checked while the file copies
        assertTrue(customFontSizeAllowed(null))
        assertTrue(customFontSizeAllowed(-1))
    }
}
