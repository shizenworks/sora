package com.mochyy.mochymusic.viewmodels

// Exporting everything to one zip and bringing a zip back in: SAF documents, the app's data folder,
// the database and the restart that swaps a restore in. The shared wizard and storage section only
// show progress, so all of that sits behind this contract on Android.

/** what a transfer zip says it holds */
data class TransferManifestInfo(
    val formatVersion: Int,
    val createdAt: Long,
    val appId: String,
    val appVersion: String,
    val dataDir: String,
    val databaseVersion: Int,
    val songs: Int,
    val playlists: Int,
    val plays: Int,
    val localSongs: Int,
    val bytes: Long,
)

interface TransferPlatform {
    /** the permission that lets mochy find local songs again; it changed name in Android 13 */
    val audioPermission: String

    /** the name the export suggests, so it's easy to spot in downloads later */
    fun exportFileName(): String

    suspend fun export(target: String, onProgress: (Float) -> Unit): Result<Unit>

    /** checks a picked zip before anything is unpacked, the failure message is ready to show */
    suspend fun peek(source: String): Result<TransferManifestInfo>

    /** true when the zip has local songs and mochy can't see the phone's music yet, so it's worth asking first */
    fun wantsAudioPermission(manifest: TransferManifestInfo): Boolean

    suspend fun stage(source: String, onProgress: (Float) -> Unit): Result<Unit>

    /** restarts mochy after an import, so the next start swaps it in */
    suspend fun restartToFinish()
}
