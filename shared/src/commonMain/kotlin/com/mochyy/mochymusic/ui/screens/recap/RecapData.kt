// RecapData.kt
//
// Everything the monthly recap and the Stats page show, worked out from the play history alone
// (the `event` table: one row per play of 30 s or more). Nothing here leaves the device.
//
// Days are kotlinx-datetime's; the wall clock is read exactly as before: the event table stores
// LocalDateTime.now() as if it were UTC, so a play's time is the stored millis read back in UTC,
// never through the device's zone. "Today" is the device's own calendar day.

package com.mochyy.mochymusic.ui.screens.recap

import com.mochyy.mochymusic.utils.ioDispatcher
import androidx.compose.runtime.Immutable
import com.mochyy.mochymusic.db.MochyDatabase
import com.mochyy.mochymusic.db.RecapEventRow
import com.mochyy.mochymusic.db.entities.Song
import com.mochyy.mochymusic.db.localDateTimeFromEpochMillisUtc
import com.mochyy.mochymusic.db.nowLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt

/** The recap opens once a month has this many different songs in it. */
internal const val RecapMinSongs = 10

/** ...and not before this day of the month: on the 1st and 2nd the previous month is shown instead. */
internal const val RecapFirstDay = 3

@Immutable
data class StatsRange(val kind: Kind, val year: Int, val month: Int = 1) {
    enum class Kind { Month, Year }

    val start: LocalDate get() = if (kind == Kind.Month) LocalDate(year, month, 1) else LocalDate(year, 1, 1)
    val endExclusive: LocalDate get() = if (kind == Kind.Month) start.plus(1, DateTimeUnit.MONTH) else start.plus(1, DateTimeUnit.YEAR)

    /** "October" in the device's language, the same full name java.time gave it. */
    val monthName: String get() = fullMonthName(month)

    /** What the recap calls the period: "October", or "2026" for a whole year. */
    val label: String get() = if (kind == Kind.Month) monthName else "$year"

    fun previous(): StatsRange =
        if (kind == Kind.Month) monthOf(start.minus(1, DateTimeUnit.MONTH)) else StatsRange(Kind.Year, year - 1)

    /** Days in the month, or 12 for a year: how many buckets [ListeningStats.bucketMs] has. */
    val bucketCount: Int get() = if (kind == Kind.Month) start.daysInMonth() else 12

    companion object {
        fun monthOf(date: LocalDate) = StatsRange(Kind.Month, date.year, date.month.calendarNumber)
        fun yearOf(date: LocalDate) = StatsRange(Kind.Year, date.year)
    }
}

@Immutable
data class RecapSong(
    val id: String,
    val title: String,
    val artistName: String,
    val artistThumbnailUrl: String?,
    val thumbnailUrl: String?,
    val playTimeMs: Long,
    val plays: Int,
)

@Immutable
data class RecapArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val playTimeMs: Long,
    val plays: Int,
    /** The song by this artist played the most in the period. */
    val topSong: RecapSong?,
)

// the one day a single song got played the most, if it's at least a few times
@Immutable
data class RepeatDay(val song: RecapSong, val date: LocalDate, val plays: Int)

// when and how steadily the listening happened, for the habits card
@Immutable
data class ListeningHabits(
    val activeDays: Int,
    val elapsedDays: Int,
    val longestStreak: Int,
    val biggestDay: LocalDate?,
    val biggestDayMs: Long,
    val longestSessionMs: Long,
    val longestSessionStart: LocalDateTime?,
)

