package com.mochyy.mochymusic.shared.desktop

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import com.mochyy.mochymusic.constants.MaxImageCacheSizeKey
import com.mochyy.mochymusic.shared.image.DefaultImageCacheMb
import com.mochyy.mochymusic.shared.image.mochyImageDefaults
import com.mochyy.mochymusic.utils.PreferencesCache
import java.io.File
import okio.Path.Companion.toPath

// Covers on desktop get the same crossfade and memory share as Android, plus a disk cache in the
// platform's cache folder, sized by the same setting the Android loader reads.
internal fun configureDesktopImageLoader() {
    val cacheSizeMb = (PreferencesCache.current(desktopPlaybackPreferences.store)[MaxImageCacheSizeKey]
        ?: DefaultImageCacheMb).toLong()
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .mochyImageDefaults(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
            }
            .apply {
                if (cacheSizeMb == 0L) {
                    diskCachePolicy(CachePolicy.DISABLED)
                } else {
                    val directory = desktopImageCacheDirectory()
                    if (directory != null) {
                        diskCache(
                            DiskCache.Builder()
                                .directory(directory.resolve("coil").absolutePath.toPath())
                                .maxSizeBytes(cacheSizeMb * 1024L * 1024L)
                                .build()
                        )
                    }
                }
            }
            .build()
    }
}

// ~/.cache on Linux and the platform equivalents elsewhere; null when the folder can't be made,
// so a read-only home falls back to the memory cache instead of crashing at start-up
private fun desktopImageCacheDirectory(): File? {
    val home = File(System.getProperty("user.home"))
    val os = System.getProperty("os.name").lowercase()
    val cache = when {
        os.startsWith("mac") -> home.resolve("Library/Caches")
        os.startsWith("windows") ->
            System.getenv("LOCALAPPDATA")?.takeUnless(String::isBlank)?.let(::File) ?: home.resolve("AppData/Local")
        else -> System.getenv("XDG_CACHE_HOME")?.takeUnless(String::isBlank)?.let(::File)?.takeIf(File::isAbsolute)
            ?: home.resolve(".cache")
    }
    val directory = cache.resolve("Mochy/image_cache")
    return directory.takeIf { it.isDirectory || it.mkdirs() }
}
