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

@Database(entities = [Trail::class, TrailLocation::class], version = 5, exportSchema = false)
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

        // Migration from version 3 to 4 - add elevation column to trail_locations
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE trail_locations ADD COLUMN elevation REAL")
            }
        }

        // Migration from version 4 to 5 - remove elevation columns
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove elevation column from trails table
                database.execSQL("""
                    CREATE TABLE trails_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        userID TEXT NOT NULL,
                        name TEXT,
                        location TEXT,
                        description TEXT,
                        privacy TEXT NOT NULL,
                        username TEXT NOT NULL,
                        distance REAL,
                        duration TEXT,
                        avgSpeed REAL,
                        effort TEXT,
                        weather TEXT,
                        tags TEXT,
                        notes TEXT,
                        createdAt INTEGER NOT NULL,
                        isSynced INTEGER NOT NULL
                    )
                """)
                database.execSQL("INSERT INTO trails_new SELECT id, userID, name, location, description, privacy, username, distance, duration, avgSpeed, effort, weather, tags, notes, createdAt, isSynced FROM trails")
                database.execSQL("DROP TABLE trails")
                database.execSQL("ALTER TABLE trails_new RENAME TO trails")
                
                // Remove elevation column from trail_locations table
                database.execSQL("""
                    CREATE TABLE trail_locations_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trailId TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(trailId) REFERENCES trails(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("INSERT INTO trail_locations_new SELECT id, trailId, latitude, longitude, timestamp FROM trail_locations")
                database.execSQL("DROP TABLE trail_locations")
                database.execSQL("ALTER TABLE trail_locations_new RENAME TO trail_locations")
                database.execSQL("CREATE INDEX index_trail_locations_trailId ON trail_locations(trailId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "localtrail.db"
                ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}