package com.mochyy.mochymusic.di.koin

import android.content.Context
import com.mochyy.mochymusic.importer.MusicImportService
import com.mochyy.mochymusic.importer.YouTubeSyncManager
import com.mochyy.mochymusic.viewmodels.OnboardingViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The two Hilt singletons the onboarding view model needs. Koin can't build them (they take Hilt's
 * database and Context), so Hilt hands them over, the same way the other Android-only bindings do.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface OnboardingEntryPoint {
    fun importService(): MusicImportService
    fun youTubeSync(): YouTubeSyncManager
}

/**
 * Onboarding's Koin binding. The application [Context] is passed in by the app, since Koin has no
 * Android context of its own here; everything else comes from the Hilt entry point above.
 */
fun onboardingModule(application: Context) = module {
    viewModel {
        val entry = EntryPointAccessors.fromApplication(application, OnboardingEntryPoint::class.java)
        OnboardingViewModel(application, entry.importService(), entry.youTubeSync())
    }
}
