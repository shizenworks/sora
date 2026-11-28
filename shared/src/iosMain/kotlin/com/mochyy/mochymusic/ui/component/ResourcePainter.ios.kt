package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

// iOS callers name shared drawables, so an app resource id is only the Android path
@Composable
actual fun rememberAppIconPainter(id: Int): Painter = rememberResourcePainter(id)
