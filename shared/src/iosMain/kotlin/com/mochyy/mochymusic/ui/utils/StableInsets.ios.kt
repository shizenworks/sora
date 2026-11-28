package com.mochyy.mochymusic.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable

@Composable
internal actual fun platformStableSystemBars(): WindowInsets = WindowInsets.systemBars

@Composable
internal actual fun platformStableStatusBars(): WindowInsets = WindowInsets.statusBars
