package com.mochyy.mochymusic.ui.screens.recap

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

// Ported from Metrolist (github.com/MetrolistGroup/Metrolist), GPL-3.0:
// shared/src/iosMain/kotlin/com/metrolist/shared/ui/statistics/StatisticsFormatter.ios.kt
@OptIn(ExperimentalForeignApi::class, kotlin.time.ExperimentalTime::class)
internal actual class StatisticsFormatter actual constructor() {
    private val locale = NSLocale.currentLocale

    private fun formatter(template: String) =
        NSDateFormatter().apply {
            this.locale = locale
            setLocalizedDateFormatFromTemplate(template)
        }

    private val months = formatter("LLL")
    private val weekdaysNarrow = formatter("EEEEE")
    private val dates = formatter("EEE, LLL d")
    private val times = formatter("jm")

    actual fun shortMonth(month: Int): String = months.stringFromDate(nativeDate(2000, month, 15))

    actual fun narrowWeekday(isoDay: Int): String = weekdaysNarrow.stringFromDate(nativeDate(2024, 1, isoDay))

    actual fun shortDate(date: LocalDate): String = dates.stringFromDate(nativeDate(date.year, date.month.ordinal + 1, date.day))

    actual fun shortTime(hour: Int, minute: Int): String = times.stringFromDate(nativeDate(2000, 1, 1, hour, minute))

    private fun nativeDate(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): NSDate =
        NSDate.dateWithTimeIntervalSince1970(
            LocalDateTime(year, month, day, hour, minute)
                .toInstant(TimeZone.currentSystemDefault())
                .epochSeconds
                .toDouble(),
        )
}
