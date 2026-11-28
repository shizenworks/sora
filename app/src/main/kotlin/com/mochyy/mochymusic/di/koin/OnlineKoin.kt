package com.mochyy.mochymusic.di.koin

import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.db.MusicDatabase
import com.mochyy.mochymusic.ui.screens.search.suggestions.SuggestionsViewModel
import com.mochyy.mochymusic.viewmodels.AlbumViewModel
import com.mochyy.mochymusic.viewmodels.ArtistItemsViewModel
import com.mochyy.mochymusic.viewmodels.ArtistViewModel
import com.mochyy.mochymusic.viewmodels.GenreViewModel
import com.mochyy.mochymusic.viewmodels.OnlineSearchSuggestionViewModel
import com.mochyy.mochymusic.viewmodels.OnlineSearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The online ViewModels that still need Android-only pieces (the blocking MusicDatabase members,
 * app pages/models, the player queue and the Apple scraper), so they stay in :app but get built by
 * Koin like the shared ones. Loaded from loadAndroidKoin.
 */
val onlineAndroidModule = module {
    // the same MusicDatabase instance appModule was started with, through the shared interface
    single<MusicDatabase> { get<MochyDatabase>() as MusicDatabase }

    viewModelOf(::AlbumViewModel)
    viewModelOf(::ArtistViewModel)
    viewModelOf(::ArtistItemsViewModel)
    viewModelOf(::GenreViewModel)
    viewModelOf(::OnlineSearchViewModel)
    viewModelOf(::OnlineSearchSuggestionViewModel)
    viewModelOf(::SuggestionsViewModel)
}
