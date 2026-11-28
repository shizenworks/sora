package com.mochyy.mochymusic.shared.ios

import kotlinx.coroutines.MainScope
import com.mochyy.mochymusic.shared.playback.SharedQueue
import com.mochyy.mochymusic.shared.playback.QueuePlayerConnection
import com.mochyy.mochymusic.shared.lyrics.playerLyricsProviders
import com.mochyy.mochymusic.shared.lyrics.LyricsProvider
import com.mochyy.mochymusic.shared.playback.QueueTransport

/** One player and one library shared by the phone UI and CarPlay scenes. */
internal object IosPlaybackRuntime {
    val youtube by lazy { IosYouTubeClient() }
    val library by lazy { iosLibrary }
    var playbackFailedText: String = "Playback failed."
    /** The phone UI's queue owner; system next/previous go through it. */
    val queueTransport = QueueTransport()

    val player by lazy { IosPlayerController(youtube, queueTransport) { playbackFailedText } }
    /** Up next for the whole app; started by whichever comes first, the phone UI or a CarPlay scene. */
    val queue by lazy {
        SharedQueue(player, iosPlaybackPreferences, library, queueTransport, MainScope()).also { it.start() }
    }
    /** The shared screens' player, behind the portable contract. */
    val connection by lazy { QueuePlayerConnection(player, queue, MainScope()) }
    val lyricsProviders: List<LyricsProvider> by lazy { playerLyricsProviders(youtube.http) }
}
