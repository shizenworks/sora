package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
actual fun platformCancelLabel(): String = stringResource(android.R.string.cancel)

@Composable
actual fun platformOkLabel(): String = stringResource(android.R.string.ok)

actual fun monotonicMillis(): Long = System.nanoTime() / 1_000_000L
