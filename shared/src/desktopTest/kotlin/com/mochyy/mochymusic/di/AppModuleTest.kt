package com.mochyy.mochymusic.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.mochyy.mochymusic.db.InternalDatabase
import com.mochyy.mochymusic.db.MochyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okio.Path.Companion.toOkioPath
import org.koin.dsl.koinApplication
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertSame

class AppModuleTest {
    // Koin must hand out exactly what the platform passed in: one database, one store, never its own copy
    @Test
    fun handsOutThePlatformsOwnDatabaseAndStore() {
        val directory = createTempDirectory("mochy-koin").toFile()
        val database = Room.databaseBuilder<InternalDatabase>(directory.resolve("mochy.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
            .build()
        // the store's actor runs on this test's scope, so it can be stopped and waited for before the
        // directory goes away instead of living on Dispatchers.IO past the end of the test
        val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.createWithPath(scope = storeScope) {
            directory.resolve("settings.preferences_pb").toOkioPath()
        }
        val koin = koinApplication { modules(appModule({ database }, { store })) }.koin
        try {
            assertSame<MochyDatabase>(database, koin.get<MochyDatabase>())
            assertSame(store, koin.get<DataStore<Preferences>>())
            assertSame(koin.get<MochyDatabase>(), koin.get<MochyDatabase>())
        } finally {
            koin.close()
            runBlocking { withTimeout(5_000) { storeScope.coroutineContext[Job]!!.cancelAndJoin() } }
            database.close()
            directory.deleteRecursively()
        }
    }
}
