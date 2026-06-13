package com.seuapp.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.seuapp.music.data.model.Track

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tracksJson: String
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: String,
    val id: String,
    val title: String,
    val channel: String,
    val duration: Long?,
    val thumbnail: String?,
    val url: String
) {
    fun toTrack() = Track(id = id, title = title, channel = channel, duration = duration, thumbnail = thumbnail, url = url)
}

@Entity(tableName = "recent_tracks")
data class RecentTrackEntity(
    @PrimaryKey val trackId: String,
    val id: String,
    val title: String,
    val channel: String,
    val duration: Long?,
    val thumbnail: String?,
    val url: String,
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toTrack() = Track(id = id, title = title, channel = channel, duration = duration, thumbnail = thumbnail, url = url)
}
