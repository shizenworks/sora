package com.music.innertube.utils

// The text helpers of Utils.kt that need no JVM, kept in common code so every platform parses the same way.

fun parseCookieString(cookie: String): Map<String, String> =
    cookie.split(';')
        .mapNotNull { part ->
            val trimmed = part.trim()
            val splitIndex = trimmed.indexOf('=')
            if (splitIndex <= 0) null
            else trimmed.substring(0, splitIndex).trim() to trimmed.substring(splitIndex + 1).trim()
        }
        .toMap()

// some languages write durations with a dot instead of a colon (indonesian "3.45", finnish, danish), so either separator is accepted
private val DURATION_REGEX = Regex("^(?:(\\p{Nd}+)[:.])?(\\p{Nd}+)[:.](\\p{Nd}{2})$")

fun String.parseTime(): Int? {
    val (hours, minutes, seconds) = DURATION_REGEX.matchEntire(trim())?.destructured ?: return null
    return try {
        (hours.toIntOrNull() ?: 0) * 3600 + minutes.toInt() * 60 + seconds.toInt()
    } catch (e: NumberFormatException) {
        null
    }
}

fun isPrivateId(browseId: String): Boolean {
    return browseId.contains("privately")
}
