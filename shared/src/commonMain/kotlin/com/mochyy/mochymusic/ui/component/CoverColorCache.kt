package com.mochyy.mochymusic.ui.component

/**
 * The small LRU the cover components keep their last colours in, replaced from android.util.LruCache
 * now that they are shared. Access happens on the composition thread, as it did before; [get]
 * refreshes the entry so the least recently used colour is the one dropped.
 */
internal class CoverColorCache<K, V>(private val maxSize: Int) {
    private val entries = LinkedHashMap<K, V>()

    operator fun get(key: K): V? {
        val value = entries.remove(key) ?: return null
        entries[key] = value
        return value
    }

    operator fun set(key: K, value: V) {
        if (!entries.containsKey(key) && entries.size >= maxSize) {
            entries.remove(entries.keys.first())
        }
        entries[key] = value
    }
}
