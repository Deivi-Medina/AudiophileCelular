package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_songs")
data class PlaylistSongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val audioUrl: String? = null,
    val durationText: String = "3:30",
    val isLocal: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(tableName = "song_reviews")
data class SongReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,
    val songTitle: String,
    val artist: String,
    val authorName: String,
    val rating: Float, // 1.0 a 5.0 estrellas
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)
