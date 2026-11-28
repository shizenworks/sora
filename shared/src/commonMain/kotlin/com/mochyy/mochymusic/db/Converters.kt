package com.mochyy.mochymusic.db

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? =
        if (value != null) {
            localDateTimeFromEpochMillisUtc(value)
        } else {
            null
        }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? = date?.toEpochMillisUtc()
}
