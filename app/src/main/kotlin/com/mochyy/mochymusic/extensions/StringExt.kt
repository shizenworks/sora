// StringExt.kt

package com.mochyy.mochymusic.extensions

import androidx.sqlite.db.SimpleSQLiteQuery

// String?.toEnum lives in :shared (EnumExt.kt), same package

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)
