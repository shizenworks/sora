package com.mochyy.mochymusic.shared.ios

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mochyy.mochymusic.db.InternalDatabase
import com.mochyy.mochymusic.db.SharedMigrations
import com.mochyy.mochymusic.shared.library.LibraryDatabase
import com.mochyy.mochymusic.shared.library.LocalLibrary
import com.mochyy.mochymusic.shared.settings.PlaybackSettingsFile
import com.mochyy.mochymusic.shared.settings.playbackPreferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal val iosPlaybackPreferences by lazy {
    playbackPreferences { "${applicationSupportPath()}/$PlaybackSettingsFile" }
}

internal val iosLibrary by lazy {
    val database = Room.databaseBuilder<LibraryDatabase>(name = "${applicationSupportPath()}/library.db")
        .setDriver(BundledSQLiteDriver())
        .build()
    LocalLibrary(database.tracks())
}

// the music database shared ViewModels read (Koin hands it out), next to library.db under Android's file name
internal val iosDatabase by lazy {
    Room.databaseBuilder<InternalDatabase>(name = "${applicationSupportPath()}/mochy.db")
        .setDriver(BundledSQLiteDriver())
        .addMigrations(*SharedMigrations)
        .build()
}

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportPath(): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return checkNotNull(directory?.path)
}
