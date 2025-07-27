package com.example.localtrail

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.localtrail.utils.SyncManager
import com.example.localtrail.utils.NetworkManager
import com.example.localtrail.utils.MapboxConfig
import com.mapbox.common.MapboxOptions

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        
        // Initialize Mapbox with secure token
        try {
            MapboxOptions.accessToken = MapboxConfig.getAccessToken(this)
        } catch (e: Exception) {
            android.util.Log.e("App", "Failed to initialize Mapbox", e)
        }
        
        // Initialize SyncManager for offline-first functionality
        val syncManager = SyncManager.getInstance(this)
        NetworkManager.getInstance(this)
        
        // Download trail locations from Firestore in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                syncManager.downloadAllTrailLocations()
            } catch (e: Exception) {
                android.util.Log.e("App", "Failed to download trail locations", e)
            }
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }
}