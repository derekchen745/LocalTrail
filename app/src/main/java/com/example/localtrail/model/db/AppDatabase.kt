package com.example.localtrail.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.localtrail.model.Trail
import com.example.localtrail.model.TrailDao
import com.example.localtrail.model.TrailLocation
import com.example.localtrail.model.TrailLocationDao
import com.example.localtrail.model.typeconverters.TrailTypeConverters

@Database(entities = [Trail::class, TrailLocation::class], version = 2, exportSchema = false)
@TypeConverters(TrailTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao
    abstract fun trailLocationDao(): TrailLocationDao

    // Singleton instance of AppDatabase
    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localtrail.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}