package com.example.cityexplorerchallenge_agh.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChallengeEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun challengeDao(): ChallengeDao

    companion object {
        private var instance: AppDatabase? = null

        // Build the database once, then reuse the same one.
        fun get(context: Context): AppDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "city-explorer-challenges.db"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration(true)
                    .build()
            }
            return instance!!
        }
    }
}