package com.mochyy.mochymusic.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mochyy.mochymusic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// liked songs' cover saved as a picture, for the places that only take an image link (home's cards,
// their colours, lists). The drawing itself is LikedCoverPaints, shared with the live cover; only the
// Android file encoding lives here.
object LikedCoverImage {
    private const val Folder = "liked_cover"
    private const val Size = 512
    private const val HeartShare = 0.4f

    // the picture for the songs liked last, drawn once per set of colours and reused; null if it couldn't be written
    suspend fun url(context: android.content.Context, likedCovers: List<String>): String? {
        val colors = LikedCoverColors.of(context, likedCovers)
        val key = colors.joinToString("-") { Integer.toHexString(it.toArgb()) }
        return withContext(Dispatchers.IO) {
            val folder = java.io.File(context.filesDir, Folder).apply { mkdirs() }
            val file = java.io.File(folder, "liked.$key.webp")
            if (!file.exists()) {
                val written = runCatching { write(context, colors, file) }.isSuccess
                if (!written) return@withContext null
                // the colours moved on, so the older pictures are no use
                folder.listFiles()?.forEach { if (it != file) it.delete() }
            }
            android.net.Uri.fromFile(file).toString()
        }
    }

    private fun write(context: android.content.Context, colors: List<Color>, file: java.io.File) {
        val bitmap = android.graphics.Bitmap.createBitmap(Size, Size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        LikedCoverPaints(colors, Size.toFloat(), Size.toFloat()).draw(canvas, Size.toFloat(), Size.toFloat())
        androidx.core.content.ContextCompat.getDrawable(context, R.drawable.favorite_border)?.mutate()?.let { heart ->
            val side = (Size * HeartShare).toInt()
            val start = (Size - side) / 2
            heart.setBounds(start, start, start + side, start + side)
            heart.setTint(android.graphics.Color.WHITE)
            heart.draw(canvas)
        }
        @Suppress("DEPRECATION")
        val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.graphics.Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            android.graphics.Bitmap.CompressFormat.WEBP
        }
        // written beside it first, so a cut-short write never leaves a broken picture in its place
        val partial = java.io.File(file.parentFile, file.name + ".part")
        partial.outputStream().buffered().use { bitmap.compress(format, 92, it) }
        bitmap.recycle()
        check(partial.renameTo(file)) { "couldn't put the liked cover in place" }
    }
}
