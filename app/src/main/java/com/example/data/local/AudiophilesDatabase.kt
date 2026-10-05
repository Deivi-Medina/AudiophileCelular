package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DownloadedSongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        SongReviewEntity::class,
        FavoriteSongEntity::class,
        RecentSongEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AudiophilesDatabase : RoomDatabase() {
    abstract fun downloadedSongDao(): DownloadedSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songReviewDao(): SongReviewDao
    abstract fun favoriteAndRecentDao(): FavoriteAndRecentDao

    companion object {
        @Volatile
        private var INSTANCE: AudiophilesDatabase? = null

        /**
         * Migración v2 → v3: agrega la columna `orderIndex` a la tabla
         * `playlist_songs` con valor por defecto 0, preservando los datos
         * existentes de playlists y canciones ya guardadas.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE playlist_songs ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun getInstance(context: Context): AudiophilesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AudiophilesDatabase::class.java,
                    "audiophiles_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}