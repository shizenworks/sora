// DataStore.kt

package com.mochyy.mochymusic.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.mochyy.mochymusic.extensions.toEnum
import kotlin.properties.ReadOnlyProperty

private val Context.settingsFile: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Volatile private var sealedSettings: SecretPreferences? = null

// the settings store everyone reads and writes, with the secrets in it sealed on disk by the vault
val Context.dataStore: DataStore<Preferences>
    get() = sealedSettings ?: synchronized(SecretPreferences) {
        sealedSettings ?: SecretPreferences(applicationContext.settingsFile).also { sealedSettings = it }
    }

// PreferencesCache, LocalAppPreferences, rememberPreference and rememberEnumPreference live in :shared
// (utils/AppPreferences.kt, same package); App starts the cache on this sealed store

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? =
    PreferencesCache.current(this)[key]

fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>,
    defaultValue: T,
): T = PreferencesCache.current(this)[key] ?: defaultValue

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key] ?: defaultValue }

inline fun <reified T : Enum<T>> enumPreference(
    context: Context,
    key: Preferences.Key<String>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key].toEnum(defaultValue) }
