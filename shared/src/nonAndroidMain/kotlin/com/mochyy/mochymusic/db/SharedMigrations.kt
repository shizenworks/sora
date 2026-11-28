package com.mochyy.mochymusic.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** The version desktop and iOS created their first mochy.db at; nothing older exists off Android. */
internal const val FirstNonAndroidVersion = 45

/**
 * Schema upgrades for the desktop and iOS mochy.db, written against SQLiteConnection.
 *
 * Android keeps its own AllMigrations (app/, SupportSQLite, versions 1 to 45). Those never run here:
 * desktop and iOS databases were first created at [FirstNonAndroidVersion], so only steps from there
 * on belong in this list. Without an entry for a bump, Room refuses to open the file at all, and the
 * app stops on start instead of showing the library. SharedMigrationsTest fails CI when
 * [MusicDatabaseVersion] moves past the last step listed here.
 *
 * When a bump lands: add `object : Migration(n, n + 1) { override fun migrate(connection: SQLiteConnection) { ... } }`
 * here with the same SQL as the Android migration for that step.
 */
internal val SharedMigrations: Array<Migration> = arrayOf(
    object : Migration(45, 46) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `artist` ADD COLUMN `blockedAt` INTEGER DEFAULT NULL")
        }
    },
)
