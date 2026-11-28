package com.mochyy.mochymusic.ui.screens.settings.importsync

import androidx.compose.runtime.Composable
import com.mochyy.mochymusic.importer.ImportOptions
import com.mochyy.mochymusic.importer.ImportProgress
import com.mochyy.mochymusic.importer.ParsedImport
import com.mochyy.mochymusic.importer.ResyncRead
import com.mochyy.mochymusic.importer.ResyncReads
import com.mochyy.mochymusic.importer.ResyncStart
import com.mochyy.mochymusic.importer.ResyncTarget
import com.mochyy.mochymusic.importer.YtSyncState
import com.mochyy.mochymusic.importer.account.AccountImportState
import com.mochyy.mochymusic.importer.account.AccountService
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Everything the import sheet needs from the platform that runs it. Android's implementation sits
 * over MusicImportService, YouTubeSyncManager and LinkImporter; a platform without them never binds
 * it, so its import rows stay hidden. Secrets (service cookies, the YouTube session) never cross this
 * contract: only parsed imports and read progress do.
 */
interface ImportPlatform {
    val progress: StateFlow<ImportProgress>
    val youTubeState: StateFlow<YtSyncState>

    /** size and display name of a picked file, when the platform can tell them */
    suspend fun pickedFile(uri: String): PickedFile?

    /** the picked file's text, or null when it can't be opened */
    suspend fun readText(uri: String): String?

    /** writes the export exactly as built, BOM included */
    suspend fun writeFile(uri: String, bytes: ByteArray)

    suspend fun fetchLink(link: String): ParsedImport

    fun startImport(parsed: ParsedImport, options: ImportOptions): Boolean
    fun cancelImport()
    fun clearFinishedImport()

    fun syncNow()
    fun pullHistoryNow()
    fun uploadLocalPlaylists()
    fun undoLastSync(withYouTube: Boolean)
    suspend fun lostLikesCount(): Int
    fun bringBackLikes()
    fun clearYouTubeMessage()

    /** signs out but keeps everything already synced in the library */
    suspend fun signOut()
}

/** what a platform's file picker reports for the chosen file; nulls when it can't say */
data class PickedFile(val sizeBytes: Long?, val displayName: String?)

/** A platform's CSV picker. onPicked gets the platform's own URI as an opaque string. */
fun interface ImportFilePicker {
    fun pick(sourceLabel: String, onPicked: (String?) -> Unit)
}

/** A platform's "save the export as" dialog. onPicked gets the platform's own URI. */
fun interface ImportCsvSaver {
    fun save(defaultName: String, onPicked: (String?) -> Unit)
}

/** what the switch-accounts page shows; its id is the account's opaque hash, never the cookie */
data class SavedAccountInfo(
    val id: String,
    val name: String,
    val email: String,
)

data class AccountSwitcherState(
    val accounts: List<SavedAccountInfo> = emptyList(),
    val activeId: String? = null,
)

/** The platform's saved-accounts list and the switch/remove work behind it. */
interface SavedAccountsGateway {
    val state: StateFlow<AccountSwitcherState>
    fun switchTo(accountId: String)
    fun remove(accountId: String)
}

/** The signed-in account's session, kept by the platform: Android seals it in its preferences store. */
interface AccountSessionGateway {
    suspend fun forgetAccount()
    suspend fun clearSyncedContent()
    suspend fun saveSession(
        cookie: String,
        visitorData: String,
        dataSyncId: String,
        accountName: String,
        accountEmail: String,
        accountChannelHandle: String,
    )

    /** replaces the process with a fresh launch, as the account settings page always did */
    fun restartApp()
}

/** The signed-in account's library, read through the platform's YouTube client. */
interface AccountContentSource {
    suspend fun playlists(hideYoutubeShorts: Boolean): Result<List<PlaylistItem>>
    suspend fun albums(): Result<List<AlbumItem>>
    suspend fun artists(): Result<List<ArtistItem>>
    fun reportFailure(throwable: Throwable)
}

/** One sign-in → list → pick → read pass against one service, as the platform's session runs it. */
interface AccountImportRunner {
    val state: StateFlow<AccountImportState>
    val ready: StateFlow<ParsedImport?>
    val notice: StateFlow<String?>
    val resyncReads: StateFlow<ResyncReads?>

    fun start()
    fun startResync(targets: List<ResyncTarget>)
    fun onHeader(name: String, value: String, url: String)
    fun onApplePageProbe(raw: String?)
    fun onCookies(cookieHeader: String?)
    fun signInAgain()
    fun refreshYouTube()
    fun retry()
    fun toggle(id: String)
    fun selectAll()
    fun selectNone()
    fun readSelected()
    fun pickForResync(id: String)
    fun cancelReading()
    fun consumeReady()
    fun clearNotice()
}

fun interface AccountImportRunnerFactory {
    fun create(service: AccountService, scope: CoroutineScope): AccountImportRunner
}

/** The playlists a Re-sync reads again, looked up in the platform's imported-playlist records. */
interface PlaylistResyncGateway {
    suspend fun accountTargets(service: AccountService): List<ResyncTarget>
    suspend fun target(playlistId: String): ResyncTarget?
    suspend fun startWithReads(reads: Map<String, ResyncRead>): ResyncStart
    suspend fun startFromLink(playlistId: String, link: String): ResyncStart
}

/** One service's sign-in page: the platform draws its own web view and reports what the page sends. */
class AccountSignInPage(
    val service: AccountService,
    val title: String,
    val subtitle: String,
    val onHeader: (name: String, value: String, url: String) -> Unit,
    val onApplePageProbe: (String?) -> Unit,
    val onCookies: (String?) -> Unit,
    val onExit: () -> Unit,
)

interface AccountSignInSurface {
    @Composable
    fun Content(page: AccountSignInPage)
}
