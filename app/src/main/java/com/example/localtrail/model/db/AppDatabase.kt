package com.example.localtrail.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.localtrail.model.Trail
import com.example.localtrail.model.TrailDao
import com.example.localtrail.model.TrailLocation
import com.example.localtrail.model.TrailLocationDao
import com.example.localtrail.model.typeconverters.TrailTypeConverters

@Database(entities = [Trail::class, TrailLocation::class], version = 3, exportSchema = false)
@TypeConverters(TrailTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao
    abstract fun trailLocationDao(): TrailLocationDao

    // Singleton instance of AppDatabase
    companion object {
        private var INSTANCE: AppDatabase? = null

        // Migration from version 2 to 3 - add isSynced column
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE trails ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localtrail.db"
                ).addMigrations(MIGRATION_2_3).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}