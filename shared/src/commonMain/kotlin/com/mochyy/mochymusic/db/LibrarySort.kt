package com.mochyy.mochymusic.db

import com.mochyy.mochymusic.db.entities.Album
import com.mochyy.mochymusic.db.entities.Playlist
import com.mochyy.mochymusic.db.entities.Song

/**
 * Compares names in the device language, ignoring case and accents: the library's Name and Artist sorts.
 * Android and desktop use java.text.Collator at primary strength; iOS compares case-insensitively for now.
 * Made fresh for every sort so a language change shows on the next one, as before.
 */
expect fun nameCollator(): Comparator<String>

fun List<Song>.sortedBySongTitle(): List<Song> =
    sortedWith(compareBy(nameCollator()) { it.song.title })

fun List<Song>.sortedBySongArtists(): List<Song> =
    sortedWith(
        compareBy(nameCollator()) { song ->
            song.artists.joinToString("") { it.name }
        },
    )

// by artist, then the songs of each album kept together (that second sort is plain, not by language)
fun List<Song>.sortedBySongArtistsThenAlbum(): List<Song> =
    sortedBySongArtists()
        .groupBy { it.album?.title }
        .flatMap { (_, songsByAlbum) ->
            songsByAlbum.sortedBy { album ->
                album.artists.joinToString(
                    "",
                ) { it.name }
            }
        }

fun List<Album>.sortedByAlbumTitle(): List<Album> =
    sortedWith(compareBy(nameCollator()) { it.album.title })

fun List<Album>.sortedByAlbumArtists(): List<Album> =
    sortedWith(compareBy(nameCollator()) { album -> album.artists.joinToString("") { it.name } })

fun List<Playlist>.sortedByPlaylistName(): List<Playlist> =
    sortedWith(compareBy(nameCollator()) { it.playlist.name })
