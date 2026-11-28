package com.mochyy.mochymusic.ui.screens.recap

import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

// the same java.time calls the Stats page made while it lived in :app, so every language's month,
// weekday and clock text is unchanged
internal actual class StatisticsFormatter actual constructor() {
    private val locale: Locale get() = Locale.getDefault()
    private val shortTime: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    actual fun shortMonth(month: Int): String =
        Month.of(month).getDisplayName(JavaTextStyle.SHORT, locale)

    actual fun narrowWeekday(isoDay: Int): String =
        DayOfWeek.of(isoDay).getDisplayName(JavaTextStyle.NARROW, locale)

    actual fun shortDate(date: LocalDate): String =
        "${DayOfWeek.of(date.dayOfWeek.isoDayNumber).getDisplayName(JavaTextStyle.SHORT, locale)}, " +
            "${shortMonth(date.month.ordinal + 1)} ${date.day}"

    actual fun shortTime(hour: Int, minute: Int): String =
        LocalTime.of(hour, minute).format(shortTime)
}
