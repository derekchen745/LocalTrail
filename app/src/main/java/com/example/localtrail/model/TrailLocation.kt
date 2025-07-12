package com.example.localtrail.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "trail_locations",
    foreignKeys = [ForeignKey(
        entity = Trail::class,
        parentColumns = ["id"],
        childColumns = ["trailId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trailId")]
)
data class TrailLocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trailId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)