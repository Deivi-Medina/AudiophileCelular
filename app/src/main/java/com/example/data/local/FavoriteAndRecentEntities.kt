package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val audioUrl: String? = null,
    val durationText: String = "3:30",
    val isLocal: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played_songs")
data class RecentSongEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val artist: String,
    val coverUrl: String? = null,
    val audioUrl: String? = null,
    val durationText: String = "3:30",
    val isLocal: Boolean = false,
    val playedAt: Long = System.currentTimeMillis()
)
