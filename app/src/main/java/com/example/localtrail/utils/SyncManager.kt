package com.example.localtrail.utils

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.localtrail.controller.AccountController
import com.example.localtrail.model.Trail
import com.example.localtrail.model.TrailLocation
import com.example.localtrail.model.db.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.TimeUnit

class SyncManager private constructor(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val networkManager = NetworkManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    init {
        // Monitor network connectivity and sync when online
        scope.launch {
            networkManager.isOnline.collect { isOnline ->
                if (isOnline) {
                    Log.d("SyncManager", "Network available, starting sync...")
                    syncPendingTrails()
                }
            }
        }
        
        // Schedule periodic sync using WorkManager
        schedulePeriodicSync()
    }
    
    suspend fun saveTrailOfflineFirst(trail: Trail, trailLocations: List<TrailLocation>): Boolean {
        return try {
            // Always save to local database first
            database.trailDao().insert(trail.copy(isSynced = false))
            
            // Save trail locations
            trailLocations.forEach { location ->
                database.trailLocationDao().insert(location)
            }
            
            Log.d("SyncManager", "Trail saved locally: ${trail.name}")
            
            // Try to sync immediately if online
            if (networkManager.isOnline.first()) {
                try {
                    syncTrailToFirestore(trail, trailLocations)
                } catch (e: Exception) {
                    Log.w("SyncManager", "Immediate sync failed, will retry later", e)
                }
            } else {
                Log.d("SyncManager", "Offline - trail will sync when connection available")
            }
            
            true
        } catch (e: Exception) {
            Log.e("SyncManager", "Error saving trail locally", e)
            false
        }
    }
    
    private suspend fun syncPendingTrails() {
        try {
            val unsyncedTrails = database.trailDao().getUnsyncedTrails()
            Log.d("SyncManager", "Found ${unsyncedTrails.size} unsynced trails")
            
            unsyncedTrails.forEach { trail ->
                val trailLocations = database.trailLocationDao().getTrailLocationsForTrailId(trail.id)
                syncTrailToFirestore(trail, trailLocations)
            }
            
        } catch (e: Exception) {
            Log.e("SyncManager", "Error syncing pending trails", e)
        }
    }
    
    private suspend fun syncTrailToFirestore(trail: Trail, trailLocations: List<TrailLocation>) {
        return withContext(Dispatchers.IO) {
            try {
                val user = AccountController.getCurrentUser()
                if (user == null) {
                    Log.e("SyncManager", "No user logged in, cannot sync trail")
                    return@withContext
                }
                
                // Prepare trail data for Firestore
                val trailData = hashMapOf(
                    "name" to trail.name,
                    "location" to trail.location,
                    "description" to trail.description,
                    "userID" to trail.userID,
                    "privacy" to trail.privacy.name,
                    "username" to trail.username,
                    "distance" to trail.distance,
                    "duration" to trail.duration,
                    "elevation" to trail.elevation,
                    "avgSpeed" to trail.avgSpeed,
                    "effort" to trail.effort,
                    "weather" to trail.weather,
                    "tags" to trail.tags,
                    "notes" to trail.notes,
                    "createdAt" to trail.createdAt
                )
                
                // Use suspendCoroutine to convert callback to suspend function
                suspendCoroutine<Unit> { continuation ->
                    firestore.collection("trails")
                        .document(trail.id)
                        .set(trailData)
                        .addOnSuccessListener {
                            Log.d("SyncManager", "Successfully synced trail to Firestore: ${trail.name}")
                            continuation.resume(Unit)
                        }
                        .addOnFailureListener { e ->
                            Log.e("SyncManager", "Failed to sync trail to Firestore: ${trail.name}", e)
                            continuation.resumeWithException(e)
                        }
                }
                
                // Sync trail locations to Firestore (optional, for detailed tracking)
                syncTrailLocationsToFirestore(trail.id, trailLocations)
                
                // Mark as synced in local database
                database.trailDao().markTrailAsSynced(trail.id)
                Log.d("SyncManager", "Trail marked as synced: ${trail.name}")
                
            } catch (e: Exception) {
                Log.e("SyncManager", "Error syncing trail to Firestore: ${trail.name}", e)
                throw e
            }
        }
    }
    
    private suspend fun syncTrailLocationsToFirestore(trailId: String, trailLocations: List<TrailLocation>) {
        try {
            val locationsData = trailLocations.map { location ->
                hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "timestamp" to location.timestamp
                )
            }
            
            suspendCoroutine<Unit> { continuation ->
                firestore.collection("trails")
                    .document(trailId)
                    .update("locations", locationsData)
                    .addOnSuccessListener {
                        Log.d("SyncManager", "Successfully synced trail locations for trail: $trailId")
                        continuation.resume(Unit)
                    }
                    .addOnFailureListener { e ->
                        Log.w("SyncManager", "Failed to sync trail locations for trail: $trailId", e)
                        // Don't fail the whole sync if locations fail
                        continuation.resume(Unit)
                    }
            }
        } catch (e: Exception) {
            Log.w("SyncManager", "Error syncing trail locations", e)
            // Don't fail the whole sync if locations fail
        }
    }
    
    /**
     * Downloads trail locations from Firestore and stores them locally
     */
    suspend fun downloadTrailLocationsFromFirestore(trailId: String): List<TrailLocation> {
        return withContext(Dispatchers.IO) {
            try {
                val result = suspendCoroutine<List<TrailLocation>> { continuation ->
                    firestore.collection("trails")
                        .document(trailId)
                        .get()
                        .addOnSuccessListener { document ->
                            try {
                                val locations = mutableListOf<TrailLocation>()
                                val locationsData = document.get("locations") as? List<HashMap<String, Any>>
                                
                                locationsData?.forEach { locationMap ->
                                    val latitude = (locationMap["latitude"] as? Number)?.toDouble() ?: 0.0
                                    val longitude = (locationMap["longitude"] as? Number)?.toDouble() ?: 0.0
                                    val timestamp = (locationMap["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                    
                                    locations.add(
                                        TrailLocation(
                                            trailId = trailId,
                                            latitude = latitude,
                                            longitude = longitude,
                                            timestamp = timestamp
                                        )
                                    )
                                }
                                
                                Log.d("SyncManager", "Downloaded ${locations.size} locations for trail: $trailId")
                                continuation.resume(locations)
                            } catch (e: Exception) {
                                Log.e("SyncManager", "Error parsing trail locations", e)
                                continuation.resume(emptyList())
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("SyncManager", "Error downloading trail locations", e)
                            continuation.resume(emptyList())
                        }
                }
                
                // Store downloaded locations in local database
                result.forEach { location ->
                    try {
                        database.trailLocationDao().insert(location)
                    } catch (e: Exception) {
                        // Location might already exist, that's okay
                        Log.d("SyncManager", "Location already exists in local database")
                    }
                }
                
                result
            } catch (e: Exception) {
                Log.e("SyncManager", "Error in downloadTrailLocationsFromFirestore", e)
                emptyList()
            }
        }
    }
    
    /**
     * Downloads all trail locations for all user's trails
     */
    suspend fun downloadAllTrailLocations() {
        withContext(Dispatchers.IO) {
            try {
                val trails = database.trailDao().getAllTrails()
                trails.forEach { trail ->
                    downloadTrailLocationsFromFirestore(trail.id)
                }
                Log.d("SyncManager", "Downloaded locations for ${trails.size} trails")
            } catch (e: Exception) {
                Log.e("SyncManager", "Error downloading all trail locations", e)
            }
        }
    }
    
    suspend fun getLocalTrails(userId: String): List<Trail> {
        return try {
            database.trailDao().getTrailsByUserId(userId)
        } catch (e: Exception) {
            Log.e("SyncManager", "Error fetching local trails", e)
            emptyList()
        }
    }
    
    suspend fun getUnsyncedTrailCount(): Int {
        return try {
            database.trailDao().getUnsyncedTrailCount()
        } catch (e: Exception) {
            Log.e("SyncManager", "Error getting unsynced trail count", e)
            0
        }
    }
    
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).setConstraints(constraints)
         .addTag("trail_sync")
         .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "trail_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
    
    fun onDestroy() {
        scope.cancel()
        networkManager.unregisterCallback()
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SyncManager? = null
        
        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

// WorkManager worker for periodic sync
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val syncManager = SyncManager.getInstance(applicationContext)
            val unsyncedCount = syncManager.getUnsyncedTrailCount()
            
            Log.d("SyncWorker", "Starting sync work - $unsyncedCount unsynced trails")
            
            if (unsyncedCount > 0) {
                // The syncManager will automatically sync pending trails when constructed
                // and when network becomes available
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync work failed", e)
            Result.retry()
        }
    }
}
