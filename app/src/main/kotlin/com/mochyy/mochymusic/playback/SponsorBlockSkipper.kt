package com.mochyy.mochymusic.playback

import android.content.Context
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import com.mochyy.mochymusic.constants.SponsorBlockCategoriesKey
import com.mochyy.mochymusic.constants.SponsorBlockDefaultCategories
import com.mochyy.mochymusic.constants.SponsorBlockEnabledKey
import com.mochyy.mochymusic.utils.get
import com.mochyy.mochymusic.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The SponsorBlock categories Mochy knows, by their API names. All are looked up; the user picks which to skip. */
val SponsorBlockCategoryNames = listOf("music_offtopic", "sponsor", "selfpromo", "intro", "outro")

private data class SponsorSegment(val category: String, val startMs: Long, val endMs: Long)

/**
 * Skips the parts of a YouTube song that SponsorBlock users marked as not music (sponsor reads,
 * off-topic intros and so on). The segments are looked up once when a song starts, cached per
 * video id, and each skip is a single player message set for the segment's start. Nothing polls.
 */
class SponsorBlockSkipper(private val context: Context) {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // the last few songs looked up, so a queue that loops back doesn't ask again
    private val cache = object : LinkedHashMap<String, List<SponsorSegment>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<SponsorSegment>>?) = size > CACHE_SIZE
    }

    private var lookup: Job? = null
    private var scheduled: List<PlayerMessage> = emptyList()
    private var currentId: String? = null

    /** Called when a new song starts (or repeats). Looks up its segments and schedules the skips. */
    fun onItemChanged(player: ExoPlayer, mediaItem: MediaItem?, scope: CoroutineScope) {
        cancel()
        val id = mediaItem?.mediaId ?: return
        // local files and anything that isn't a YouTube video id have no segments
        if (!YOUTUBE_ID.matches(id)) return
        currentId = id
        lookup = scope.launch(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            if (prefs[SponsorBlockEnabledKey] != true) return@launch
            val segments = segmentsFor(id) ?: return@launch
            withContext(Dispatchers.Main) {
                // the song may have changed while the lookup was out
                if (currentId != id || player.currentMediaItem?.mediaId != id) return@withContext
                // every category is scheduled; each skip checks the switches when it's reached
                schedule(player, segments)
            }
        }
    }

    /** Drops the pending skips, e.g. when the song changes. */
    fun cancel() {
        lookup?.cancel()
        lookup = null
        currentId = null
        scheduled.forEach { runCatching { it.cancel() } }
        scheduled = emptyList()
    }

    // one message per segment, at its start; delivered on the main thread as playback reaches it
    private fun schedule(player: ExoPlayer, segments: List<SponsorSegment>) {
        val index = player.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return
        val duration = player.duration
        scheduled = segments
            .filter { it.endMs > it.startMs && (duration == C.TIME_UNSET || it.endMs < duration - TAIL_MS) }
            .map { segment ->
                player.createMessage { _, _ -> if (wantsSkip(segment)) player.seekTo(segment.endMs) }
                    .setPosition(index, segment.startMs)
                    .setLooper(Looper.getMainLooper())
                    .setDeleteAfterDelivery(true)
                    .send()
            }
    }

    // read when the skip is due, not when the song started, so switching SponsorBlock or a category off (or a
    // category on) partway through a song counts; the settings cache answers this without touching disk
    private fun wantsSkip(segment: SponsorSegment): Boolean =
        context.dataStore.get(SponsorBlockEnabledKey, false) &&
            segment.category in context.dataStore.get(SponsorBlockCategoriesKey, SponsorBlockDefaultCategories)

    // cached result, or one request for all categories (so changing the picks needs no new lookup)
    private fun segmentsFor(id: String): List<SponsorSegment>? {
        synchronized(cache) { cache[id] }?.let { return it }
        val segments = fetch(id) ?: return null
        synchronized(cache) { cache[id] = segments }
        return segments
    }

    /** Null when the request failed, so a later play tries again. A song with no segments is an empty list. */
    private fun fetch(id: String): List<SponsorSegment>? {
        val url = "https://sponsor.ajay.app/api/skipSegments".toHttpUrl().newBuilder()
            .addQueryParameter("videoID", id)
            .addQueryParameter("categories", SponsorBlockCategoryNames.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\""))
            .build()
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use<okhttp3.Response, List<SponsorSegment>?> { response ->
                // 404 is the service's way of saying nothing is marked for this video
                if (response.code == 404) return@use emptyList()
                if (!response.isSuccessful) return@use null
                parse(response.body?.string().orEmpty())
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun parse(body: String): List<SponsorSegment> = try {
        val array = JSONArray(body)
        (0 until array.length()).mapNotNull { i ->
            val item = array.getJSONObject(i)
            if (item.optString("actionType") != "skip") return@mapNotNull null
            val range = item.getJSONArray("segment")
            val startMs = (range.getDouble(0) * 1000).toLong()
            val endMs = (range.getDouble(1) * 1000).toLong()
            SponsorSegment(item.optString("category"), startMs, endMs)
        }
    } catch (_: org.json.JSONException) {
        emptyList()
    }

    private companion object {
        const val CACHE_SIZE = 64
        // a segment ending this close to the end of the song is left to play out, a seek could overshoot
        const val TAIL_MS = 300L
        val YOUTUBE_ID = Regex("[A-Za-z0-9_-]{11}")
    }
}
