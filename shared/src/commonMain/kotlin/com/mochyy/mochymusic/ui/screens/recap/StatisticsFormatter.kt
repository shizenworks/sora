package com.mochyy.mochymusic.ui.screens.recap

import kotlinx.datetime.LocalDate

/**
 * The Stats page's dates and day names in the device's language. Android and desktop answer with the
 * exact java.time calls the page made before it moved; iOS has no earlier Stats page to match, so it
 * uses the platform formatter.
 */
internal expect class StatisticsFormatter() {
    /** "Nov" - java.time's TextStyle.SHORT month name. */
    fun shortMonth(month: Int): String

    /** "W" - the one-letter weekday for the weekday bars, Monday = 1. */
    fun narrowWeekday(isoDay: Int): String

    /** "wed, nov 19" - the way the charts' tooltips name a day. */
    fun shortDate(date: LocalDate): String

    /** "9:05 AM" - java.time's localized short time for [hour]:[minute]. */
    fun shortTime(hour: Int, minute: Int): String
}
