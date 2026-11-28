// CarLibraryTab.kt

package com.mochyy.mochymusic.car.screens

import androidx.annotation.DrawableRes
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.media3.common.util.UnstableApi
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.car.CarApp
import com.mochyy.mochymusic.car.CarCollection
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// the phone's library, one row per kind of list, each opening its own screen
@UnstableApi
internal class CarLibraryTab(app: CarApp) : CarDataTab<CarLibraryTab.Data>(app, "library", R.string.car_tab_library, R.drawable.library_music) {
    // deviceSongs is -1 when the row is hidden in settings
    class Data(
        val liked: Int,
        val downloaded: Int,
        val deviceSongs: Int,
        val pinned: List<CarCollection>,
        val pinnedCovers: List<CarIcon>,
    )

    override suspend fun fetch(): Data = coroutineScope {
        val liked = async { app.catalog.likedCount() }
        val downloaded = async { app.catalog.downloadedCount() }
        val device = async { if (app.catalog.showDeviceSongs()) app.catalog.deviceSongs().size else -1 }
        val pinned = app.catalog.pinnedPlaylists()
        Data(liked.await(), downloaded.await(), device.await(), pinned, app.images.covers(pinned.map { it.thumbnail }))
    }

    override fun empty() = Data(0, 0, -1, emptyList(), emptyList())

    override fun build(data: Data): Template {
        val songs = { count: Int -> app.carContext.resources.getQuantityString(R.plurals.n_song, count, count) }
        val liked = entry(R.string.car_liked, songs(data.liked), R.drawable.favorite) {
            app.push(CarSongsScreen(app, app.string(R.string.car_liked)) { app.catalog.liked() })
        }
        val downloaded = entry(R.string.car_downloaded, songs(data.downloaded), R.drawable.download) {
            app.push(CarSongsScreen(app, app.string(R.string.car_downloaded)) { app.catalog.downloaded() })
        }
        // imported local files and downloads together, the car's folder for what's on the phone
        val device = if (data.deviceSongs < 0) emptyList() else listOf(
            entry(R.string.car_device_songs, songs(data.deviceSongs), R.drawable.smartphone) {
                app.push(CarSongsScreen(app, app.string(R.string.car_device_songs)) { app.catalog.deviceSongs() })
            },
        )
        val rows = listOf(liked, downloaded) + device + listOf(
            entry(R.string.car_playlists, null, R.drawable.queue_music) {
                app.push(CarCollectionsScreen(app, app.string(R.string.car_playlists), grid = false) { app.catalog.playlists() })
            },
            entry(R.string.car_albums, null, R.drawable.album) {
                app.push(CarCollectionsScreen(app, app.string(R.string.car_albums), grid = true) { app.catalog.albums() })
            },
            entry(R.string.car_artists, null, R.drawable.artist) {
                app.push(CarCollectionsScreen(app, app.string(R.string.car_artists), grid = true) { app.catalog.artists() })
            },
        )
        val template = SectionedItemTemplate.Builder().addSection(RowSection.Builder().setItems(rows).build())
        // pinned playlists sit under the library rows, in the order picked in settings
        if (data.pinned.isNotEmpty()) {
            val pinned = data.pinned.mapIndexed { index, list ->
                collectionRow(list, data.pinnedCovers[index]) { app.push(CarSongsScreen(app, list.title, list.songs)) }
            }
            template.addSection(RowSection.Builder().setTitle(app.string(R.string.car_pinned_playlists)).setItems(pinned).build())
        }
        return template.addAction(app.playerButton()).build()
    }

    override fun loadingTemplate(): Template = SectionedItemTemplate.Builder().setLoading(true).build()

    private fun entry(title: Int, text: String?, @DrawableRes icon: Int, onClick: () -> Unit): Row =
        Row.Builder()
            .setTitle(app.string(title))
            .apply { if (text != null) addText(text) }
            .setImage(app.images.icon(icon), Row.IMAGE_TYPE_ICON)
            .setBrowsable(true)
            .setOnClickListener(onClick)
            .build()
}
