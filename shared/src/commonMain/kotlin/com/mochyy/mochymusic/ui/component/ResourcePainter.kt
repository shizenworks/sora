package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.rememberAsyncImagePainter

// a bundled photo or logo decoded by coil off the main thread at the size it's drawn, and kept in memory: painterResource decoded the full picture on the main thread every time its screen composed (119 ms for the discord card on a tab switch)
@Composable
fun rememberResourcePainter(id: Int): Painter = rememberAsyncImagePainter(model = id)

/**
 * An Android drawable id from app callers, drawn the way `painterResource` draws it on Android.
 * Desktop and iOS never pass an app resource id; their callers use a shared drawable instead.
 */
@Composable
expect fun rememberAppIconPainter(id: Int): Painter
