// Android's transfer engine and SAF documents, behind the shared transfer view model's contract.

package com.mochyy.mochymusic.ui.screens.settings.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.mochyy.mochymusic.importer.transfer.TransferExporter
import com.mochyy.mochymusic.importer.transfer.TransferImporter
import com.mochyy.mochymusic.importer.transfer.TransferManifest
import com.mochyy.mochymusic.importer.transfer.TransferRestore
import com.mochyy.mochymusic.viewmodels.TransferManifestInfo
import com.mochyy.mochymusic.viewmodels.TransferPlatform
import java.time.LocalDate
import timber.log.Timber

class AndroidTransferPlatform(
    private val context: Context,
    private val exporter: TransferExporter,
    private val importer: TransferImporter,
) : TransferPlatform {

    override val audioPermission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    override fun exportFileName(): String = "mochy-export-${LocalDate.now()}.zip"

    override suspend fun export(target: String, onProgress: (Float) -> Unit): Result<Unit> =
        exporter.export(Uri.parse(target), onProgress)
            .onFailure { Timber.e(it, "Export failed") }
            .map { }

    override suspend fun peek(source: String): Result<TransferManifestInfo> =
        importer.peek(Uri.parse(source)).map { it.toInfo() }

    override fun wantsAudioPermission(manifest: TransferManifestInfo): Boolean =
        manifest.localSongs > 0 && ContextCompat.checkSelfPermission(context, audioPermission) != PackageManager.PERMISSION_GRANTED

    override suspend fun stage(source: String, onProgress: (Float) -> Unit): Result<Unit> =
        importer.stage(Uri.parse(source), onProgress)
            .onFailure { Timber.e(it, "Import failed") }
            .map { }

    override suspend fun restartToFinish() = TransferRestore.restartToFinish(context)

    private fun TransferManifest.toInfo(): TransferManifestInfo = TransferManifestInfo(
        formatVersion = formatVersion,
        createdAt = createdAt,
        appId = appId,
        appVersion = appVersion,
        dataDir = dataDir,
        databaseVersion = databaseVersion,
        songs = songs,
        playlists = playlists,
        plays = plays,
        localSongs = localSongs,
        bytes = bytes,
    )
}
