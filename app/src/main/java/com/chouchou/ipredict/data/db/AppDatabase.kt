package com.chouchou.ipredict.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [EventDateEntity::class, EventTypeEntity::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDateDao(): EventDateDao
    abstract fun eventTypeDao(): EventTypeDao // 添加新的DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ipredict_database"
                )
                    .fallbackToDestructiveMigration() // 版本变更时重建数据库
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}