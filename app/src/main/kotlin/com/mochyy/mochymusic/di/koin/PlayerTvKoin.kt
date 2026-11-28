package com.mochyy.mochymusic.di.koin

import com.mochyy.mochymusic.db.InternalDatabase
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.db.MusicDatabase
import com.mochyy.mochymusic.lyrics.LyricsHelper
import com.mochyy.mochymusic.playback.CachedSongs
import com.mochyy.mochymusic.ui.tv.TvSearchViewModel
import com.mochyy.mochymusic.ui.tv.TvSongsViewModel
import com.mochyy.mochymusic.utils.NetworkConnectivityObserver
import com.mochyy.mochymusic.viewmodels.IpodLibraryViewModel
import com.mochyy.mochymusic.viewmodels.LyricsMenuViewModel
import com.mochyy.mochymusic.viewmodels.LyricsScreenViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The player and TV view models while they still live in :app (LyricsMenu, LyricsScreen, the iPod
 * library, the TV songs list and the TV search). They are built by Koin now; the screens ask for
 * them with koinViewModel, as ArtistAlbumsScreen already does for the shared one.
 *
 * Loaded from loadAndroidKoin() in App.onCreate, right after startMochyKoin, so the shared database
 * and store they build on are already in the graph.
 */
val playerTvModule = module {
    // the one Room database App.onCreate hands the shared graph (startMochyKoin); MusicDatabase is
    // only the app-side DAO surface over it, so this wrapper reads and writes that same database
    single { MusicDatabase(get<MochyDatabase>() as InternalDatabase) }

    // @Singleton in Hilt: the lyrics menu is the only collector of its networkStatus
    single { NetworkConnectivityObserver(androidContext()) }

    // unscoped in Hilt too - every view model that asked was given its own instance and its own
    // in-memory lyrics cache; a factory keeps it exactly that way
    factory { LyricsHelper(androidContext(), get()) }

    // the TV songs list reads what the player has in its cache. A second SimpleCache over the same
    // folder is not allowed (Media3 locks it), so this is the instance the player already built and
    // it comes from Hilt until the player itself moves to Koin; then it is a single here.
    single {
        EntryPointAccessors
            .fromApplication(androidContext(), PlayerTvEntryPoint::class.java)
            .cachedSongs()
    }

    viewModelOf(::LyricsMenuViewModel)
    viewModelOf(::LyricsScreenViewModel)
    viewModelOf(::IpodLibraryViewModel)
    viewModelOf(::TvSongsViewModel)
    viewModelOf(::TvSearchViewModel)
}

/** The one piece of the player's graph a Koin-built TV view model needs while Hilt still owns it. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerTvEntryPoint {
    fun cachedSongs(): CachedSongs
}
