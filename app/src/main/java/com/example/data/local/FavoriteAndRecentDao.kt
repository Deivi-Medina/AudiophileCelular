package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteAndRecentDao {
    // Favoritos
    @Query("SELECT * FROM favorite_songs ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteSongEntity>>

    @Query("SELECT COUNT(*) FROM favorite_songs")
    fun getFavoritesCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songId = :songId)")
    fun isFavorite(songId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE songId = :songId)")
    suspend fun isFavoriteSync(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(song: FavoriteSongEntity)

    @Query("DELETE FROM favorite_songs WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)

    // Recientes
    @Query("SELECT * FROM recently_played_songs ORDER BY playedAt DESC LIMIT 50")
    fun getRecentlyPlayed(): Flow<List<RecentSongEntity>>

    @Query("SELECT COUNT(*) FROM recently_played_songs")
    fun getRecentlyPlayedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPlayed(song: RecentSongEntity)

    @Query("DELETE FROM recently_played_songs")
    suspend fun clearRecentHistory()
}
