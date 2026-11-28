// AppPreferences.kt
//
// The settings store every platform's Compose UI reads and writes. Each platform provides its one store
// at the root of its composition (Android: context.dataStore, the SecretPreferences instance; desktop and
// iOS: the store their playback preferences own) and warms PreferencesCache with it at start-up, so a
// rememberPreference reads its value synchronously and the first frame already shows it.

package com.mochyy.mochymusic.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.mochyy.mochymusic.extensions.toEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import okio.IOException
import kotlin.concurrent.Volatile

/** Reads the store's current preferences, blocking the caller; used only until the cache is warm. */
internal expect fun readPreferencesBlocking(store: DataStore<Preferences>): Preferences

/**
 * The latest preferences of one store, held so a composition reads them synchronously.
 *
 * A store that can't be read (an unreadable or corrupt file) never takes the app down: the cache stays
 * cold and empty, so rows show their defaults, but that empty set is never treated as the saved data.
 * It stays that way until the app starts again (the collector has ended and the read isn't retried
 * every frame). A write that fails is dropped; nothing is saved.
 */
open class StorePreferencesCache internal constructor() {

    private val snapshotState = mutableStateOf(emptyPreferences())

    @Volatile
    private var plain: Preferences = emptyPreferences()

    @Volatile
    private var warm: Boolean = false

    /** A read failed and nothing has been read since; the blocking read isn't retried every frame. */
    @Volatile
    var readFailed: Boolean = false
        private set

    /** The store the platform started the cache with; compositions that provide none read this one. */
    @Volatile
    var store: DataStore<Preferences>? = null
        private set

    private val writeScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    val snapshot: State<Preferences> get() = snapshotState

    /** Whether [snapshot] holds what the store actually read, rather than the empty stand-in. */
    val isWarm: Boolean get() = warm

    /** Keeps the cache following [store] for as long as [scope] lives; a second start for the same store does nothing. */
    fun start(store: DataStore<Preferences>, scope: CoroutineScope = writeScope) {
        if (this.store === store) return
        this.store = store
        scope.launch(ioDispatcher, start = CoroutineStart.UNDISPATCHED) {
            store.data
                .catch { error -> if (error is IOException) readFailed = true else throw error }
                .collect { prefs -> publish(prefs) }
        }
    }

    fun current(store: DataStore<Preferences>): Preferences {
        if (warm || readFailed) return plain
        val prefs = try {
            readPreferencesBlocking(store)
        } catch (error: IOException) {
            readFailed = true
            return plain
        }
        publish(prefs)
        return prefs
    }

    fun <T> set(store: DataStore<Preferences>, key: Preferences.Key<T>, value: T) {

        writeScope.launch {
            try {
                store.edit { it[key] = value }
            } catch (_: IOException) {
                // the file can't be written; the old value stays on screen, as nothing was saved
            }
        }
    }

    private fun publish(prefs: Preferences) {
        plain = prefs
        warm = true
        readFailed = false
        snapshotState.value = prefs
    }


    fun edit(store: DataStore<Preferences>, block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        writeScope.launch { store.edit { block(it) } }
    }
}

/** The one cache the app's compositions read, warmed by the platform at start-up. */
object PreferencesCache : StorePreferencesCache()

/** The app's settings store for this composition. */
val LocalAppPreferences: ProvidableCompositionLocal<DataStore<Preferences>> = staticCompositionLocalOf {
    PreferencesCache.store ?: error("No app preferences: provide LocalAppPreferences or start PreferencesCache")
}

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val store = LocalAppPreferences.current

    PreferencesCache.current(store)

    val valueState = remember(key, defaultValue) {
        derivedStateOf { PreferencesCache.snapshot.value[key] ?: defaultValue }
    }

    return remember(key, store) {
        object : MutableState<T> {
            override var value: T
                get() = valueState.value
                set(value) = PreferencesCache.set(store, key, value)

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val store = LocalAppPreferences.current
    PreferencesCache.current(store)

    val valueState = remember(key, defaultValue) {
        derivedStateOf { PreferencesCache.snapshot.value[key].toEnum(defaultValue) }
    }

    return remember(key, store) {
        object : MutableState<T> {
            override var value: T
                get() = valueState.value
                set(value) = PreferencesCache.set(store, key, value.name)

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
