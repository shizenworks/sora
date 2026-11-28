package com.mochyy.mochymusic.ui.screens.recap

import kotlinx.datetime.LocalDateTime

/**
 * The device language's full month name ("October"), exactly the text
 * java.time's Month.getDisplayName(TextStyle.FULL, Locale.getDefault()) gave the recap and the
 * Stats page before they moved.
 */
expect fun fullMonthName(month: Int): String

/** The device language's full weekday name ("Monday") for ISO day 1..7, as java.time spelled it. */
expect fun fullDayName(isoDay: Int): String

/** The recap receipt's stamp, spelled as java.time's ofPattern("dd/MM/yyyy") spelled it. */
internal expect fun receiptDateStamp(value: LocalDateTime): String

/** ...and as ofPattern("HH:mm") spelled it. */
internal expect fun receiptTimeStamp(value: LocalDateTime): String
