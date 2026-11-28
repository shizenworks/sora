package com.mochyy.mochymusic.viewmodels

import com.mochyy.mochymusic.importer.ParsedImport

sealed interface PendingImport {
    data object Idle : PendingImport
    data class Loading(val label: String) : PendingImport
    data class Ready(val parsed: ParsedImport, val sourceLabel: String) : PendingImport
    data class Failed(val message: String) : PendingImport
}
