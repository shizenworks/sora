package com.mochyy.mochymusic.shared.ios

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import com.mochyy.mochymusic.constants.MaxImageCacheSizeKey
import com.mochyy.mochymusic.shared.image.DefaultImageCacheMb
import com.mochyy.mochymusic.shared.image.mochyImageDefaults
import com.mochyy.mochymusic.utils.PreferencesCache
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

// Covers on iOS use the same crossfade and memory share as Android, with the disk cache under the
// app's caches folder and the same size setting the Android loader reads.
internal fun configureIosImageLoader() {
    val cacheSizeMb = (PreferencesCache.current(iosPlaybackPreferences.store)[MaxImageCacheSizeKey]
        ?: DefaultImageCacheMb).toLong()
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .mochyImageDefaults(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .apply {
                if (cacheSizeMb == 0L) {
                    diskCachePolicy(CachePolicy.DISABLED)
                } else {
                    val caches = NSSearchPathForDirectoriesInDomains(
                        NSCachesDirectory,
                        NSUserDomainMask,
                        true,
                    ).firstOrNull() as? String
                    if (caches != null) {
                        diskCache(
                            DiskCache.Builder()
                                .directory((caches.toPath() / "Mochy" / "image_cache" / "coil"))
                                .maxSizeBytes(cacheSizeMb * 1024L * 1024L)
                                .build()
                        )
                    }
                }
            }
            .build()
    }
}
