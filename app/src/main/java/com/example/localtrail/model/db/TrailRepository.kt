package com.example.localtrail.model.db

import android.content.Context
import com.example.localtrail.model.Trail

class TrailRepository(private val context: Context) {
    private val appDb = AppDatabase.getInstance(context)

    suspend fun saveTrailLocal(trail: Trail) {
        appDb.trailDao().insert(trail)
    }
}