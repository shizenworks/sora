package com.mochyy.mochymusic.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.mochyy.mochymusic.db.daos.SpeedDialDao
import com.mochyy.mochymusic.db.entities.AlbumArtistMap
import com.mochyy.mochymusic.db.entities.AlbumEntity
import com.mochyy.mochymusic.db.entities.ArtistEntity
import com.mochyy.mochymusic.db.entities.Event
import com.mochyy.mochymusic.db.entities.FormatEntity
import com.mochyy.mochymusic.db.entities.ImportSession
import com.mochyy.mochymusic.db.entities.ImportSessionSong
import com.mochyy.mochymusic.db.entities.ImportedPlaylist
import com.mochyy.mochymusic.db.entities.ImportedPlaylistTrack
import com.mochyy.mochymusic.db.entities.ImportedSong
import com.mochyy.mochymusic.db.entities.LyricsEntity
import com.mochyy.mochymusic.db.entities.PlayCountEntity
import com.mochyy.mochymusic.db.entities.PlaylistEntity
import com.mochyy.mochymusic.db.entities.PlaylistEvent
import com.mochyy.mochymusic.db.entities.PlaylistSongMap
import com.mochyy.mochymusic.db.entities.PlaylistSongMapPreview
import com.mochyy.mochymusic.db.entities.RecognitionHistory
import com.mochyy.mochymusic.db.entities.RelatedSongMap
import com.mochyy.mochymusic.db.entities.SearchHistory
import com.mochyy.mochymusic.db.entities.SetVideoIdEntity
import com.mochyy.mochymusic.db.entities.SongAlbumMap
import com.mochyy.mochymusic.db.entities.SongArtistMap
import com.mochyy.mochymusic.db.entities.SongEdit
import com.mochyy.mochymusic.db.entities.SongEntity
import com.mochyy.mochymusic.db.entities.SortedSongAlbumMap
import com.mochyy.mochymusic.db.entities.SortedSongArtistMap
import com.mochyy.mochymusic.db.entities.SpeedDialItem

// The music database on desktop and iOS: the same schema as Android's InternalDatabase in app/
// (same name, entities, views and version), with only the DAOs Room can run off Android. The entity
// list is written twice, so Fast checks diffs this class's exported schema against app/schemas.
// No target compiles both classes: Android never sees nonAndroidMain.
@Database(
    entities = [
        SongEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaylistEntity::class,
        SongArtistMap::class,
        SongAlbumMap::class,
        AlbumArtistMap::class,
        PlaylistSongMap::class,
        SearchHistory::class,
        FormatEntity::class,
        LyricsEntity::class,
        Event::class,
        RelatedSongMap::class,
        SetVideoIdEntity::class,
        PlayCountEntity::class,
        RecognitionHistory::class,
        SpeedDialItem::class,
        PlaylistEvent::class,
        ImportedSong::class,
        ImportSession::class,
        ImportSessionSong::class,
        ImportedPlaylist::class,
        ImportedPlaylistTrack::class,
        SongEdit::class,
    ],
    views = [
        SortedSongArtistMap::class,
        SortedSongAlbumMap::class,
        PlaylistSongMapPreview::class,
    ],
    version = MusicDatabaseVersion,
    exportSchema = true,
)
@TypeConverters(Converters::class)
@ConstructedBy(InternalDatabaseConstructor::class)
abstract class InternalDatabase : RoomDatabase(), MochyDatabase {
    abstract override val songDao: SongDao
    abstract override val playlistDao: PlaylistDao
    abstract override val historyDao: HistoryDao
    abstract override val suspendingDao: SuspendingDao
    abstract override val speedDialDao: SpeedDialDao
}

@Suppress("KotlinNoActualForExpect") // Room generates each platform's constructor.
expect object InternalDatabaseConstructor : RoomDatabaseConstructor<InternalDatabase> {
    override fun initialize(): InternalDatabase
}
