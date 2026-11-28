// Android’s side of the shared storage page: media3 downloads, the ExoPlayer cache, SAF folders and the app's files.

package com.mochyy.mochymusic.ui.screens.settings.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import coil3.SingletonImageLoader
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.db.MusicDatabase
import com.mochyy.mochymusic.playback.CachedSongs
import com.mochyy.mochymusic.playback.DownloadExporter
import com.mochyy.mochymusic.playback.DownloadUtil
import com.mochyy.mochymusic.playback.ExoDownloadService
import com.mochyy.mochymusic.playback.ExportChoice as AppExportChoice
import com.mochyy.mochymusic.playback.ExportLayout as AppExportLayout
import com.mochyy.mochymusic.playback.ExportStep as AppExportStep
import com.mochyy.mochymusic.playback.MoveStep as AppMoveStep
import com.mochyy.mochymusic.playback.StorageMover
import com.mochyy.mochymusic.playback.StoragePlace
import com.mochyy.mochymusic.playback.StoragePlaces
import com.mochyy.mochymusic.playback.StoredKind
import com.mochyy.mochymusic.viewmodels.CanvasGifStats
import com.mochyy.mochymusic.viewmodels.DownloadedSong
import com.mochyy.mochymusic.viewmodels.DriveUsage
import com.mochyy.mochymusic.viewmodels.ExportChoice
import com.mochyy.mochymusic.viewmodels.ExportLayout
import com.mochyy.mochymusic.viewmodels.ExportStep
import com.mochyy.mochymusic.viewmodels.MoveStep
import com.mochyy.mochymusic.viewmodels.PlaceChoice
import com.mochyy.mochymusic.viewmodels.StorageKind
import com.mochyy.mochymusic.viewmodels.StoragePlaceInfo
import com.mochyy.mochymusic.viewmodels.StoragePlatform
import com.mochyy.mochymusic.viewmodels.StorageUsage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File

