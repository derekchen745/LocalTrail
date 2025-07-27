package com.example.localtrail.utils

import com.example.localtrail.model.Trail

object TrailDifficultyUtils {
    
    /**
     * Determines trail difficulty based on tags
     * Priority: Easy > Moderate > Challenging > Unknown (default)
     */
    fun getTrailDifficulty(trail: Trail): TrailDifficulty {
        val tags = trail.tags?.map { it.lowercase() } ?: emptyList()
        
        return when {
            tags.contains("easy") -> TrailDifficulty.EASY
            tags.contains("moderate") -> TrailDifficulty.MODERATE
            tags.contains("challenging") -> TrailDifficulty.CHALLENGING
            else -> TrailDifficulty.UNKNOWN // Default to unknown if no difficulty tag
        }
    }
    
    /**
     * Gets the color for a trail based on difficulty
     */
    fun getTrailColor(trail: Trail): String {
        return when (getTrailDifficulty(trail)) {
            TrailDifficulty.EASY -> "#4CAF50"      // Green
            TrailDifficulty.MODERATE -> "#FF9800"  // Orange  
            TrailDifficulty.CHALLENGING -> "#F44336" // Red
            TrailDifficulty.UNKNOWN -> "#6200EE"   // Purple (original color)
        }
    }
    
    /**
     * Gets the hex color as an integer for Android drawing
     */
    fun getTrailColorInt(trail: Trail): Int {
        return android.graphics.Color.parseColor(getTrailColor(trail))
    }
}

enum class TrailDifficulty {
    EASY,
    MODERATE, 
    CHALLENGING,
    UNKNOWN
}
