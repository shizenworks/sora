// ImportSyncViewModel.kt

package com.mochyy.mochymusic.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.importer.ImportOptions
import com.mochyy.mochymusic.importer.ImportProgress
import com.mochyy.mochymusic.importer.LinkImportException
import com.mochyy.mochymusic.importer.MochyCsvExporter
import com.mochyy.mochymusic.importer.ParsedImport
import com.mochyy.mochymusic.importer.YtSyncState
import com.mochyy.mochymusic.importer.detectLinkSource
import com.mochyy.mochymusic.importer.parseImportCsv
import com.mochyy.mochymusic.ui.screens.settings.importsync.ImportPlatform
import com.mochyy.mochymusic.utils.ioDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The Import settings page's file/link import and its export. The import service, the YouTube sync
 * manager and the platform's file access come in through [ImportPlatform]; Koin builds it on Android
 * (shared di/ImportSyncModule.kt plus the app's bindings), where those services live.
 */
class ImportSyncViewModel(
    private val database: MochyDatabase,
    private val platform: ImportPlatform,
) : ViewModel() {

    val importProgress: StateFlow<ImportProgress> = platform.progress
    val youTubeState: StateFlow<YtSyncState> = platform.youTubeState

    private val _pending = MutableStateFlow<PendingImport>(PendingImport.Idle)
    val pending: StateFlow<PendingImport> = _pending.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private var loadJob: Job? = null

    fun loadLink(link: String) {
        val source = detectLinkSource(link)?.label ?: "link"
        loadJob?.cancel()
        _pending.value = PendingImport.Loading("Reading the $source playlist…")
        loadJob = viewModelScope.launch(ioDispatcher) {
            _pending.value = try {
                PendingImport.Ready(platform.fetchLink(link), source)
            } catch (e: CancellationException) {
                throw e
            } catch (e: LinkImportException) {
                PendingImport.Failed(e.message ?: "Couldn't read that link.")
            } catch (e: Exception) {
                PendingImport.Failed("Couldn't read that link.")
            }
        }
    }

    /** [sourceLabel] is only for the review dialog ("Mochy backup", "TuneMyMusic"). */
    fun loadFile(uri: String, sourceLabel: String) {
        loadJob?.cancel()
        _pending.value = PendingImport.Loading("Reading the file…")
        loadJob = viewModelScope.launch(ioDispatcher) {
            _pending.value = try {
                val file = platform.pickedFile(uri)
                if (file?.sizeBytes != null && file.sizeBytes > MAX_CSV_BYTES) {
                    PendingImport.Failed("That file is too big to be a playlist export.")
                } else {
                    val text = platform.readText(uri) ?: throw IllegalStateException("Couldn't open that file")
                    val fileName = file?.displayName
                        ?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Imported playlist"
                    val parsed = parseImportCsv(text, fallbackPlaylistName = fileName)
                    if (parsed.totalSongs == 0 && parsed.totalPlaylists == 0) {
                        PendingImport.Failed("No songs in that file. It needs a header row with at least a \"Track name\" column.")
                    } else {
                        PendingImport.Ready(parsed, sourceLabel)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PendingImport.Failed("Couldn't read that file. Make sure it's a .csv export.")
            }
        }
    }

    /** Something already read elsewhere (an account import) goes straight to the review dialog. */
    fun offer(parsed: ParsedImport, sourceLabel: String) {
        loadJob?.cancel()
        _pending.value = PendingImport.Ready(parsed, sourceLabel)
    }

    fun dismissPending() {
        loadJob?.cancel()
        _pending.value = PendingImport.Idle
    }

    /** False when another import is still running. */
    fun startImport(parsed: ParsedImport, mirrorToYouTube: Boolean): Boolean {
        val started = platform.startImport(parsed, ImportOptions(mirrorToYouTube = mirrorToYouTube))
        if (started) _pending.value = PendingImport.Idle
        return started
    }

    fun cancelImport() = platform.cancelImport()

    fun clearFinishedImport() = platform.clearFinishedImport()

    fun export(uri: String) {
        _exportMessage.value = "Exporting…"
        viewModelScope.launch(ioDispatcher) {
            _exportMessage.value = try {
                val (csv, summary) = MochyCsvExporter.build(database)
                // the BOM makes Excel read names in any script correctly; our reader skips it
                val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                platform.writeFile(uri, bom + csv.encodeToByteArray())
                "Exported ${summary.likedSongs} liked songs and ${summary.playlists} playlists (${summary.playlistSongs} songs)."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Export failed: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    fun syncNow() = platform.syncNow()
    fun pullHistoryNow() = platform.pullHistoryNow()
    fun uploadLocalPlaylists() = platform.uploadLocalPlaylists()
    fun undoLastSync(withYouTube: Boolean) = platform.undoLastSync(withYouTube)
    suspend fun lostLikesCount() = platform.lostLikesCount()
    fun bringBackLikes() = platform.bringBackLikes()
    fun clearYouTubeMessage() = platform.clearYouTubeMessage()

    /** Signs out but keeps everything already synced in the library. */
    fun signOut() {
        viewModelScope.launch(ioDispatcher) { platform.signOut() }
    }

    private companion object {
        const val MAX_CSV_BYTES = 40L * 1024 * 1024
    }
}
