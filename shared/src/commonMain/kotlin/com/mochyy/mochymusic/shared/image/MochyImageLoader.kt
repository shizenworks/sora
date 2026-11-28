package com.mochyy.mochymusic.shared.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.memory.MemoryCache
import coil3.request.crossfade

// The values the Android loader in App.kt uses: covers ease in, and the memory cache is 15% of
// the app's heap. Desktop and iOS start from the same numbers so a cover looks the same everywhere.
const val ImageCrossfadeMs = 300
const val ImageMemoryCachePercent = 0.15
const val DefaultImageCacheMb = 512

fun ImageLoader.Builder.mochyImageDefaults(context: PlatformContext): ImageLoader.Builder = apply {
    crossfade(ImageCrossfadeMs)
    memoryCache {
        MemoryCache.Builder()
            .maxSizePercent(context, ImageMemoryCachePercent)
            .build()
    }
}
