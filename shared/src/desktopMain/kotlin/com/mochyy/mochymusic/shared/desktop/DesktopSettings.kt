package com.mochyy.mochymusic.shared.desktop

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mochyy.mochymusic.db.InternalDatabase
import com.mochyy.mochymusic.db.SharedMigrations
import com.mochyy.mochymusic.shared.library.LibraryDatabase
import com.mochyy.mochymusic.shared.library.LocalLibrary
import com.mochyy.mochymusic.shared.settings.PlaybackSettingsFile
import com.mochyy.mochymusic.shared.settings.playbackPreferences
import java.io.File

internal val desktopPlaybackPreferences by lazy {
    playbackPreferences { appDataDirectory().resolve(PlaybackSettingsFile).absolutePath }
}

internal val desktopLibrary by lazy {
    val directory = appDataDirectory()
    directory.mkdirs()
    val database = Room.databaseBuilder<LibraryDatabase>(directory.resolve("library.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .build()
    LocalLibrary(database.tracks())
}

// the music database shared ViewModels read (Koin hands it out), next to library.db under Android's file name
internal val desktopDatabase by lazy {
    val directory = appDataDirectory()
    directory.mkdirs()
    Room.databaseBuilder<InternalDatabase>(directory.resolve("mochy.db").absolutePath)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(*SharedMigrations)
        .build()
}

private fun appDataDirectory(): File {
    val home = File(System.getProperty("user.home"))
    val os = System.getProperty("os.name").lowercase()
    val appData = when {
        os.startsWith("mac") -> home.resolve("Library/Application Support")
        os.startsWith("windows") ->
            System.getenv("APPDATA")?.takeUnless(String::isBlank)?.let(::File) ?: home.resolve("AppData/Roaming")
        else -> System.getenv("XDG_CONFIG_HOME")?.let(::File)?.takeIf(File::isAbsolute) ?: home.resolve(".config")
    }
    return appData.resolve("Mochy")
}
