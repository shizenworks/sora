package com.mochyy.mochymusic.di.koin

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.App
import com.mochyy.mochymusic.constants.AccountChannelHandleKey
import com.mochyy.mochymusic.constants.AccountEmailKey
import com.mochyy.mochymusic.constants.AccountNameKey
import com.mochyy.mochymusic.constants.DataSyncIdKey
import com.mochyy.mochymusic.constants.InnerTubeCookieKey
import com.mochyy.mochymusic.constants.SavedAccountsKey
import com.mochyy.mochymusic.constants.VisitorDataKey
import com.mochyy.mochymusic.importer.ImportOptions
import com.mochyy.mochymusic.importer.LinkImporter
import com.mochyy.mochymusic.importer.MusicImportService
import com.mochyy.mochymusic.importer.ParsedImport
import com.mochyy.mochymusic.importer.PlaylistResync
import com.mochyy.mochymusic.importer.ResyncRead
import com.mochyy.mochymusic.importer.YouTubeSyncManager
import com.mochyy.mochymusic.importer.account.AccountService
import com.mochyy.mochymusic.ui.screens.settings.importsync.AccountContentSource
import com.mochyy.mochymusic.ui.screens.settings.importsync.AccountImportRunner
import com.mochyy.mochymusic.ui.screens.settings.importsync.AccountImportRunnerFactory
import com.mochyy.mochymusic.ui.screens.settings.importsync.AccountSessionGateway
import com.mochyy.mochymusic.ui.screens.settings.importsync.AccountSwitcherState
import com.mochyy.mochymusic.ui.screens.settings.importsync.ImportPlatform
import com.mochyy.mochymusic.ui.screens.settings.importsync.PickedFile
import com.mochyy.mochymusic.ui.screens.settings.importsync.PlaylistResyncGateway
import com.mochyy.mochymusic.ui.screens.settings.importsync.SavedAccountInfo
import com.mochyy.mochymusic.ui.screens.settings.importsync.SavedAccountsGateway
import com.mochyy.mochymusic.utils.SavedAccount
import com.mochyy.mochymusic.utils.SyncUtils
import com.mochyy.mochymusic.utils.dataStore
import com.mochyy.mochymusic.utils.reportException
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.utils.completed
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

/**
 * The platform half of the import and sync settings: Android's import service, YouTube sync, file
 * access, sealed saved-accounts store and sign-in web view. The view models live in shared and are
 * bound in shared di/ImportSyncModule.kt; this loads what they need. It reads Hilt's singletons as
 * they are, so the import running in the background is the same one every other page sees.
 */
