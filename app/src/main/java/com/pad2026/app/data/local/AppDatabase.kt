package com.pad2026.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DeviceEntity::class, AdEntity::class, DownloadTaskEntity::class, PlayLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun adDao(): AdDao
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun playLogDao(): PlayLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pad_ad_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
