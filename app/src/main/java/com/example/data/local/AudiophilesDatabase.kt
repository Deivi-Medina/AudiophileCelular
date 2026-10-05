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
        RecentSongEntity::class,
        ListenLaterEntity::class,
        PinnedFavoriteEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AudiophilesDatabase : RoomDatabase() {
    abstract fun downloadedSongDao(): DownloadedSongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun songReviewDao(): SongReviewDao
    abstract fun favoriteAndRecentDao(): FavoriteAndRecentDao
    abstract fun reviewExtrasDao(): ReviewExtrasDao

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

        /** v4 → v5: portada en reseñas, lista "Por escuchar" y 5 favoritas fijadas. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE song_reviews ADD COLUMN coverUrl TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `listen_later` (`songId` TEXT NOT NULL, `songTitle` TEXT NOT NULL, " +
                        "`artist` TEXT NOT NULL, `coverUrl` TEXT, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pinned_favorites` (`position` INTEGER NOT NULL, `songId` TEXT NOT NULL, " +
                        "`songTitle` TEXT NOT NULL, `artist` TEXT NOT NULL, `coverUrl` TEXT, PRIMARY KEY(`position`))"
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}