fun startImportSyncKoin(application: Application) {
    val graph = EntryPointAccessors.fromApplication(application, ImportSyncEntryPoint::class.java)
    loadKoinModules(
        module {
            single<ImportPlatform> {
                AndroidImportPlatform(graph.context(), graph.musicImportService(), graph.youTubeSyncManager())
            }
            single<SavedAccountsGateway> { AndroidSavedAccountsGateway(graph.context(), graph.syncUtils()) }
            single<AccountSessionGateway> { AndroidAccountSessionGateway(graph.context(), graph.syncUtils()) }
            single<AccountContentSource> { AndroidAccountContentSource() }
            single<AccountImportRunnerFactory> { AndroidAccountImportRunnerFactory(graph.context()) }
            single<PlaylistResyncGateway> { AndroidPlaylistResyncGateway(graph.playlistResync()) }
        }
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImportSyncEntryPoint {
    @ApplicationContext
    fun context(): Context

    fun musicImportService(): MusicImportService
    fun youTubeSyncManager(): YouTubeSyncManager
    fun syncUtils(): SyncUtils
    fun playlistResync(): PlaylistResync
}

private class AndroidImportPlatform(
    private val context: Context,
    private val importService: MusicImportService,
    private val youTubeSync: YouTubeSyncManager,
) : ImportPlatform {
    override val progress = importService.progress
    override val youTubeState = youTubeSync.state

    override suspend fun pickedFile(uri: String): PickedFile? {
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return PickedFile(null, null)
        return PickedFile(
            sizeBytes = queryColumn(parsed, OpenableColumns.SIZE)?.toLongOrNull(),
            displayName = queryColumn(parsed, OpenableColumns.DISPLAY_NAME),
        )
    }

    override suspend fun readText(uri: String): String? =
        context.contentResolver.openInputStream(Uri.parse(uri))?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }

    override suspend fun writeFile(uri: String, bytes: ByteArray) {
        context.contentResolver.openOutputStream(Uri.parse(uri))?.use { out -> out.write(bytes) }
            ?: throw IllegalStateException("Couldn't write the file")
    }

    override suspend fun fetchLink(link: String) = LinkImporter.fetch(link)

    override fun startImport(parsed: ParsedImport, options: ImportOptions): Boolean =
        importService.startImport(parsed, options)

    override fun cancelImport() = importService.cancel()

    override fun clearFinishedImport() = importService.clearFinished()

    override fun syncNow() = youTubeSync.syncNow()

    override fun pullHistoryNow() = youTubeSync.pullHistoryNow()

    override fun uploadLocalPlaylists() = youTubeSync.uploadLocalPlaylists()

    override fun undoLastSync(withYouTube: Boolean) = youTubeSync.undoLastSync(withYouTube)

    override suspend fun lostLikesCount(): Int = youTubeSync.lostLikesCount()

    override fun bringBackLikes() = youTubeSync.bringBackLikes()

    override fun clearYouTubeMessage() = youTubeSync.clearMessage()

    override suspend fun signOut() = App.forgetAccount(context)

    private fun queryColumn(uri: Uri, column: String): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
        }
    }.getOrNull()
}

private class AndroidSavedAccountsGateway(
    private val context: Context,
    private val syncUtils: SyncUtils,
) : SavedAccountsGateway {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val state: StateFlow<AccountSwitcherState> =
        context.dataStore.data
            .map { prefs ->
                AccountSwitcherState(
                    SavedAccount.parse(prefs[SavedAccountsKey]).map(::toInfo),
                    SavedAccount.fromActive(prefs)?.opaqueId,
                )
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), AccountSwitcherState())

    init {
        // keeps the saved copy of whoever is signed in, so a sign-in from any screen lands in the list
        scope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { SavedAccount.fromActive(it) }
                .distinctUntilChanged()
                .collect { active ->
                    if (active != null) context.dataStore.edit { SavedAccount.upsert(it, active) }
                }
        }
    }

    override fun switchTo(accountId: String) {
        scope.launch(Dispatchers.IO) {
            val account = SavedAccount.parse(context.dataStore.data.first()[SavedAccountsKey])
                .firstOrNull { it.opaqueId == accountId } ?: return@launch
            // stop the old account's queued and running syncs before the session changes hands
            syncUtils.cancelAllSyncs()
            context.dataStore.edit { it.writeActive(account) }
            // the sync reads the client's session, so set it here rather than wait for the app's collector
            runCatching {
                YouTube.cookie = account.cookie
                if (account.visitorData.isNotBlank()) YouTube.visitorData = account.visitorData
                YouTube.dataSyncId = normalizeDataSyncId(account.dataSyncId)
            }
            syncUtils.performFullSync()
        }
    }

    override fun remove(accountId: String) {
        scope.launch(Dispatchers.IO) {
            val removed = SavedAccount.parse(context.dataStore.data.first()[SavedAccountsKey])
                .firstOrNull { it.opaqueId == accountId } ?: return@launch
            val wasActive = SavedAccount.fromActive(context.dataStore.data.first())?.opaqueId == removed.opaqueId
            context.dataStore.edit { prefs ->
                prefs[SavedAccountsKey] = SavedAccount.encode(
                    SavedAccount.parse(prefs[SavedAccountsKey]).filterNot { it.opaqueId == removed.opaqueId }
                )
            }
            // removing the signed-in account signs it out, same as the sign-out row
            if (wasActive) App.forgetAccount(context)
        }
    }

    private fun toInfo(account: SavedAccount) = SavedAccountInfo(account.opaqueId, account.name, account.email)

    private fun MutablePreferences.writeActive(a: SavedAccount) {
        putOrRemove(InnerTubeCookieKey, a.cookie)
        putOrRemove(VisitorDataKey, a.visitorData)
        putOrRemove(DataSyncIdKey, a.dataSyncId)
        putOrRemove(AccountNameKey, a.name)
        putOrRemove(AccountEmailKey, a.email)
        putOrRemove(AccountChannelHandleKey, a.channelHandle)
    }

    // a missing value is removed rather than written blank, so the previous account's value doesn't carry over
    private fun MutablePreferences.putOrRemove(key: Preferences.Key<String>, value: String) {
        if (value.isBlank()) remove(key) else this[key] = value
    }

    // same cleanup the app applies to the data sync id before it reaches the client
    private fun normalizeDataSyncId(raw: String): String? =
        raw.takeIf { it.isNotBlank() }?.let {
            it.takeIf { !it.contains("||") } ?: it.takeIf { it.endsWith("||") }?.substringBefore("||") ?: it.substringAfter("||")
        }
}

