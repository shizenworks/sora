package com.mochyy.mochymusic.viewmodels

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.mochyy.mochymusic.constants.LibraryFilter

// Koin builds it (di/LibraryModule.kt). It only holds which Library tab is open and reads nothing else.
class LibraryViewModel : ViewModel() {
    private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
    val filter: MutableState<LibraryFilter> = curScreen
}
