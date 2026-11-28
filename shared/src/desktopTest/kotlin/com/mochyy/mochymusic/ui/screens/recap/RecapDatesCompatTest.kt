package com.mochyy.mochymusic.ui.screens.recap

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The recap dates switched from java.time to kotlinx-datetime when the statistics moved to shared.
 * These pin the two conversions that decide which day a play belongs to, and the month/day names the
 * screens show, against exactly what java.time produced before.
 */
class RecapDatesCompatTest {
    @Test
    fun startOfDayIsTheSameStoredMillis() {
        val dates = listOf(
            LocalDate(1999, 12, 31),
            LocalDate(2024, 1, 1),
            LocalDate(2026, 11, 23),
            LocalDate(2028, 2, 29),
        )
        for (date in dates) {
            val java = date.toJavaLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            assertEquals(java, date.toStoredMillis(), date.toString())
        }
    }

    @Test
    fun storedMillisReadBackAsJavaReadThem() {
        // including sub-second and pre-epoch values: java's ofEpochSecond(floorDiv(millis, 1000), 0, UTC)
        val samples = listOf(0L, 1L, 999L, 1_000L, 1_700_000_000_123L, 1_700_000_000_000L, -1L, -1500L)
        for (millis in samples) {
            val java = java.time.LocalDateTime.ofEpochSecond(Math.floorDiv(millis, 1000L), 0, ZoneOffset.UTC)
            assertEquals(java, storedMillisToDateTime(millis).toJavaLocalDateTime(), millis.toString())
        }
    }

    @Test
    fun monthAndDayNamesAreJavaNames() {
        for (month in 1..12) {
            assertEquals(
                java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()),
                fullMonthName(month),
                month.toString(),
            )
        }
        for (day in 1..7) {
            assertEquals(
                java.time.DayOfWeek.of(day).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()),
                fullDayName(day),
                day.toString(),
            )
        }
    }

    @Test
    fun bucketCountsAreTheLengthsJavaUsed() {
        assertEquals(29, StatsRange(StatsRange.Kind.Month, 2024, 2).bucketCount)
        assertEquals(28, StatsRange(StatsRange.Kind.Month, 2026, 2).bucketCount)
        assertEquals(31, StatsRange(StatsRange.Kind.Month, 2026, 12).bucketCount)
        assertEquals(12, StatsRange(StatsRange.Kind.Year, 2026).bucketCount)
    }
}