@Immutable
class ListeningStats(
    val range: StatsRange,
    val totalMs: Long,
    val plays: Int,
    val songCount: Int,
    val artistCount: Int,
    val albumCount: Int,
    /** Most played first (by plays, then time), up to [MaxSongs]. */
    val topSongs: List<RecapSong>,
    /** Most listened first (by time), up to [MaxArtists]. */
    val topArtists: List<RecapArtist>,
    /** Listening time per weekday, Monday first. */
    val weekdayMs: List<Long>,
    /** Listening time per hour of the day, 0..23. */
    val hourMs: List<Long>,
    /** Listening time per day of the month, or per month of the year. */
    val bucketMs: List<Long>,
    val habits: ListeningHabits? = null,
    // songs never played before this period, best first, with how many there were and how long they played
    val newSongs: List<RecapSong> = emptyList(),
    val newSongCount: Int = 0,
    val newMs: Long = 0L,
    val repeat: RepeatDay? = null,
    // the period before this one, null when nothing was played then; partial means only up to the same day
    val previousTotalMs: Long? = null,
    val previousIsPartial: Boolean = false,
) {
    val isEmpty: Boolean get() = plays == 0

    companion object {
        const val MaxSongs = 20
        const val MaxArtists = 25
    }
}

/** One artist's share of the period's listening, for the Stats page's genre card. */
@Immutable
data class GenreShare(val name: String, val fraction: Float, val playTimeMs: Long)

/** What the recap card's barcode leads to once it's scanned. */
sealed interface RecapAvailability {
    data class Ready(val stats: ListeningStats, val monthProgressPercent: Int) : RecapAvailability

    /** Not unlocked yet; [comeBack] finishes "come back later ...". */
    data class NotYet(val comeBack: String) : RecapAvailability
}

/**
 * The history queries the recap and Stats page need, and the aggregation over their rows.
 *
 * The app still exposes RecapRepository because settings/ProfileScreen and the TV pages hand it
 * java.time.LocalDate; that adapter converts at the boundary and calls this. Everything past the
 * boundary is here, so another platform only needs a MochyDatabase.
 */
object RecapStats {

    suspend fun load(db: MochyDatabase, range: StatsRange): ListeningStats = withContext(ioDispatcher) {
        val from = range.start.toStoredMillis()
        val events = db.historyDao.recapEvents(from, range.endExclusive.toStoredMillis())
        val ids = events.mapTo(LinkedHashSet()) { it.songId }.toList()
        val songs = ids.chunked(500).flatMap { db.songDao.getSongsByIds(it) }.associateBy { it.id }
        val playedBefore = ids.chunked(500).flatMapTo(HashSet()) { db.historyDao.songsPlayedBefore(it, from) }
        val today = nowLocal().date
        val previous = range.previous()
        // a period still running is held against the same stretch of the one before, or every month would look like a slump
        val running = today >= range.start && today < range.endExclusive
        val previousEnd =
            if (running) {
                previous.start.plus(today.toEpochDays() - range.start.toEpochDays() + 1, DateTimeUnit.DAY)
                    .coerceAtMost(range.start)
            } else {
                range.start
            }
        val previousTotal = db.historyDao.listeningTotal(previous.start.toStoredMillis(), previousEnd.toStoredMillis()).takeIf { it > 0 }
        withContext(Dispatchers.Default) {
            aggregate(range, events, songs, playedBefore, previousTotal, running, today)
        }
    }

    /**
     * The month the recap is about today, or why there isn't one yet. [force] (Experimental) opens
     * it anyway with whatever has been played: this month, else last month, else this year.
     */
    suspend fun recapFor(db: MochyDatabase, today: LocalDate, force: Boolean = false): RecapAvailability {
        val normal = recapByTheRules(db, today)
        if (!force || normal is RecapAvailability.Ready) return normal
        val candidates = listOf(StatsRange.monthOf(today), StatsRange.monthOf(today.minus(1, DateTimeUnit.MONTH)), StatsRange.yearOf(today))
        for (range in candidates) {
            val stats = load(db, range)
            if (stats.songCount > 0) return RecapAvailability.Ready(stats, progressOf(range, today))
        }
        return RecapAvailability.NotYet("once you've played a song")
    }

