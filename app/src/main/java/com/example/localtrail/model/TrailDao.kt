package com.example.localtrail.model

import androidx.room.*

@Dao
interface TrailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trail: Trail)

    @Delete
    suspend fun delete(trail: Trail)

    @Update
    suspend fun update(trail: Trail)

    @Query("SELECT * from trails")
    suspend fun getAllTrails(): List<Trail>
    
    @Query("SELECT * FROM trails WHERE userID = :userId")
    suspend fun getTrailsByUserId(userId: String): List<Trail>
    
    @Query("SELECT * FROM trails WHERE isSynced = 0")
    suspend fun getUnsyncedTrails(): List<Trail>
    
    @Query("UPDATE trails SET isSynced = 1 WHERE id = :trailId")
    suspend fun markTrailAsSynced(trailId: String)
    
    @Query("UPDATE trails SET isSynced = 0 WHERE id = :trailId")
    suspend fun markTrailAsUnsynced(trailId: String)
    
    @Query("SELECT COUNT(*) FROM trails WHERE isSynced = 0")
    suspend fun getUnsyncedTrailCount(): Int
}