package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppRuleEntity::class,
        LockSessionEntity::class,
        InterceptionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AegisDatabase : RoomDatabase() {
    abstract fun aegisDao(): AegisDao

    companion object {
        @Volatile
        private var INSTANCE: AegisDatabase? = null

        fun getInstance(context: Context): AegisDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AegisDatabase::class.java,
                    "aegis_lock_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