    private fun progressOf(range: StatsRange, today: LocalDate): Int = when {
        range.kind == StatsRange.Kind.Year && range.year == today.year ->
            (today.dayOfYear * 100f / today.daysInYear()).roundToInt().coerceIn(1, 100)
        range.kind == StatsRange.Kind.Month && range.year == today.year && range.month == today.month.calendarNumber ->
            (today.day * 100f / today.daysInMonth()).roundToInt().coerceIn(1, 100)
        else -> 100
    }

    private suspend fun recapByTheRules(db: MochyDatabase, today: LocalDate): RecapAvailability {
        val thisMonth = StatsRange.monthOf(today)
        if (today.day < RecapFirstDay) {
            val previous = StatsRange.monthOf(today.minus(1, DateTimeUnit.MONTH))
            val stats = load(db, previous)
            if (stats.songCount >= RecapMinSongs) return RecapAvailability.Ready(stats, 100)
            val unlock = LocalDate(today.year, today.month, RecapFirstDay)
            return RecapAvailability.NotYet(
                "on ${fullMonthName(unlock.month.calendarNumber)} $RecapFirstDay, " +
                    "once you've played $RecapMinSongs songs",
            )
        }
        val stats = load(db, thisMonth)
        if (stats.songCount >= RecapMinSongs) {
            val percent = (today.day * 100f / today.daysInMonth()).roundToInt().coerceIn(1, 100)
            return RecapAvailability.Ready(stats, percent)
        }
        val missing = RecapMinSongs - stats.songCount
        return RecapAvailability.NotYet(
            "when you've played $missing more ${if (missing == 1) "song" else "songs"} this month",
        )
    }

    /** First day anything was played, for the Stats timeline and as a fallback join date. */
    suspend fun firstPlayDate(db: MochyDatabase): LocalDate? = withContext(ioDispatcher) {
        db.historyDao.firstEventTimestamp()?.let { storedMillisToDateTime(it).date }
    }

    private fun aggregate(
        range: StatsRange,
        events: List<RecapEventRow>,
        songs: Map<String, Song>,
        playedBefore: Set<String>,
        previousTotalMs: Long?,
        previousIsPartial: Boolean,
        today: LocalDate,
    ): ListeningStats {
        class Acc(var ms: Long = 0L, var plays: Int = 0)

        val perSong = HashMap<String, Acc>()
        val weekday = LongArray(7)
        val hour = LongArray(24)
        val buckets = LongArray(range.bucketCount)
        var total = 0L
        for (e in events) {
            val ms = e.playTime.coerceAtLeast(0L)
            total += ms
            perSong.getOrPut(e.songId) { Acc() }.apply {
                this.ms += ms
                plays++
            }
            val at = storedMillisToDateTime(e.timestamp)
            weekday[at.dayOfWeek.isoDayNumber - 1] += ms
            hour[at.hour] += ms
            val bucket = if (range.kind == StatsRange.Kind.Month) at.day - 1 else at.month.calendarNumber - 1
            if (bucket in buckets.indices) buckets[bucket] += ms
        }

        fun recapSong(id: String, acc: Acc): RecapSong? {
            val song = songs[id] ?: return null
            return RecapSong(
                id = id,
                title = song.song.title,
                artistName = song.artists.joinToString(", ") { it.name }.ifBlank { song.song.albumName ?: "Unknown artist" },
                artistThumbnailUrl = song.artists.firstNotNullOfOrNull { it.thumbnailUrl },
                thumbnailUrl = song.song.thumbnailUrl,
                playTimeMs = acc.ms,
                plays = acc.plays,
            )
        }

        val songOrder = compareByDescending<RecapSong> { it.plays }.thenByDescending { it.playTimeMs }
        val allSongs = perSong.mapNotNull { (id, acc) -> recapSong(id, acc) }.sortedWith(songOrder)

        // one artist can come back under a few ids (a channel, a topic channel, a local tag), so
        // they're merged by name - otherwise the same person could take two places in the top 10
        class ArtistAcc(val id: String, val name: String, var thumb: String?) {
            var ms = 0L
            var plays = 0
            val songs = ArrayList<RecapSong>()
        }

        val perArtist = LinkedHashMap<String, ArtistAcc>()
        val albums = HashSet<String>()
        for (s in allSongs) {
            val song = songs[s.id] ?: continue
            song.song.albumId?.let { albums += it }
            song.artists.distinctBy { it.name.trim().lowercase() }.forEach { artist ->
                val key = artist.name.trim().lowercase()
                if (key.isEmpty()) return@forEach
                val acc = perArtist.getOrPut(key) { ArtistAcc(artist.id, artist.name.trim(), artist.thumbnailUrl) }
                if (acc.thumb == null) acc.thumb = artist.thumbnailUrl
                acc.ms += s.playTimeMs
                acc.plays += s.plays
                acc.songs += s
            }
        }
        val topArtists = perArtist.values
            .sortedByDescending { it.ms }
            .take(ListeningStats.MaxArtists)
            .map { a ->
                val top = a.songs.sortedWith(songOrder).firstOrNull()
                RecapArtist(
                    id = a.id,
                    name = a.name,
                    thumbnailUrl = a.thumb ?: top?.thumbnailUrl,
                    playTimeMs = a.ms,
                    plays = a.plays,
                    topSong = top,
                )
            }

        return ListeningStats(
            range = range,
            totalMs = total,
            plays = events.size,
            songCount = perSong.size,
            artistCount = perArtist.size,
            albumCount = albums.size,
            topSongs = allSongs.take(ListeningStats.MaxSongs),
            topArtists = topArtists,
            weekdayMs = weekday.toList(),
            hourMs = hour.toList(),
            bucketMs = buckets.toList(),
        ).withExtras(events, allSongs.associateBy { it.id }, playedBefore, previousTotalMs, previousIsPartial, today)
    }
}

