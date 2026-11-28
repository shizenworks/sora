// SystemMediaSession.kt
// Publishes what Mochy is playing to the operating system. Only Linux (MPRIS over D-Bus) is implemented;
// Windows (SMTC) and macOS (Now Playing) get a no-op until native code is added for them.

package com.mochyy.mochymusic.shared.desktop.media

import com.mochyy.mochymusic.shared.playback.PlayerController

interface SystemMediaSession {
    /** Unpublishes from the operating system. Safe to call more than once. */
    fun close()
}

/** Starts the media session for this OS. Never throws: any failure gives a session that does nothing. */
fun createSystemMediaSession(player: PlayerController, controls: DesktopMediaControls): SystemMediaSession {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    if (!os.contains("linux")) return NoOpSystemMediaSession
    return try {
        LinuxMprisSession(player, controls)
    } catch (_: Exception) {
        NoOpSystemMediaSession
    } catch (_: LinkageError) {
        NoOpSystemMediaSession
    }
}

private object NoOpSystemMediaSession : SystemMediaSession {
    override fun close() = Unit
}
