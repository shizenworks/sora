package com.mochyy.mochymusic.playback

import android.content.Context
import android.os.PowerManager
import com.mochyy.mochymusic.constants.KeepCpuAwakeKey
import com.mochyy.mochymusic.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Holds a partial wake lock while music plays, if "keep CPU awake" is on, so playback doesn't stall
 * with the screen off. The lock is taken only while playing and dropped on pause. The setting is
 * read through a flow, so nothing reads the preferences on the main thread.
 */
class CpuWakeHold(private val context: Context) {
    private val lock = context.getSystemService(PowerManager::class.java)
        ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mochy:playback")
        ?.apply { setReferenceCounted(false) }
    @Volatile private var keepAwake = false
    private var playing = false
    private var watch: Job? = null

    /** Follows the setting for as long as [scope] lives, so turning it off drops the lock at once. */
    fun attach(scope: CoroutineScope) {
        watch?.cancel()
        watch = scope.launch {
            context.dataStore.data.map { it[KeepCpuAwakeKey] == true }.distinctUntilChanged().collect {
                keepAwake = it
                update()
            }
        }
    }

    fun onPlayingChanged(isPlaying: Boolean) {
        playing = isPlaying
        update()
    }

    fun release() {
        watch?.cancel()
        watch = null
        if (lock?.isHeld == true) lock.release()
    }

    private fun update() {
        val wake = lock ?: return
        val hold = keepAwake && playing
        if (hold && !wake.isHeld) wake.acquire()
        if (!hold && wake.isHeld) wake.release()
    }
}
