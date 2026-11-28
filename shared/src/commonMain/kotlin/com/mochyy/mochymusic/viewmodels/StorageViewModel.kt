// StorageViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.utils.ioDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The settings → storage page. Everything it measures and deletes belongs to the platform (downloads,
 * the ExoPlayer cache, SAF folders), so the work goes through [StoragePlatform] and this class only
 * holds the state the page reads; Android fills the contract from its existing services, unchanged.
 */
class StorageViewModel(
    private val platform: StoragePlatform,
) : ViewModel() {

    val moveStep: StateFlow<MoveStep> = platform.moveStep
    val exportStep: StateFlow<ExportStep> = platform.exportStep

    private val _usage = MutableStateFlow<StorageUsage?>(null)
    val usage: StateFlow<StorageUsage?> = _usage.asStateFlow()

    /** True while something is being deleted, so the buttons can't be tapped twice. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Every finished download, newest first; the sheet sorts it however it likes. */
    val downloads: StateFlow<List<DownloadedSong>?> = platform.downloads

    /** Room the Discord canvas gifs take; null until the first measurement. */
    private val _canvasGifs = MutableStateFlow<CanvasGifStats?>(null)
    val canvasGifs: StateFlow<CanvasGifStats?> = _canvasGifs.asStateFlow()

    /** Walks Mochy's folders again. The page calls it on open and whenever the downloads change. */
    fun measure() {
        viewModelScope.launch { _usage.value = withContext(ioDispatcher) { platform.measure() } }
    }

    fun refreshCanvasGifs() {
        viewModelScope.launch { _canvasGifs.value = withContext(ioDispatcher) { platform.canvasGifStats() } }
    }

    /** Songs kept from streaming and every cover picture. Downloads and the library stay. */
    fun clearCaches() = runBusy { platform.clearCaches() }

    /** Deletes the cover pictures saved for local songs. The songs and their files stay. */
    fun clearLocalCovers() = runBusy { platform.clearLocalCovers() }

    fun removeAllDownloads() = runBusy {
        platform.removeAllDownloads()
        // the service deletes in the background; give it a moment before measuring again
        delay(1_200)
    }

    fun removeDownloads(songIds: Collection<String>) = runBusy {
        platform.removeDownloads(songIds)
        delay(800)
    }

    fun clearCanvasGifs() = runBusy {
        platform.clearCanvasGifs()
        _canvasGifs.value = withContext(ioDispatcher) { platform.canvasGifStats() }
    }

    /** the drives on offer for [kind], read off the main thread since it measures the folder */
    suspend fun placeChoice(kind: StorageKind): PlaceChoice = withContext(ioDispatcher) { platform.placeChoice(kind) }

    // a folder from the folder picker as the place mochy would save into, or null when it isn't usable
    suspend fun pickedFolder(tree: String): StoragePlaceInfo? = withContext(ioDispatcher) { platform.pickedFolder(tree) }

    fun needsFolderAccess(place: StoragePlaceInfo): Boolean = platform.needsFolderAccess(place)

    fun move(kind: StorageKind, to: StoragePlaceInfo) = platform.move(kind, to)

    fun clearMoveFailure() = platform.clearMoveFailure()

    fun export(tree: String, choice: ExportChoice) = platform.export(tree, choice)

    fun clearExport() = platform.clearExport()

    private fun runBusy(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                _usage.value = withContext(ioDispatcher) { platform.measure() }
                _busy.value = false
            }
        }
    }
}
