package com.mochyy.mochymusic.ui.component

import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

internal actual fun CacheDrawScope.likedCoverDrawer(corners: List<Color>): DrawScope.() -> Unit =
    { drawLikedCoverSkia(corners) }
