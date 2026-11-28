package com.mochyy.mochymusic.shared.ios

import com.mochyy.mochymusic.shared.library.LocalLibrary
import com.mochyy.mochymusic.shared.playback.PlayerController
import com.mochyy.mochymusic.shared.playback.PlayerState
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObjectProtocol

/** Swift calls this when iOS creates the CarPlay scene, even when the phone UI hasn't opened. */
fun installIosCarPlayBridge() {
    IosCarPlayBridge.install()
}

/** Notification bridge that gives CarPlay access to the same local library and AVPlayer as Compose. */
private object IosCarPlayBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = Json { ignoreUnknownKeys = true }
    private val observers = mutableListOf<NSObjectProtocol>()
    private val requestedLimits = mutableMapOf("saved" to 100, "recent" to 100)
    private var installed = false
    private var library: LocalLibrary? = null
    private var player: PlayerController? = null
    private var savedCollector: Job? = null
    private var recentCollector: Job? = null
    private var playbackCollector: Job? = null
    private var playRequestJob: Job? = null
    private var currentPlayRequestId: String? = null
    private var lastPlaybackKey: Pair<String?, Boolean>? = null

    fun install() {
        if (installed) return
        installed = true
        observe(IosCarPlayEvents.libraryRequest, ::requestLibrary)
        observe(IosCarPlayEvents.playRequest, ::requestPlay)
        // a reconnecting car missed every change made while it was away, so it gets the current state even if unchanged
        observe(IosCarPlayEvents.playbackRequest) { player?.let { publishPlaybackState(it.state.value, force = true) } }
        attachRuntime()
    }

    private fun attachRuntime() {
        val runtime = IosPlaybackRuntime
        val currentLibrary = runtime.library
        val currentPlayer = runtime.player
        // a CarPlay-only launch still restores Up next and moves through it as songs end
        runtime.queue
        library = currentLibrary
        player = currentPlayer

        savedCollector?.cancel()
        recentCollector?.cancel()
        playbackCollector?.cancel()
        savedCollector = scope.launch {
            currentLibrary.saved.collect { publishLibrary("saved", it) }
        }
        recentCollector = scope.launch {
            currentLibrary.recent.collect { publishLibrary("recent", it) }
        }
        playbackCollector = scope.launch {
            currentPlayer.state.collect { state -> publishPlaybackState(state) }
        }
    }

    private fun observe(name: String, handler: (String?) -> Unit) {
        observers += NSNotificationCenter.defaultCenter.addObserverForName(
            name = name,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { notification -> handler(notification?.`object` as? String) }
    }

    private fun requestLibrary(payload: String?) {
        val request = parse(payload) ?: return
        val collection = request["collection"]?.jsonPrimitive?.content ?: return
        val flow = libraryFlow(collection) ?: return
        val limit = (request["limit"]?.jsonPrimitive?.intOrNull ?: 100).coerceIn(1, 500)
        requestedLimits[collection] = limit
        scope.launch { publishLibrary(collection, flow.first()) }
    }

    private fun publishLibrary(collection: String, songs: List<SongItem>) {
        val limit = requestedLimits[collection] ?: 100
        val items = buildJsonArray {
            songs.take(limit).forEach { song ->
                add(buildJsonObject {
                    put("id", song.id)
                    put("title", song.title)
                    put("artist", song.artists.joinToString { it.name })
                    put("isExplicit", song.explicit)
                })
            }
        }
        post(
            IosCarPlayEvents.librarySnapshot,
            buildJsonObject {
                put("collection", collection)
                put("items", items)
            }.toString(),
        )
    }

    private fun requestPlay(payload: String?) {
        val request = parse(payload) ?: return
        val requestId = request["requestId"]?.jsonPrimitive?.content ?: return
        val collection = request["collection"]?.jsonPrimitive?.content ?: return
        val songId = request["id"]?.jsonPrimitive?.content ?: return
        val songs = libraryFlow(collection)
        if (songs == null) {
            publishPlayResult(requestId, ready = false)
            return
        }

        currentPlayRequestId?.let { publishPlayResult(it, ready = false) }
        playRequestJob?.cancel()
        currentPlayRequestId = requestId
        playRequestJob = scope.launch {
            try {
                val song = songs.first().firstOrNull { it.id == songId }
                if (song == null) {
                    publishPlayResult(requestId, ready = false)
                    return@launch
                }

                val activePlayer = player ?: run {
                    publishPlayResult(requestId, ready = false)
                    return@launch
                }
                // through the queue, as a tap on the phone does: a song picked from Up next leaves it
                IosPlaybackRuntime.queue.play(song)
                val result = withTimeoutOrNull(45_000L) {
                    activePlayer.state.first { state ->
                        state.song?.id == songId && state.error != null ||
                            (state.song?.id == songId && state.isPlaying && !state.isLoading)
                    }
                }
                val ready = result?.song?.id == songId && result.isPlaying && !result.isLoading && result.error == null
                publishPlayResult(requestId, ready)
                if (ready) library?.recordPlay(song)
            } finally {
                if (currentPlayRequestId == requestId) currentPlayRequestId = null
            }
        }
    }

    private fun publishPlayResult(requestId: String, ready: Boolean) {
        post(
            IosCarPlayEvents.playResult,
            buildJsonObject {
                put("requestId", requestId)
                put("ready", ready)
            }.toString(),
        )
    }

    private fun publishPlaybackState(state: PlayerState, force: Boolean = false) {
        val key = state.song?.id to state.isPlaying
        if (!force && lastPlaybackKey == key) return
        lastPlaybackKey = key
        post(
            IosCarPlayEvents.playbackChanged,
            buildJsonObject {
                state.song?.id?.let { put("songId", it) }
                put("isPlaying", state.isPlaying)
            }.toString(),
        )
    }

    private fun libraryFlow(collection: String): Flow<List<SongItem>>? = when (collection) {
        "saved" -> library?.saved
        "recent" -> library?.recent
        else -> null
    }

    private fun parse(payload: String?): JsonObject? = payload?.let {
        runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull()
    }

    private fun post(name: String, payload: String) {
        NSNotificationCenter.defaultCenter.postNotificationName(name, payload)
    }
}

private object IosCarPlayEvents {
    private const val prefix = "com.mochyy.mochymusic.ios.carplay."
    const val libraryRequest = "${prefix}libraryRequest"
    const val librarySnapshot = "${prefix}librarySnapshot"
    const val playRequest = "${prefix}playRequest"
    const val playResult = "${prefix}playResult"
    const val playbackChanged = "${prefix}playbackChanged"
    const val playbackRequest = "${prefix}playbackRequest"
}
