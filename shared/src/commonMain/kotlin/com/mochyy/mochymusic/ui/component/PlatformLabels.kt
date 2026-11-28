package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable

/** The platform's own "Cancel" and "OK": Android's translated labels, plain English on desktop and iOS for now. */
@Composable
expect fun platformCancelLabel(): String

@Composable
expect fun platformOkLabel(): String

/** A millisecond clock that only moves forward, on the same time base as Compose's frame clock on Android. */
expect fun monotonicMillis(): Long
