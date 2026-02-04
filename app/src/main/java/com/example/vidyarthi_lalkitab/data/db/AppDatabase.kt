package com.example.vidyarthi_lalkitab.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.vidyarthi_lalkitab.data.dao.KundliDao
import com.example.vidyarthi_lalkitab.data.entity.KundliEntity

@Database(entities = [KundliEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kundliDao(): KundliDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kundli_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