// the stats past the basics: habits, new finds, the song on repeat and last period, all from the same plays
private fun ListeningStats.withExtras(
    events: List<RecapEventRow>,
    songsById: Map<String, RecapSong>,
    playedBefore: Set<String>,
    previousTotalMs: Long?,
    previousIsPartial: Boolean,
    today: LocalDate,
): ListeningStats {
    if (events.isEmpty()) return this
    val dayMs = HashMap<LocalDate, Long>()
    val daySongPlays = HashMap<LocalDate, HashMap<String, Int>>()
    val sorted = events.sortedBy { it.timestamp }
    for (e in sorted) {
        val day = storedMillisToDateTime(e.timestamp).date
        dayMs[day] = (dayMs[day] ?: 0L) + e.playTime.coerceAtLeast(0L)
        val counts = daySongPlays.getOrPut(day) { HashMap() }
        counts[e.songId] = (counts[e.songId] ?: 0) + 1
    }

    val habits = habitsOf(sorted, dayMs, today)

    // a song counts as new when this period holds its very first play; songsById is already best first
    val newSongs = songsById.values.filter { it.id !in playedBefore }

    var repeat: RepeatDay? = null
    for ((day, counts) in daySongPlays) {
        for ((id, count) in counts) {
            if (count < 3 || count <= (repeat?.plays ?: 0)) continue
            val song = songsById[id] ?: continue
            repeat = RepeatDay(song, day, count)
        }
    }

    return ListeningStats(
        range = range,
        totalMs = totalMs,
        plays = plays,
        songCount = songCount,
        artistCount = artistCount,
        albumCount = albumCount,
        topSongs = topSongs,
        topArtists = topArtists,
        weekdayMs = weekdayMs,
        hourMs = hourMs,
        bucketMs = bucketMs,
        habits = habits,
        newSongs = newSongs.take(10),
        newSongCount = newSongs.size,
        newMs = newSongs.sumOf { it.playTimeMs },
        repeat = repeat,
        previousTotalMs = previousTotalMs,
        previousIsPartial = previousIsPartial,
    )
}

