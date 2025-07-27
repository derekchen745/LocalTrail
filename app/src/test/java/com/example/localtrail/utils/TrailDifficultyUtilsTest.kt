package com.example.localtrail.utils

import com.example.localtrail.model.Trail
import com.example.localtrail.model.enums.TrailPrivacy
import org.junit.Test
import org.junit.Assert.*

class TrailDifficultyUtilsTest {
    
    @Test
    fun testEasyTrailColor() {
        val trail = Trail(
            id = "test1",
            userID = "user1",
            name = "Easy Trail",
            tags = listOf("Easy", "Scenic")
        )
        
        assertEquals("#4CAF50", TrailDifficultyUtils.getTrailColor(trail))
        assertEquals(TrailDifficulty.EASY, TrailDifficultyUtils.getTrailDifficulty(trail))
    }
    
    @Test
    fun testModerateTrailColor() {
        val trail = Trail(
            id = "test2",
            userID = "user1", 
            name = "Moderate Trail",
            tags = listOf("Moderate", "Forest")
        )
        
        assertEquals("#FF9800", TrailDifficultyUtils.getTrailColor(trail))
        assertEquals(TrailDifficulty.MODERATE, TrailDifficultyUtils.getTrailDifficulty(trail))
    }
    
    @Test
    fun testChallengingTrailColor() {
        val trail = Trail(
            id = "test3",
            userID = "user1",
            name = "Hard Trail", 
            tags = listOf("Challenging", "Mountain")
        )
        
        assertEquals("#F44336", TrailDifficultyUtils.getTrailColor(trail))
        assertEquals(TrailDifficulty.CHALLENGING, TrailDifficultyUtils.getTrailDifficulty(trail))
    }
    
    @Test
    fun testDefaultToUnknown() {
        val trail = Trail(
            id = "test4",
            userID = "user1",
            name = "Unknown Difficulty",
            tags = listOf("Scenic", "Lake")
        )
        
        assertEquals("#6200EE", TrailDifficultyUtils.getTrailColor(trail))
        assertEquals(TrailDifficulty.UNKNOWN, TrailDifficultyUtils.getTrailDifficulty(trail))
    }
    
    @Test
    fun testNullTags() {
        val trail = Trail(
            id = "test5",
            userID = "user1",
            name = "No Tags Trail",
            tags = null
        )
        
        assertEquals("#6200EE", TrailDifficultyUtils.getTrailColor(trail))
        assertEquals(TrailDifficulty.UNKNOWN, TrailDifficultyUtils.getTrailDifficulty(trail))
    }
    
    @Test
    fun testCaseInsensitive() {
        val trail = Trail(
            id = "test6",
            userID = "user1",
            name = "Mixed Case Trail",
            tags = listOf("EASY", "scenic")
        )
        
        assertEquals("#4CAF50", TrailDifficultyUtils.getTrailColor(trail))
        assertEquals(TrailDifficulty.EASY, TrailDifficultyUtils.getTrailDifficulty(trail))
    }
}
