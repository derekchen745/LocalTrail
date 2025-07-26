package com.example.localtrail

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.example.localtrail.utils.SyncManager
import com.example.localtrail.utils.NetworkManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        
        // Initialize SyncManager for offline-first functionality
        SyncManager.getInstance(this)
        NetworkManager.getInstance(this)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set
    }
}