package com.mochyy.mochymusic.shared.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.music.innertube.models.SongItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.IOException
import okio.Path.Companion.toPath

internal sealed interface UpcomingQueueRead {
    data object Loading : UpcomingQueueRead
    data class Loaded(val songs: List<SongItem>) : UpcomingQueueRead
    data object Unavailable : UpcomingQueueRead
}

// [store] is the platform's one settings file; shared Compose UI reads it through LocalAppPreferences
internal class PlaybackPreferences(val store: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    val values = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    /** A failed read stays distinguishable from a valid empty queue so startup can never erase it. */
    val upcomingQueue: Flow<UpcomingQueueRead> = store.data
        .map<Preferences, UpcomingQueueRead> { preferences -> UpcomingQueueRead.Loaded(decodeQueue(preferences[UpcomingQueueKey])) }
        .catch { error ->
            if (error is IOException) emit(UpcomingQueueRead.Unavailable) else throw error
        }

    suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    suspend fun setUpcomingQueue(songs: List<SongItem>) {
        val payload = json.encodeToString(songs.map(SongItem::withoutTransientTokens))
        store.edit { it[UpcomingQueueKey] = payload }
    }

    private fun decodeQueue(payload: String?): List<SongItem> {
        if (payload.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<SongItem>>(payload).map(SongItem::withoutTransientTokens)
        } catch (_: SerializationException) {
            // Old or partially written values should not crash the player or be rewritten on startup.
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    private companion object {
        val UpcomingQueueKey = stringPreferencesKey("upcomingQueue")
    }
}

private fun SongItem.withoutTransientTokens(): SongItem = copy(
    endpoint = null,
    libraryAddToken = null,
    libraryRemoveToken = null,
    historyRemoveToken = null,
    setVideoId = null,
)

internal const val PlaybackSettingsFile = "settings.preferences_pb"

internal fun playbackPreferences(path: () -> String): PlaybackPreferences =
    PlaybackPreferences(PreferenceDataStoreFactory.createWithPath { path().toPath() })
