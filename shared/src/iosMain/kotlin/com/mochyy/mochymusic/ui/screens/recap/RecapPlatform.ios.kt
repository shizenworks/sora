package com.mochyy.mochymusic.ui.screens.recap

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

// No java.time on iOS yet: English names until the platform gets its own formatter. There is no
// earlier iOS behaviour to match here.
private val FullMonthNames = listOf(
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
)

actual fun fullMonthName(month: Int): String = FullMonthNames[month - 1]

private val FullDayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

actual fun fullDayName(isoDay: Int): String = FullDayNames[isoDay - 1]

private fun pad2(value: Int): String = value.toString().padStart(2, '0')

internal actual fun receiptDateStamp(value: LocalDateTime): String = "${pad2(value.day)}/${pad2(value.month.number)}/${value.year}"

internal actual fun receiptTimeStamp(value: LocalDateTime): String = "${pad2(value.hour)}:${pad2(value.minute)}"
