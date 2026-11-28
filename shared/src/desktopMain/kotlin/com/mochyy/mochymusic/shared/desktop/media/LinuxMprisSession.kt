// LinuxMprisSession.kt
// MPRIS over the session D-Bus (org.mpris.MediaPlayer2): media keys, playerctl and desktop media widgets
// control Mochy and read its title, artist, art and position. Every bus failure just leaves MPRIS off.

package com.mochyy.mochymusic.shared.desktop.media

import com.mochyy.mochymusic.shared.playback.PlayerController
import com.mochyy.mochymusic.shared.playback.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.exceptions.DBusExecutionException
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.Variant

private const val MPRIS_BUS_NAME = "org.mpris.MediaPlayer2.mochy"
private const val MPRIS_PATH = "/org/mpris/MediaPlayer2"
private const val MPRIS_ROOT = "org.mpris.MediaPlayer2"
private const val MPRIS_PLAYER = "org.mpris.MediaPlayer2.Player"
// Position updates arrive a few times a second; a jump past this is a seek, not timing jitter.
private const val SEEK_JUMP_MS = 2_000L
// A wedged session bus must not hold up playback or shutdown for dbus-java's 20 s default.
private const val DBUS_CALL_TIMEOUT_MILLIS = 2_000

@DBusInterfaceName(MPRIS_PLAYER)
interface MprisPlayerMethods : DBusInterface {
    fun Play()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Next()
    fun Previous()
    fun Seek(offset: Long)
    fun SetPosition(trackId: DBusPath, position: Long)

    /** Sent after the position jumps (a seek from here, from the app, or a restart), in microseconds. */
    class Seeked(path: String, val position: Long) : DBusSignal(path, position)
}

/**
 * The Linux media session. The D-Bus connection is opened on a background thread so a slow bus never
 * blocks the window; if it cannot be opened, nothing is published and playback continues untouched.
 */
