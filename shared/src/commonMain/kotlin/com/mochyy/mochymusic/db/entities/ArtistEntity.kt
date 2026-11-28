package com.mochyy.mochymusic.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mochyy.mochymusic.db.nowLocal
import kotlinx.datetime.LocalDateTime
import kotlin.random.Random

@Immutable
@Entity(tableName = "artist")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    val lastUpdateTime: LocalDateTime = nowLocal(),
    val bookmarkedAt: LocalDateTime? = null,
    // Neiro/ArchiveTune port: when the listener blocked this artist; null while unblocked
    @ColumnInfo(name = "blockedAt", defaultValue = "NULL")
    val blockedAt: LocalDateTime? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false
) {
    val isYouTubeArtist: Boolean
        get() = id.startsWith("UC") || id.startsWith("FEmusic_library_privately_owned_artist")

    val isPrivatelyOwnedArtist: Boolean
        get() = id.startsWith("FEmusic_library_privately_owned_artist")

    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else nowLocal(),
    )

    companion object {
        private const val Letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

        fun generateArtistId() = "LA" + CharArray(8) { Letters[Random.nextInt(Letters.length)] }.concatToString()
    }
}
