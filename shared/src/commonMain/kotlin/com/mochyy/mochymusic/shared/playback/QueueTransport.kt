package com.mochyy.mochymusic.shared.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lets an operating-system transport request actions from the shared UI's queue owner without
 * copying that queue into the platform player. The UI unregisters its callbacks when disposed.
 * Hosts call [next] and [previous] on the UI dispatcher that owns that queue.
 */
class QueueTransport {
    private val mutableCanGoNext = MutableStateFlow(false)
    private var binding = 0L
    private var nextHandler: (() -> Unit)? = null
    private var previousHandler: (() -> Unit)? = null

    val canGoNext: StateFlow<Boolean> = mutableCanGoNext.asStateFlow()

    fun bind(onNext: () -> Unit, onPrevious: () -> Unit): () -> Unit {
        val token = ++binding
        nextHandler = onNext
        previousHandler = onPrevious
        return {
            if (binding == token) {
                nextHandler = null
                previousHandler = null
                mutableCanGoNext.value = false
            }
        }
    }

    fun setCanGoNext(available: Boolean) {
        mutableCanGoNext.value = available
    }

    fun next(): Boolean {
        if (!mutableCanGoNext.value) return false
        val handler = nextHandler ?: return false
        handler()
        return true
    }

    fun previous(): Boolean {
        val handler = previousHandler ?: return false
        handler()
        return true
    }
}
