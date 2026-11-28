package com.mochyy.mochymusic.ui.component

import androidx.compose.runtime.Composable
import platform.Foundation.NSProcessInfo

@Composable
actual fun platformCancelLabel(): String = "Cancel"

@Composable
actual fun platformOkLabel(): String = "OK"

actual fun monotonicMillis(): Long = (NSProcessInfo.processInfo.systemUptime * 1000.0).toLong()
