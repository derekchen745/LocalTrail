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
}