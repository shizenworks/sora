package com.mochyy.mochymusic.viewmodels

import com.mochyy.mochymusic.db.entities.Song
import kotlinx.coroutines.flow.StateFlow

// The storage page's numbers and jobs are all Android's: media3's downloads, the ExoPlayer cache, SAF
// folders and the app's own files. The shared page and view model talk to this contract; the Android
// app fills it from those services, and a platform without downloads simply never shows the page.

/** the two big folders mochy can keep on another drive */
enum class StorageKind { Downloads, SongCache }

/** one drive mochy keeps things on, for its own overall card */
data class DriveUsage(
    val label: String,
    val isPhone: Boolean,
    val total: Long,
    val free: Long,
    /** what mochy has on this drive */
    val mochy: Long,
) {
    val othersUsed: Long get() = (total - free - mochy).coerceAtLeast(0L)
}

/** How much room Mochy takes, in the four parts the Storage page shows, and every drive it sits on. */
data class StorageUsage(
    /** the phone first, then any memory card holding downloads or the cache */
    val drives: List<DriveUsage>,
    val cache: Long,
    val downloads: Long,
    val database: Long,
    val localCovers: Long,
    val appData: Long,
) {
    val mochy: Long get() = cache + downloads + database + localCovers + appData
    val phone: DriveUsage get() = drives.first()
}

/** A finished download, with what it costs on the phone. */
data class DownloadedSong(
    val song: Song,
    val bytes: Long,
    /** when the download finished, ms since 1970 */
    val downloadedAt: Long,
)

/** a folder mochy can save into, as the shared page names it; the platform keeps the real handle */
data class StoragePlaceInfo(
    val root: String,
    val label: String,
    val isPhone: Boolean,
    val total: Long,
    val free: Long,
    val isFolder: Boolean = false,
)

/** the drives one kind of folder could go to, where it is now, and how much a move would copy */
data class PlaceChoice(
    val places: List<StoragePlaceInfo>,
    val current: StoragePlaceInfo,
    val bytesToMove: Long,
)

/** how a move is going, for the storage page and the place sheet */
sealed interface MoveStep {
    data object Idle : MoveStep
    data class Working(val kind: StorageKind, val progress: Float) : MoveStep
    data class Failed(val kind: StorageKind, val message: String) : MoveStep
}

/** how the exported songs are sorted into folders */
enum class ExportLayout { Playlists, Albums }

data class ExportChoice(val layout: ExportLayout, val covers: Boolean, val lyrics: Boolean)

sealed interface ExportStep {
    data object Idle : ExportStep
    data class Working(val done: Int, val total: Int) : ExportStep
    data class Done(val songs: Int, val skipped: Int) : ExportStep
    data class Failed(val message: String) : ExportStep
}

/** room the Discord canvas gifs take, measured again after a clear; shared so the page needn't know the store */
data class CanvasGifStats(val bytes: Long, val count: Int)

interface StoragePlatform {
    val moveStep: StateFlow<MoveStep>
    val exportStep: StateFlow<ExportStep>
    val downloads: StateFlow<List<DownloadedSong>?>

    suspend fun measure(): StorageUsage
    suspend fun clearCaches()
    suspend fun clearLocalCovers()
    suspend fun removeAllDownloads()
    suspend fun removeDownloads(songIds: Collection<String>)

    suspend fun placeChoice(kind: StorageKind): PlaceChoice
    /** a folder picked with the system picker, as the place mochy would save into; null when it can't be used */
    suspend fun pickedFolder(tree: String): StoragePlaceInfo?
    fun needsFolderAccess(place: StoragePlaceInfo): Boolean
    fun move(kind: StorageKind, to: StoragePlaceInfo)
    fun clearMoveFailure()

    fun export(tree: String, choice: ExportChoice)
    fun clearExport()

    suspend fun canvasGifStats(): CanvasGifStats
    suspend fun clearCanvasGifs()
}
