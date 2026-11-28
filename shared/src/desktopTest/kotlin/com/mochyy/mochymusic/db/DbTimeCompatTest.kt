package com.mochyy.mochymusic.db

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Data already on phones was written with java.time. These pin the switch to kotlinx-datetime:
 * the same epoch millis in the database, and every text form java's toString() wrote into the
 * sync undo journal and the liked-songs memory still reads back as the same moment.
 */
class DbTimeCompatTest {
    private val samples = listOf(
        java.time.LocalDateTime.of(2026, 11, 22, 13, 5),
        java.time.LocalDateTime.of(2026, 11, 22, 13, 5, 1),
        java.time.LocalDateTime.of(2026, 11, 22, 13, 5, 1, 123_000_000),
        java.time.LocalDateTime.of(2026, 11, 22, 13, 5, 1, 123_456_000),
        java.time.LocalDateTime.of(2026, 11, 22, 13, 5, 1, 123_456_789),
        java.time.LocalDateTime.of(1999, 1, 1, 0, 0),
    )

    @Test
    fun readsEveryTextFormJavaWrote() {
        for (old in samples) {
            assertEquals(old, LocalDateTime.parse(old.toString()).toJavaLocalDateTime(), old.toString())
        }
    }

    @Test
    fun javaReadsWhatKotlinxWrites() {
        for (old in samples) {
            assertEquals(old, java.time.LocalDateTime.parse(old.toKotlinLocalDateTime().toString()))
        }
    }

    @Test
    fun storesTheSameMillisAsBefore() {
        for (old in samples) {
            val before = old.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
            assertEquals(before, old.toKotlinLocalDateTime().toEpochMillisUtc())
            assertEquals(
                java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(before), ZoneOffset.UTC),
                localDateTimeFromEpochMillisUtc(before).toJavaLocalDateTime(),
            )
        }
    }
}
