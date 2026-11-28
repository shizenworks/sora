package com.mochyy.mochymusic.di

import com.mochyy.mochymusic.viewmodels.LibraryImportedViewModel
import com.mochyy.mochymusic.viewmodels.LibraryViewModel
import com.mochyy.mochymusic.viewmodels.LocalSearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The Library view models whose dependencies are already in shared: MochyDatabase's DAOs and the one
 * DataStore. Loaded by startMochyKoin with appModule, so desktop and iOS get them as well as Android.
 * The Library view models that need YouTube, sync, downloads or the import services are bound on
 * Android only, in app/.../di/LibraryKoin.kt.
 */
val libraryModule = module {
    viewModelOf(::LibraryImportedViewModel)
    viewModelOf(::LocalSearchViewModel)
    viewModelOf(::LibraryViewModel)
}
