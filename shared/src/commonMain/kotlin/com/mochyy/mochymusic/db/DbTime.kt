package com.mochyy.mochymusic.db

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// local wall-clock time, same meaning as java's LocalDateTime.now()
@OptIn(kotlin.time.ExperimentalTime::class)
fun nowLocal(): LocalDateTime = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

// stored columns are UTC epoch millis
fun LocalDateTime.toEpochMillisUtc(): Long = toInstant(TimeZone.UTC).toEpochMilliseconds()

@OptIn(kotlin.time.ExperimentalTime::class)
fun localDateTimeFromEpochMillisUtc(value: Long): LocalDateTime =
    kotlin.time.Instant.fromEpochMilliseconds(value).toLocalDateTime(TimeZone.UTC)

// the current time as epoch millis, same value as java's System.currentTimeMillis()
@OptIn(kotlin.time.ExperimentalTime::class)
fun nowEpochMillis(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
