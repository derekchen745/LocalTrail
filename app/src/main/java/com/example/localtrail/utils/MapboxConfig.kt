package com.example.localtrail.utils

import android.content.Context
import android.util.Log
import java.io.InputStream
import java.util.Properties

object MapboxConfig {
    private var accessToken: String? = null
    
    fun getAccessToken(context: Context): String {
        if (accessToken == null) {
            accessToken = loadTokenFromConfig(context)
            Log.d("MapboxConfig", "Loaded token: ${accessToken?.take(10)}...")
        }
        return accessToken ?: throw IllegalStateException("Mapbox access token not found")
    }
    
    private fun loadTokenFromConfig(context: Context): String? {
        return try {
            val inputStream: InputStream = context.resources.openRawResource(
                context.resources.getIdentifier(
                    "mapbox_config", 
                    "raw", 
                    context.packageName
                )
            )
            val properties = Properties()
            properties.load(inputStream)
            inputStream.close()
            properties.getProperty("mapbox_access_token")
        } catch (e: Exception) {
            Log.e("MapboxConfig", "Error loading Mapbox token from config", e)
            null
        }
    }
}
