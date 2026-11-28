// The Android implementations of the shared storage, backup and scrobbling contracts. One module,
// loaded right after the shared one, so the shared view models have their platform side.

package com.mochyy.mochymusic.di.koin

import android.content.Context
import com.mochyy.mochymusic.backup.CloudBackupManager
import com.mochyy.mochymusic.backup.CloudSignIn
import com.mochyy.mochymusic.db.MusicDatabase
import com.mochyy.mochymusic.playback.CachedSongs
import com.mochyy.mochymusic.playback.DownloadExporter
import com.mochyy.mochymusic.playback.DownloadUtil
import com.mochyy.mochymusic.playback.StorageMover
import com.mochyy.mochymusic.importer.transfer.TransferExporter
import com.mochyy.mochymusic.importer.transfer.TransferImporter
import com.mochyy.mochymusic.scrobble.Scrobbler
import com.mochyy.mochymusic.ui.screens.settings.integrations.AndroidScrobblePlatform
import com.mochyy.mochymusic.ui.screens.settings.integrations.backup.AndroidCloudBackupPlatform
import com.mochyy.mochymusic.ui.screens.settings.storage.AndroidStoragePlatform
import com.mochyy.mochymusic.ui.screens.settings.storage.AndroidTransferPlatform
import com.mochyy.mochymusic.viewmodels.CloudBackupPlatform
import com.mochyy.mochymusic.viewmodels.ScrobblePlatform
import com.mochyy.mochymusic.viewmodels.StoragePlatform
import com.mochyy.mochymusic.viewmodels.TransferPlatform
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/** Hilt's singletons, asked for once so Koin can build the platform contracts after start-up. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageEntryPoint {
    fun musicDatabase(): MusicDatabase
    fun cachedSongs(): CachedSongs
    fun downloadUtil(): DownloadUtil
    fun storageMover(): StorageMover
    fun downloadExporter(): DownloadExporter
    fun cloudBackupManager(): CloudBackupManager
    fun cloudSignIn(): CloudSignIn
    fun scrobbler(): Scrobbler
    fun transferExporter(): TransferExporter
    fun transferImporter(): TransferImporter
}

fun loadStorageKoin(context: Context) {
    val entry = EntryPointAccessors.fromApplication(context.applicationContext, StorageEntryPoint::class.java)
    loadKoinModules(storageAndroidModule(context.applicationContext, entry))
}

fun storageAndroidModule(context: Context, entry: StorageEntryPoint) = module {
    single<StoragePlatform> {
        AndroidStoragePlatform(
            context = context,
            database = entry.musicDatabase(),
            cachedSongs = entry.cachedSongs(),
            downloadUtil = entry.downloadUtil(),
            mover = entry.storageMover(),
            exporter = entry.downloadExporter(),
        )
    }
    single<CloudBackupPlatform> {
        AndroidCloudBackupPlatform(context, entry.cloudBackupManager(), entry.cloudSignIn())
    }
    single<ScrobblePlatform> { AndroidScrobblePlatform(entry.scrobbler()) }
    single<TransferPlatform> {
        AndroidTransferPlatform(context, entry.transferExporter(), entry.transferImporter())
    }
}
