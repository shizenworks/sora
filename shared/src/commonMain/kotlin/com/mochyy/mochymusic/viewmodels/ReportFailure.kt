package com.mochyy.mochymusic.viewmodels

/**
 * The move's stand-in for the app's reportException: same stack text in the process output, without
 * android.util.Log or Timber, so shared ViewModels fail exactly as loudly on every platform.
 */
internal fun reportFailure(throwable: Throwable) {
    println(throwable.stackTraceToString())
}
