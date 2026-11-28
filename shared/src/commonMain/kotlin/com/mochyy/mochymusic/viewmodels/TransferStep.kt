package com.mochyy.mochymusic.viewmodels

/** where an export or import stands, progress is null while there's no number to show yet */
sealed interface TransferStep {
    data object Idle : TransferStep
    data class Working(val progress: Float?) : TransferStep
    data object Done : TransferStep
    data class Failed(val message: String) : TransferStep
}
