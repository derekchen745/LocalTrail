package com.example.localtrail.model

import androidx.room.*

@Dao
interface TrailLocationDao {
    @Insert
    suspend fun insert(location: TrailLocation)

    @Delete
    suspend fun delete(location: TrailLocation)

    @Query("DELETE FROM trail_locations WHERE trailId = :trailId")
    suspend fun deleteByTrailId(trailId: String)

    @Query("SELECT * FROM trail_locations WHERE trailId = :trailId ORDER BY timestamp ASC")
    suspend fun getTrailLocationsForTrailId(trailId: String): List<TrailLocation>
}