/** the app's folders, downloads and caches, behind the shared storage page's contract */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidStoragePlatform(
    private val context: Context,
    private val database: MusicDatabase,
    private val cachedSongs: CachedSongs,
    private val downloadUtil: DownloadUtil,
    private val mover: StorageMover,
    private val exporter: DownloadExporter,
) : StoragePlatform {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // the folder the user just picked, kept so the move uses the exact place the sheet measured
    private var lastPicked: StoragePlace? = null

    override val moveStep: StateFlow<MoveStep> = mover.step
        .map { step ->
            when (step) {
                is AppMoveStep.Idle -> MoveStep.Idle
                is AppMoveStep.Working -> MoveStep.Working(step.kind.toShared(), step.progress)
                is AppMoveStep.Failed -> MoveStep.Failed(step.kind.toShared(), step.message)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, MoveStep.Idle)

    override val exportStep: StateFlow<ExportStep> = exporter.step
        .map { step ->
            when (step) {
                is AppExportStep.Idle -> ExportStep.Idle
                is AppExportStep.Working -> ExportStep.Working(step.done, step.total)
                is AppExportStep.Done -> ExportStep.Done(step.songs, step.skipped)
                is AppExportStep.Failed -> ExportStep.Failed(step.message)
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, ExportStep.Idle)

    override val downloads: StateFlow<List<DownloadedSong>?> = downloadUtil.downloads
        .map { all -> all.values.filter { it.state == Download.STATE_COMPLETED } }
        .distinctUntilChanged { old, new -> old.map { it.request.id } == new.map { it.request.id } }
        .mapLatest { done ->
            val byId = done.associateBy { it.request.id }
            val songs = if (byId.isEmpty()) emptyList() else database.getSongsByIds(byId.keys.toList())
            songs.mapNotNull { song ->
                val download = byId[song.id] ?: return@mapNotNull null
                DownloadedSong(song, download.bytesDownloaded.coerceAtLeast(0L), download.updateTimeMs)
            }.sortedByDescending { it.downloadedAt }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    override suspend fun measure(): StorageUsage = withContext(Dispatchers.IO) { readUsage() }

    override suspend fun clearCaches() {
        cachedSongs.clear().join()
        withContext(Dispatchers.IO) { com.mochyy.mochymusic.discord.CanvasGifStore.clear(context) }
        SingletonImageLoader.get(context).let { loader ->
            loader.memoryCache?.clear()
            withContext(Dispatchers.IO) { loader.diskCache?.clear() }
        }
    }

    override suspend fun clearLocalCovers() {
        com.mochyy.mochymusic.importer.LocalArtwork.clearAll(context, database)
    }

    override suspend fun removeAllDownloads() {
        DownloadService.sendRemoveAllDownloads(context, ExoDownloadService::class.java, false)
    }

    override suspend fun removeDownloads(songIds: Collection<String>) {
        songIds.forEach { DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, it, false) }
    }

    override suspend fun placeChoice(kind: StorageKind): PlaceChoice = withContext(Dispatchers.IO) {
        val appKind = kind.toApp()
        PlaceChoice(
            places = StoragePlaces.places(context).map { it.toInfo() },
            current = StoragePlaces.placeOf(context, appKind).toInfo(),
            bytesToMove = mover.bytesToMove(appKind),
        )
    }

    override suspend fun pickedFolder(tree: String): StoragePlaceInfo? = withContext(Dispatchers.IO) {
        val place = StoragePlaces.folderFromTree(context, Uri.parse(tree))
            ?.let { StoragePlaces.folderPlace(context, StoragePlaces.rootForPicked(it)) }
        lastPicked = place
        place?.toInfo()
    }

    override fun needsFolderAccess(place: StoragePlaceInfo): Boolean =
        StoragePlaces.needsFolderAccess(context, File(place.root))

    override fun move(kind: StorageKind, to: StoragePlaceInfo) {
        val place = lastPicked?.takeIf { it.root.path == to.root }
            ?: StoragePlaces.places(context).firstOrNull { it.root.path == to.root }
            ?: StoragePlace(File(to.root), to.label, to.isPhone, to.total, to.free, to.isFolder)
        mover.move(kind.toApp(), place)
    }

    override fun clearMoveFailure() = mover.clearFailure()

    override fun export(tree: String, choice: ExportChoice) {
        exporter.start(
            Uri.parse(tree),
            AppExportChoice(choice.layout.toApp(), choice.covers, choice.lyrics),
        )
    }

    override fun clearExport() = exporter.clear()

    override suspend fun canvasGifStats(): CanvasGifStats = withContext(Dispatchers.IO) {
        com.mochyy.mochymusic.discord.CanvasGifStore.stats(context).let { CanvasGifStats(it.bytes, it.count) }
    }

    override suspend fun clearCanvasGifs() = withContext(Dispatchers.IO) {
        com.mochyy.mochymusic.discord.CanvasGifStore.clear(context)
    }

    private suspend fun readUsage(): StorageUsage {
        val stat = StatFs(Environment.getDataDirectory().path)
        val dataDir = context.dataDir
        val songCacheDir = StoragePlaces.dir(context, StoredKind.SongCache)
        val downloadDir = StoragePlaces.dir(context, StoredKind.Downloads)
        val databases = context.getDatabasePath("x").parentFile

        val songCache = sizeOf(songCacheDir)
        val downloadedSongs = sizeOf(downloadDir)
        val cache = songCache + sizeOf(context.cacheDir) + sizeOf(context.codeCacheDir)
        val downloads = downloadedSongs
        val db = sizeOf(databases)
        val localCovers = com.mochyy.mochymusic.importer.LocalArtwork.sizeOnDisk(context)
        // only what sits in the data folder comes off it before the rest is counted, a folder on a card was never in it
        val onCards = listOf(songCacheDir to songCache, downloadDir to downloadedSongs).filter { (dir, _) -> dir.parentFile != context.filesDir }
        val rest = (sizeOf(dataDir) - (cache + downloads - onCards.sumOf { it.second }) - db - localCovers).coerceAtLeast(0L)
        val apk = context.applicationInfo.run {
            (listOf(sourceDir) + splitSourceDirs.orEmpty()).sumOf { File(it).length() }
        }
        val total = cache + downloads + db + localCovers + rest + apk
        val phone = DriveUsage(
            label = context.getString(R.string.storage_place_phone),
            isPhone = true,
            total = stat.totalBytes,
            free = stat.availableBytes,
            mochy = total - onCards.sumOf { it.second },
        )
        // a card holding both downloads and the cache gets one card on the page, not two
        val cards = StoredKind.entries
            .map { StoragePlaces.placeOf(context, it) }
            .filter { !it.isPhone }
            .distinctBy { it.root }
            .map { place ->
                DriveUsage(
                    label = place.label,
                    isPhone = false,
                    total = place.total,
                    free = place.free,
                    mochy = onCards.filter { (dir, _) -> dir.parentFile == place.root }.sumOf { it.second },
                )
            }
        return StorageUsage(
            drives = listOf(phone) + cards,
            cache = cache,
            downloads = downloads,
            database = db,
            localCovers = localCovers,
            appData = rest + apk,
        )
    }

    private fun sizeOf(file: File?): Long {
        if (file == null || !file.exists()) return 0L
        // a cover shared by a whole album is one file under several names, and only takes its room once
        val counted = HashSet<Long>()
        return runCatching {
            file.walkTopDown().filter { it.isFile }.sumOf { entry ->
                val stat = runCatching { android.system.Os.lstat(entry.path) }.getOrNull()
                when {
                    stat == null -> entry.length()
                    stat.st_nlink > 1 && !counted.add(stat.st_ino) -> 0L
                    else -> stat.st_size
                }
            }
        }.getOrDefault(0L)
    }

    private fun StoragePlace.toInfo() = StoragePlaceInfo(
        root = root.path,
        label = label,
        isPhone = isPhone,
        total = total,
        free = free,
        isFolder = isFolder,
    )

    private fun StoredKind.toShared(): StorageKind = when (this) {
        StoredKind.Downloads -> StorageKind.Downloads
        StoredKind.SongCache -> StorageKind.SongCache
    }

    private fun StorageKind.toApp(): StoredKind = when (this) {
        StorageKind.Downloads -> StoredKind.Downloads
        StorageKind.SongCache -> StoredKind.SongCache
    }

    private fun ExportLayout.toApp(): AppExportLayout = when (this) {
        ExportLayout.Playlists -> AppExportLayout.Playlists
        ExportLayout.Albums -> AppExportLayout.Albums
    }
}
