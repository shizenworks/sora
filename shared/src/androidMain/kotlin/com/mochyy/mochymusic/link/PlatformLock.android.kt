// PlatformLock.android.kt

package com.mochyy.mochymusic.link

actual class PlatformLock actual constructor() {
    private val lock = Any()

    actual fun <T> withLock(block: () -> T): T = synchronized(lock, block)
}
