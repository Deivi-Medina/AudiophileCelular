package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val artist: String,
    val durationText: String,
    val thumbnailUrl: String,
    val localFilePath: String,
    val bitrate: String,
    val format: String,
    val downloadedAt: Long = System.currentTimeMillis()
)
