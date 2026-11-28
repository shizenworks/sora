package com.mochyy.mochymusic.ui.screens.settings.devspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.rememberResourcePainter
import com.mochyy.mochymusic.ui.utils.SoftBlurTransformation

/**
 * The dev spaces photos as Android draws them: the two files are app drawables (drawable-nodpi, which
 * the ResourceBridge doesn't copy), so the same Coil requests as before the screens moved stay here —
 * the round avatar from [rememberResourcePainter], and for the banner a 192px blurred copy made once
 * by [SoftBlurTransformation] off the main thread.
 */
object AppDevPictures : DevPictures {
    @Composable
    override fun avatar(picture: DevPicture): Painter = rememberResourcePainter(picture.drawable)

    @Composable
    override fun banner(picture: DevPicture): Painter {
        val context = LocalContext.current
        // blurred once, small and off the main thread, so it costs the same on every phone
        val request = remember(picture) {
            ImageRequest.Builder(context)
                .data(picture.drawable)
                .size(192)
                .transformations(SoftBlurTransformation())
                .build()
        }
        return rememberAsyncImagePainter(model = request)
    }
}

private val DevPicture.drawable: Int
    get() = when (this) {
        DevPicture.Idkroo -> R.drawable.frame_51_3
        DevPicture.Keycii -> R.drawable.dev_keycii
    }
