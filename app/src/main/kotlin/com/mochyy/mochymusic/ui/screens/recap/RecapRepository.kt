// RecapRepository.kt
//
// The app side of the recap dates. The statistics themselves live in shared (RecapStats); this keeps
// the java.time entry points settings/ProfileScreen and the TV pages already call, so they hand in and
// get back the same java.time.LocalDate as before. Delete this adapter when those callers move.

package com.mochyy.mochymusic.ui.screens.recap

import com.mochyy.mochymusic.db.MusicDatabase
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

internal object RecapRepository {
    suspend fun load(db: MusicDatabase, range: StatsRange): ListeningStats = RecapStats.load(db, range)

    suspend fun recapFor(db: MusicDatabase, today: LocalDate, force: Boolean = false): RecapAvailability =
        RecapStats.recapFor(db, today.toKotlinLocalDate(), force)

    /** First day anything was played, for the Stats timeline and as a fallback join date. */
    suspend fun firstPlayDate(db: MusicDatabase): LocalDate? = RecapStats.firstPlayDate(db)?.toJavaLocalDate()
}

// The event table stores LocalDateTime.now() as if it were UTC (see Converters), so the wall clock
// of a play comes back by reading the millis as UTC - never through the device's zone.
internal fun LocalDate.toStoredMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

internal fun DayOfWeek.displayName(): String = getDisplayName(TextStyle.FULL, Locale.getDefault())

/** Morning / afternoon / evening / night, for the hour a peak falls in. */
internal fun timeOfDayName(hour: Int): String = when (hour) {
    in 5..11 -> "mornings"
    in 12..16 -> "afternoons"
    in 17..21 -> "evenings"
    else -> "nights"
}
