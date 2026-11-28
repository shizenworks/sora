package com.mochyy.mochymusic.di

import android.app.Application
import android.content.Context
import androidx.media3.datasource.cache.SimpleCache
import com.mochyy.mochymusic.db.MusicDatabase
import com.mochyy.mochymusic.importer.ImportHistory
import com.mochyy.mochymusic.importer.LocalImportService
import com.mochyy.mochymusic.importer.MusicImportService
import com.mochyy.mochymusic.playback.CachedSongs
import com.mochyy.mochymusic.playback.DownloadUtil
import com.mochyy.mochymusic.utils.SyncUtils
import com.mochyy.mochymusic.viewmodels.ArtistSongsViewModel
import com.mochyy.mochymusic.viewmodels.AutoPlaylistViewModel
import com.mochyy.mochymusic.viewmodels.CachePlaylistViewModel
import com.mochyy.mochymusic.viewmodels.HistoryViewModel
import com.mochyy.mochymusic.viewmodels.LibraryAlbumsViewModel
import com.mochyy.mochymusic.viewmodels.LibraryArtistsViewModel
import com.mochyy.mochymusic.viewmodels.LibraryHomeViewModel
import com.mochyy.mochymusic.viewmodels.LibraryImportViewModel
import com.mochyy.mochymusic.viewmodels.LibraryMixViewModel
import com.mochyy.mochymusic.viewmodels.LibraryPlaylistsViewModel
import com.mochyy.mochymusic.viewmodels.LibrarySongsViewModel
import com.mochyy.mochymusic.viewmodels.LocalGenreViewModel
import com.mochyy.mochymusic.viewmodels.LocalGenresViewModel
import com.mochyy.mochymusic.viewmodels.LocalPlaylistViewModel
import com.mochyy.mochymusic.viewmodels.PlaylistsViewModel
import com.mochyy.mochymusic.viewmodels.TopPlaylistViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * The Hilt singletons the Library's Android view models still need. Each one is a singleton in Hilt,
 * so the Koin single below hands out the same instance Hilt would have.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LibraryEntryPoint {
    fun musicDatabase(): MusicDatabase
    fun syncUtils(): SyncUtils
    fun cachedSongs(): CachedSongs
    fun downloadUtil(): DownloadUtil
    fun musicImportService(): MusicImportService
    fun localImportService(): LocalImportService
    fun importHistory(): ImportHistory
    @PlayerCache fun playerCache(): SimpleCache
    @DownloadCache fun downloadCache(): SimpleCache
}

// the two SimpleCache instances are told apart by name, since Koin has no Hilt-style qualifier
private const val PLAYER_CACHE = "playerCache"
private const val DOWNLOAD_CACHE = "downloadCache"

/**
 * The Library view models that need Android: YouTube, sync, downloads, the import services, media3's
 * caches or the app's own helpers. [app] is the Application: it is the Context binding, and it is
 * what EntryPointAccessors reads Hilt's graph from. Loaded from App.onCreate after startMochyKoin,
 * when Hilt is already up.
 */
fun libraryAndroidModule(app: Application): Module {
    val hilt = EntryPointAccessors.fromApplication(app, LibraryEntryPoint::class.java)
    return module {
        single<MusicDatabase> { hilt.musicDatabase() }
        single<SyncUtils> { hilt.syncUtils() }
        single<CachedSongs> { hilt.cachedSongs() }
        single<DownloadUtil> { hilt.downloadUtil() }
        single<MusicImportService> { hilt.musicImportService() }
        single<LocalImportService> { hilt.localImportService() }
        single<ImportHistory> { hilt.importHistory() }
        single<SimpleCache>(named(PLAYER_CACHE)) { hilt.playerCache() }
        single<SimpleCache>(named(DOWNLOAD_CACHE)) { hilt.downloadCache() }

        viewModelOf(::LibraryHomeViewModel)
        viewModelOf(::LibrarySongsViewModel)
        viewModelOf(::LibraryArtistsViewModel)
        viewModelOf(::LibraryAlbumsViewModel)
        viewModelOf(::LibraryPlaylistsViewModel)
        viewModelOf(::LibraryMixViewModel)
        viewModelOf(::ArtistSongsViewModel)
        viewModelOf(::LocalGenresViewModel)
        viewModelOf(::LocalGenreViewModel)
        viewModelOf(::HistoryViewModel)
        viewModelOf(::TopPlaylistViewModel)
        viewModelOf(::AutoPlaylistViewModel)
        viewModel { CachePlaylistViewModel(get(), get(), get(named(PLAYER_CACHE)), get(named(DOWNLOAD_CACHE))) }
        viewModelOf(::LocalPlaylistViewModel)
        viewModelOf(::PlaylistsViewModel)
        viewModelOf(::LibraryImportViewModel)
    }
}
