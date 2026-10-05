package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {
    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    fun getAllDownloadedSongs(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs ORDER BY downloadedAt DESC")
    suspend fun getDownloadedSongsList(): List<DownloadedSongEntity>

    @Query("SELECT * FROM downloaded_songs WHERE videoId = :videoId LIMIT 1")
    suspend fun getSongByVideoId(videoId: String): DownloadedSongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE videoId = :videoId")
    suspend fun deleteSong(videoId: String)

    @Query("DELETE FROM downloaded_songs WHERE localFilePath = :path OR title = :title")
    suspend fun deleteSongByPathOrTitle(path: String, title: String)
}
