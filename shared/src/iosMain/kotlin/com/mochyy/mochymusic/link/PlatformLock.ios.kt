// PlatformLock.ios.kt

package com.mochyy.mochymusic.link

import platform.Foundation.NSRecursiveLock

actual class PlatformLock actual constructor() {
    private val lock = NSRecursiveLock()

    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }
}
