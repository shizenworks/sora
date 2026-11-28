package com.mochyy.mochymusic.di

import com.mochyy.mochymusic.api.YouTubeRepository
import com.mochyy.mochymusic.api.platformYouTubeRepository
import com.mochyy.mochymusic.viewmodels.BrowseViewModel
import com.mochyy.mochymusic.viewmodels.ChartsViewModel
import com.mochyy.mochymusic.viewmodels.ExploreViewModel
import com.mochyy.mochymusic.viewmodels.MoodAndGenresViewModel
import com.mochyy.mochymusic.viewmodels.NewReleaseViewModel
import com.mochyy.mochymusic.viewmodels.OnlinePlaylistViewModel
import com.mochyy.mochymusic.viewmodels.YouTubeBrowseViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The online browsing ViewModels that live in :shared. Their YouTube requests go through the
 * platform facade (Android and desktop delegate to innertube's YouTube object; iOS fails cleanly),
 * never through the JVM-only object from commonMain.
 */
val onlineModule = module {
    single<YouTubeRepository> { platformYouTubeRepository() }

    viewModelOf(::OnlinePlaylistViewModel)
    viewModelOf(::ExploreViewModel)
    viewModelOf(::ChartsViewModel)
    viewModelOf(::NewReleaseViewModel)
    viewModelOf(::MoodAndGenresViewModel)
    viewModelOf(::BrowseViewModel)
    viewModelOf(::YouTubeBrowseViewModel)
}
