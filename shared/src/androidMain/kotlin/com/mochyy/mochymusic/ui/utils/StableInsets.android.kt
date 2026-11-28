package com.mochyy.mochymusic.ui.utils

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal actual fun platformStableSystemBars(): WindowInsets = WindowInsets.systemBarsIgnoringVisibility

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal actual fun platformStableStatusBars(): WindowInsets = WindowInsets.statusBarsIgnoringVisibility