internal class LinuxMprisSession(
    private val player: PlayerController,
    private val controls: DesktopMediaControls,
) : SystemMediaSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var connection: DBusConnection? = null // guarded by lock
    private var closed = false // guarded by lock
    @Volatile private var latest = PlayerState()
    // where the position was at the last state update, to tell a seek from playback moving on
    private var lastSongId: String? = null
    private var lastPositionMs = 0L
    private var lastAdvancing = false
    private var lastMark: TimeMark? = null

    init {
        scope.launch {
            // Only song and play/pause changes publish; the position is read on demand, never pushed per tick.
            player.state
                .onEach {
                    noticeSeek(it)
                    latest = it
                }
                .map { it.song?.id to it.isPlaying }
                .distinctUntilChanged()
                .collect { publish() }
        }
        scope.launch {
            player.queueState
                .map { it.canSkipNext }
                .distinctUntilChanged()
                .collect { publish() }
        }
        controls.queueTransport?.let { transport ->
            scope.launch {
                transport.canGoNext.collect { publish() }
            }
        }
        scope.launch { connect() }
    }

    override fun close() {
        val bus = synchronized(lock) {
            closed = true
            connection.also { connection = null }
        }
        scope.cancel()
        if (bus != null) teardown(bus)
    }

    private fun connect() {
        val bus = try {
            DBusConnectionBuilder.forSessionBus().apply {
                withShared(false)
                transportConfig().withTimeout(DBUS_CALL_TIMEOUT_MILLIS)
            }.build()
        } catch (_: Exception) {
            return
        }
        val accepted = synchronized(lock) {
            if (closed) false else {
                connection = bus
                true
            }
        }
        if (!accepted) {
            bus.close()
            return
        }
        try {
            bus.requestBusName(MPRIS_BUS_NAME)
            bus.exportObject(MPRIS_PATH, MprisObject())
        } catch (_: Exception) {
            clearConnection(bus)
            teardown(bus)
            return
        }
        publish()
    }

    private fun clearConnection(bus: DBusConnection) {
        synchronized(lock) { if (connection === bus) connection = null }
    }

    private fun teardown(bus: DBusConnection) {
        try {
            bus.unExportObject(MPRIS_PATH)
        } catch (_: Exception) {
        }
        try {
            bus.releaseBusName(MPRIS_BUS_NAME)
        } catch (_: Exception) {
        }
        try {
            bus.close()
        } catch (_: Exception) {
        }
    }

    private fun publish() {
        val bus = synchronized(lock) { connection } ?: return
        try {
            bus.sendMessage(Properties.PropertiesChanged(MPRIS_PATH, MPRIS_PLAYER, playerProperties(latest), emptyList()))
        } catch (_: Exception) {
            // A missed signal only delays the widget; the next change publishes everything again.
        }
    }

    // clients count the position forward on their own, so they're told only when it jumps: a gap of more
    // than SEEK_JUMP_MS between where playback should be and where it is, within the same song
    private fun noticeSeek(state: PlayerState) {
        val songId = state.song?.id
        val mark = lastMark
        if (songId != null && songId == lastSongId && mark != null) {
            val expected = lastPositionMs + if (lastAdvancing) mark.elapsedNow().inWholeMilliseconds else 0L
            if (abs(state.positionMs - expected) > SEEK_JUMP_MS) sendSeeked(state.positionMs)
        }
        lastSongId = songId
        lastPositionMs = state.positionMs
        lastAdvancing = state.isPlaying && !state.isLoading
        lastMark = TimeSource.Monotonic.markNow()
    }

    private fun sendSeeked(positionMs: Long) {
        val bus = synchronized(lock) { connection } ?: return
        try {
            bus.sendMessage(MprisPlayerMethods.Seeked(MPRIS_PATH, positionMs * 1000L))
        } catch (_: Exception) {
            // a missed Seeked only leaves a widget's clock off until the next song or seek
        }
    }

    private fun playerProperties(state: PlayerState): Map<String, Variant<*>> {
        val song = state.song
        val status = when {
            song == null -> "Stopped"
            state.isPlaying -> "Playing"
            else -> "Paused"
        }
        val durationMs = state.durationMs.takeIf { it > 0 } ?: ((song?.duration ?: 0) * 1000L)
        val canGoNext = player.queueState.value.canSkipNext ||
            controls.queueTransport?.canGoNext?.value == true ||
            controls.onNext != null
        return mapOf(
            "PlaybackStatus" to Variant(status),
            "Metadata" to Variant(metadata(state, durationMs), "a{sv}"),
            "CanGoNext" to Variant(canGoNext),
            "CanGoPrevious" to Variant(song != null),
            "CanPlay" to Variant(song != null),
            "CanPause" to Variant(song != null),
            "CanSeek" to Variant(song != null && durationMs > 0),
            "CanControl" to Variant(true),
        )
    }

    private fun metadata(state: PlayerState, durationMs: Long): Map<String, Variant<*>> {
        val song = state.song ?: return emptyMap()
        val map = mutableMapOf<String, Variant<*>>(
            "mpris:trackid" to Variant(DBusPath(trackPath(song.id))),
            "xesam:title" to Variant(song.title),
            "xesam:artist" to Variant(song.artists.map { it.name }, "as"),
        )
        if (song.thumbnail.startsWith("https://") || song.thumbnail.startsWith("http://")) {
            map["mpris:artUrl"] = Variant(song.thumbnail)
        }
        if (durationMs > 0) map["mpris:length"] = Variant(durationMs * 1000L)
        return map
    }

    private fun trackPath(songId: String): String = "/org/mpris/MediaPlayer2/Track/${songId.hashCode().toUInt()}"

    /** The exported object; the bus calls its methods and reads its properties from D-Bus threads. */
    inner class MprisObject : MprisPlayerMethods, Properties {
        override fun getObjectPath() = MPRIS_PATH

        override fun Play() = controls.play()
        override fun Pause() = controls.pause()
        override fun PlayPause() = controls.playPause()
        override fun Stop() = controls.pause()
        override fun Next() = controls.next()
        override fun Previous() = controls.previous()

        override fun Seek(offset: Long) {
            controls.seekTo(latest.positionMs + offset / 1000L)
        }

        override fun SetPosition(trackId: DBusPath, position: Long) {
            val current = latest.song ?: return
            // A stale track id (the song already changed) must not seek the new song.
            if (trackId.path == trackPath(current.id)) controls.seekTo(position / 1000L)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <A> Get(interfaceName: String, propertyName: String): A =
            (GetAll(interfaceName)[propertyName] ?: throw DBusExecutionException("Unknown property")) as A

        override fun <A> Set(interfaceName: String, propertyName: String, value: A) {
            throw DBusExecutionException("Property is read-only")
        }

        override fun GetAll(interfaceName: String): Map<String, Variant<*>> = when (interfaceName) {
            MPRIS_PLAYER -> playerProperties(latest) + mapOf(
                "Position" to Variant(latest.positionMs * 1000L),
                // required player properties; playback speed is fixed
                "Rate" to Variant(1.0),
                "MinimumRate" to Variant(1.0),
                "MaximumRate" to Variant(1.0),
            )
            MPRIS_ROOT -> mapOf(
                "Identity" to Variant("Mochy"),
                "CanQuit" to Variant(false),
                "CanRaise" to Variant(false),
                "HasTrackList" to Variant(false),
                "SupportedUriSchemes" to Variant(emptyList<String>(), "as"),
                "SupportedMimeTypes" to Variant(emptyList<String>(), "as"),
            )
            else -> throw DBusExecutionException("Unknown interface")
        }
    }
}
