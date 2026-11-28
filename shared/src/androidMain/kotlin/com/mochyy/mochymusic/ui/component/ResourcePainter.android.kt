package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

// the toast icons keep the synchronous vector painter they had before the move
@Composable
actual fun rememberAppIconPainter(id: Int): Painter = painterResource(id)
