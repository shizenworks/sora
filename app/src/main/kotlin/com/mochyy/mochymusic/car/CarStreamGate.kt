// CarStreamGate.kt

package com.mochyy.mochymusic.car

import android.content.Context
import android.net.ConnectivityManager
import androidx.media3.common.MediaItem
import com.mochyy.mochymusic.constants.CarMeteredStreamingKey
import com.mochyy.mochymusic.constants.SongSortType
import com.mochyy.mochymusic.db.MusicDatabase
import com.mochyy.mochymusic.playback.CachedSongs
import com.mochyy.mochymusic.utils.dataStore
import com.mochyy.mochymusic.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

// the car's mobile data rule: with streaming off, on a metered network only songs already on the phone may start
class CarStreamGate @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val cachedSongs: CachedSongs,
) {
    companion object {
        // android auto's own package; its media browser is the car's way into the library here
        const val AndroidAuto = "com.google.android.projection.gearhead"
    }

    // true when songs that aren't on the phone must not start right now
    fun blocksStreams(): Boolean {
        if (context.dataStore.get(CarMeteredStreamingKey, true)) return false
        return context.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered == true
    }

    // the items that may start: all of them unless streams are blocked, then only the ones already on the phone
    suspend fun playable(items: List<MediaItem>): List<MediaItem> {
        if (!blocksStreams()) return items
        val onPhone = onPhoneIds()
        return items.filter { it.mediaId in onPhone }
    }

    // songs that play without the network: local files, downloads and whatever the player cache holds whole
    private suspend fun onPhoneIds(): Set<String> = withContext(Dispatchers.IO) {
        val local = database.localSongs(SongSortType.CREATE_DATE, true).first().map { it.id }
        val downloaded = database.downloadedSongs(SongSortType.CREATE_DATE, true).first().map { it.id }
        val cached = cachedSongs.songs.first().map { it.id }
        (local + downloaded + cached).toSet()
    }
}
