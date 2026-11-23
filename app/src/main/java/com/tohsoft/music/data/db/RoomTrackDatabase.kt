package com.tohsoft.music.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TrackEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(TrackTypeConverters::class)
abstract class RoomTrackDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: RoomTrackDatabase? = null

        // Явный реестр миграций для сохранности медиатеки при обновлениях (без destructive wipe)
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun getInstance(context: Context): RoomTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoomTrackDatabase::class.java,
                    "mp3pro_room_tracks.db"
                )
                    .addMigrations(*MIGRATIONS)
                    .allowMainThreadQueries()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
