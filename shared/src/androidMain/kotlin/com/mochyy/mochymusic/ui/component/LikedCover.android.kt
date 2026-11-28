package com.mochyy.mochymusic.ui.component

import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

internal actual fun CacheDrawScope.likedCoverDrawer(corners: List<Color>): DrawScope.() -> Unit {
    val paints = LikedCoverPaints(corners, size.width, size.height)
    return { drawIntoCanvas { canvas -> paints.draw(canvas.nativeCanvas, size.width, size.height) } }
}

// the cover's gradients for one size and set of colours, so the live cover and its saved picture are drawn the same
class LikedCoverPaints(corners: List<Color>, private val width: Float, private val height: Float) {
    // the four averaged, under everything, so no corner of it is ever left bare
    private val base = Paint().apply {
        color = likedCoverBase(corners).toArgb()
    }

    // dithered: a soft blend this wide bands on 8-bit screens otherwise
    private val corners = corners.mapIndexed { i, c ->
        Paint().apply {
            isDither = true
            shader = RadialGradient(
                width * LikedCoverAnchors[i].x,
                height * LikedCoverAnchors[i].y,
                maxOf(width, height) * 0.9f,
                intArrayOf(c.toArgb(), c.copy(alpha = 0.72f).toArgb(), c.copy(alpha = 0f).toArgb()),
                floatArrayOf(0f, 0.36f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
    }

    // a little darker towards the edges, which gives it depth and keeps the heart on top
    private val shade = Paint().apply {
        isDither = true
        shader = RadialGradient(
            width / 2f,
            height / 2f,
            maxOf(width, height) * 0.78f,
            intArrayOf(0, 0, Color.Black.copy(alpha = 0.22f).toArgb()),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    fun draw(canvas: android.graphics.Canvas, width: Float, height: Float) {
        // a rect, not drawColor: that fills the whole clip, and on the liked page nothing clips the cover, so it greyed the screen under the songs
        canvas.drawRect(0f, 0f, width, height, base)
        corners.forEach { canvas.drawRect(0f, 0f, width, height, it) }
        canvas.drawRect(0f, 0f, width, height, shade)
    }
}
