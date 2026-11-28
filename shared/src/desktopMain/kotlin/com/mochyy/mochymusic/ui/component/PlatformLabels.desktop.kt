package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable

@Composable
actual fun platformCancelLabel(): String = "Cancel"

@Composable
actual fun platformOkLabel(): String = "OK"

actual fun monotonicMillis(): Long = System.nanoTime() / 1_000_000L
