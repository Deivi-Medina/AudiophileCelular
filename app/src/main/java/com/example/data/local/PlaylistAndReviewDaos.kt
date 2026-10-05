package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name, description = :description WHERE id = :id")
    suspend fun updatePlaylist(id: Long, name: String, description: String)

    // Ordenado por orderIndex persistente, con fallback a id para registros viejos
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC, id ASC")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongToPlaylist(song: PlaylistSongEntity): Long

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongCount(playlistId: Long): Int

    @Query("DELETE FROM playlist_songs WHERE songId = :songId OR title = :title")
    suspend fun removeSongFromAllPlaylists(songId: String, title: String)

    // ─── REORDENAMIENTO ─────────────────────────────────────────

    /** Devuelve el orderIndex máximo actual (o -1 si la playlist está vacía). */
    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int

    /** Actualiza el orderIndex de una fila concreta. */
    @Query("UPDATE playlist_songs SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int)

    /**
     * Reordena en bloque: recibe la lista de IDs en el orden final y
     * asigna orderIndex = 0, 1, 2, ... de forma atómica.
     */
    @Transaction
    suspend fun reorderSongs(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateOrderIndex(id, index)
        }
    }
}

@Dao
interface SongReviewDao {
    @Query("SELECT * FROM song_reviews WHERE songId = :songId ORDER BY createdAt DESC")
    fun getReviewsForSong(songId: String): Flow<List<SongReviewEntity>>

    @Query("SELECT * FROM song_reviews ORDER BY createdAt DESC LIMIT 50")
    fun getAllRecentReviews(): Flow<List<SongReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: SongReviewEntity): Long

    @Query("SELECT * FROM song_reviews ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<SongReviewEntity>>

    @Query("UPDATE song_reviews SET rating = :rating, comment = :comment WHERE id = :id")
    suspend fun updateReview(id: Long, rating: Float, comment: String)

    @Query("DELETE FROM song_reviews WHERE id = :id")
    suspend fun deleteReview(id: Long)
}