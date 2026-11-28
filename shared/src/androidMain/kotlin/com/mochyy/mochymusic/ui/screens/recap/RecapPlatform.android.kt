package com.mochyy.mochymusic.ui.screens.recap

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.DayOfWeek
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// the same call the screens made before they moved, so every language reads exactly as it did
actual fun fullMonthName(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())

actual fun fullDayName(isoDay: Int): String =
    DayOfWeek.of(isoDay).getDisplayName(TextStyle.FULL, Locale.getDefault())

internal actual fun receiptDateStamp(value: LocalDateTime): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy").format(value.toJavaLocalDateTime())

internal actual fun receiptTimeStamp(value: LocalDateTime): String =
    DateTimeFormatter.ofPattern("HH:mm").format(value.toJavaLocalDateTime())
