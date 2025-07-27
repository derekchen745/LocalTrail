package com.example.localtrail.utils

import android.graphics.*
import android.util.Log
import com.example.localtrail.App
import com.example.localtrail.model.TrailLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

object TrailThumbnailGenerator {
    
    /**
     * Generates a trail thumbnail using Mapbox Static Images API
     */
    suspend fun generateThumbnail(
        locations: List<TrailLocation>,
        width: Int = 300,
        height: Int = 160
    ): Bitmap? {
        if (locations.isEmpty()) {
            return generatePlaceholderThumbnail(width, height)
        }

        return try {
            Log.d("TrailThumbnail", "Generating thumbnail for ${locations.size} locations")
            val staticImageUrl = buildMapboxStaticUrl(locations, width, height)
            Log.d("TrailThumbnail", "Static URL: $staticImageUrl")
            downloadImageFromUrl(staticImageUrl)
        } catch (e: Exception) {
            Log.e("TrailThumbnail", "Error generating thumbnail", e)
            generatePlaceholderThumbnail(width, height)
        }
    }
    
    private fun buildMapboxStaticUrl(
        locations: List<TrailLocation>, 
        width: Int, 
        height: Int
    ): String {
        // Find bounds of the trail
        val minLat = locations.minOf { it.latitude }
        val maxLat = locations.maxOf { it.latitude }
        val minLng = locations.minOf { it.longitude }
        val maxLng = locations.maxOf { it.longitude }
        
        val centerLat = (minLat + maxLat) / 2
        val centerLng = (minLng + maxLng) / 2
        
        // Calculate zoom level based on bounds
        val zoom = calculateZoomForBounds(minLat, maxLat, minLng, maxLng, width, height)
        
        // Create polyline overlay for the trail
        val polylineOverlay = createPolylineOverlay(locations)
        
        // Add start and end markers
        val startLocation = locations.first()
        val endLocation = locations.last()
        
        val startMarker = "pin-s+4CAF50(${startLocation.longitude},${startLocation.latitude})"
        val endMarker = "pin-s+F44336(${endLocation.longitude},${endLocation.latitude})"
        
        // Build the Mapbox Static Images API URL
        val baseUrl = "https://api.mapbox.com/styles/v1/mapbox/outdoors-v12/static"
        val overlays = "$polylineOverlay,$startMarker,$endMarker"
        
        return "$baseUrl/$overlays/$centerLng,$centerLat,$zoom/${width}x$height@2x?access_token=${MapboxConfig.getAccessToken(App.context)}"
    }
    
    private fun createPolylineOverlay(locations: List<TrailLocation>): String {
        // Simplify the path to avoid URL length issues
        val maxPoints = 20
        val step = maxOf(1, locations.size / maxPoints)
        val simplifiedLocations = locations.filterIndexed { index, _ -> index % step == 0 }
        
        // Ensure the last point is included
        val finalLocations = if (simplifiedLocations.last() != locations.last()) {
            simplifiedLocations + locations.last()
        } else {
            simplifiedLocations
        }
        
        // Create coordinates array for GeoJSON LineString
        val coordinates = finalLocations.map { location ->
            "[${location.longitude}, ${location.latitude}]"
        }.joinToString(",")
        
        // Create GeoJSON LineString for the trail path
        val geojson = """
        {
          "type": "Feature",
          "properties": {
            "stroke": "#6200EE",
            "stroke-width": 6,
            "stroke-opacity": 0.9
          },
          "geometry": {
            "type": "LineString",
            "coordinates": [$coordinates]
          }
        }
        """.trimIndent().replace("\n", "").replace(" ", "")
        
        // URL encode the GeoJSON
        val encodedGeojson = java.net.URLEncoder.encode(geojson, "UTF-8")
        
        return "geojson($encodedGeojson)"
    }
    
    private fun calculateZoomForBounds(
        minLat: Double, 
        maxLat: Double, 
        minLng: Double, 
        maxLng: Double,
        width: Int,
        height: Int
    ): Int {
        val latDiff = maxLat - minLat
        val lngDiff = maxLng - minLng
        val maxDiff = maxOf(latDiff, lngDiff)
        
        // Calculate zoom based on the area covered
        return when {
            maxDiff > 0.1 -> 8   // Very long trails
            maxDiff > 0.05 -> 10 // Long trails
            maxDiff > 0.01 -> 12 // Medium trails
            maxDiff > 0.005 -> 13 // Short trails
            maxDiff > 0.001 -> 14 // Very short trails
            else -> 15           // Tiny trails
        }
    }
    
    private suspend fun downloadImageFromUrl(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doInput = true
                connection.connect()
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    bitmap
                } else {
                    Log.e("TrailThumbnail", "HTTP error: ${connection.responseCode}")
                    null
                }
            } catch (e: IOException) {
                Log.e("TrailThumbnail", "Error downloading image", e)
                null
            }
        }
    }
    
    private fun generatePlaceholderThumbnail(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Simple gradient background
        val backgroundPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(
                    Color.parseColor("#E8F5E8"),
                    Color.parseColor("#D4E6D4")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Draw placeholder icon
        val iconPaint = Paint().apply {
            color = Color.parseColor("#AAAAAA")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 8f
        
        // Draw simple trail icon
        canvas.drawCircle(centerX - radius, centerY, radius / 2, iconPaint)
        canvas.drawCircle(centerX, centerY, radius / 2, iconPaint)
        canvas.drawCircle(centerX + radius, centerY, radius / 2, iconPaint)
        
        return bitmap
    }
}