private class AndroidAccountSessionGateway(
    private val context: Context,
    private val syncUtils: SyncUtils,
) : AccountSessionGateway {
    override suspend fun forgetAccount() = App.forgetAccount(context)

    override suspend fun clearSyncedContent() = syncUtils.clearAllSyncedContent()

    override suspend fun saveSession(
        cookie: String,
        visitorData: String,
        dataSyncId: String,
        accountName: String,
        accountEmail: String,
        accountChannelHandle: String,
    ) {
        context.dataStore.edit { settings ->
            settings[InnerTubeCookieKey] = cookie
            settings[VisitorDataKey] = visitorData
            settings[DataSyncIdKey] = dataSyncId
            settings[AccountNameKey] = accountName
            settings[AccountEmailKey] = accountEmail
            settings[AccountChannelHandleKey] = accountChannelHandle
        }
    }

    override fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}

private class AndroidAccountContentSource : AccountContentSource {
    override suspend fun playlists(hideYoutubeShorts: Boolean): Result<List<PlaylistItem>> =
        YouTube.library("FEmusic_liked_playlists").completed().map { it.items.filterIsInstance<PlaylistItem>() }

    override suspend fun albums(): Result<List<AlbumItem>> =
        YouTube.library("FEmusic_liked_albums").completed().map { it.items.filterIsInstance<AlbumItem>() }

    override suspend fun artists(): Result<List<ArtistItem>> =
        YouTube.library("FEmusic_library_corpus_artists").completed().map { it.items.filterIsInstance<ArtistItem>() }

    override fun reportFailure(throwable: Throwable) = reportException(throwable)
}

private class AndroidAccountImportRunnerFactory(
    private val context: Context,
) : AccountImportRunnerFactory {
    override fun create(service: AccountService, scope: CoroutineScope): AccountImportRunner =
        com.mochyy.mochymusic.importer.account.AccountImportSession(service, context, scope)
}

private class AndroidPlaylistResyncGateway(
    private val resync: PlaylistResync,
) : PlaylistResyncGateway {
    override suspend fun accountTargets(service: AccountService) = resync.accountTargets(service)

    override suspend fun target(playlistId: String) = resync.target(playlistId)

    override suspend fun startWithReads(reads: Map<String, ResyncRead>) =
        resync.startWithReads(reads)

    override suspend fun startFromLink(playlistId: String, link: String) = resync.startFromLink(playlistId, link)
}