// plays closer than this (on top of the song itself) count as one sitting
private const val SessionGapMs = 10 * 60_000L

private fun ListeningStats.habitsOf(sorted: List<RecapEventRow>, dayMs: Map<LocalDate, Long>, today: LocalDate): ListeningHabits {
    val lastDay = minOf(range.endExclusive.minus(1, DateTimeUnit.DAY), maxOf(today, range.start))
    val elapsed = (lastDay.toEpochDays() - range.start.toEpochDays() + 1).toInt().coerceAtLeast(1)

    var streak = 0
    var best = 0
    var previous: LocalDate? = null
    for (day in dayMs.keys.sorted()) {
        streak = if (previous != null && day == previous.plus(1, DateTimeUnit.DAY)) streak + 1 else 1
        best = maxOf(best, streak)
        previous = day
    }

    // the stored time may be the start or the end of a play, so the gap allows for the longer of the two songs
    var sessionMs = 0L
    var sessionStart = sorted.first().timestamp
    var longestMs = 0L
    var longestStart = sessionStart
    for ((i, e) in sorted.withIndex()) {
        if (i > 0) {
            val prev = sorted[i - 1]
            val gap = e.timestamp - prev.timestamp
            if (gap > maxOf(prev.playTime, e.playTime) + SessionGapMs) {
                sessionMs = 0L
                sessionStart = e.timestamp
            }
        }
        sessionMs += e.playTime.coerceAtLeast(0L)
        if (sessionMs > longestMs) {
            longestMs = sessionMs
            longestStart = sessionStart
        }
    }

    val biggest = dayMs.maxByOrNull { it.value }
    return ListeningHabits(
        activeDays = dayMs.count { it.value > 0 },
        elapsedDays = elapsed,
        longestStreak = best,
        biggestDay = biggest?.key,
        biggestDayMs = biggest?.value ?: 0L,
        longestSessionMs = longestMs,
        longestSessionStart = storedMillisToDateTime(longestStart),
    )
}

/** Start of [this] day as the millis the event table stores: the wall clock read as if it were UTC. */
@OptIn(kotlin.time.ExperimentalTime::class)
fun LocalDate.toStoredMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

/**
 * The wall clock of a stored play: the millis read back as UTC, cut to the second the way java.time's
 * ofEpochSecond(floorDiv(millis, 1000), 0, UTC) did, so every derived day and hour is unchanged.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
fun storedMillisToDateTime(millis: Long): LocalDateTime {
    val local = localDateTimeFromEpochMillisUtc(millis)
    return LocalDateTime(local.year, local.month, local.day, local.hour, local.minute, local.second)
}

/** "12h 05m" style, the way the recap and Stats spell listening time. */
fun formatHoursMinutes(ms: Long): String {
    val minutes = (ms / 60_000L).coerceAtLeast(0L)
    return "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m"
}

/** Hours with one decimal ("3.4"), for the receipt and the Stats rows. */
fun formatHoursDecimal(ms: Long): String {
    val tenths = (ms / 360_000L).coerceAtLeast(0L)
    return "${tenths / 10}.${tenths % 10}"
}

// kotlinx-datetime 0.8 has the Month(number) factory but no number on Month; the enum is declared
// in calendar order, so its ordinal is the number java.time's Month.getValue() gave.
private val Month.calendarNumber: Int get() = ordinal + 1

// java.time had lengthOfMonth()/lengthOfYear(); these are the same counts for the same day, without
// the month-end clamping plusMonths would do.
private fun LocalDate.daysInMonth(): Int {
    val first = LocalDate(year, month, 1)
    return (first.plus(1, DateTimeUnit.MONTH).toEpochDays() - first.toEpochDays()).toInt()
}

private fun LocalDate.daysInYear(): Int = (LocalDate(year + 1, 1, 1).toEpochDays() - LocalDate(year, 1, 1).toEpochDays()).toInt()
