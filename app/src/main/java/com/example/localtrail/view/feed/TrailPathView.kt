package com.example.localtrail.view.feed

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.localtrail.model.TrailLocation
import kotlin.math.*

class TrailPathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val pathPaint = Paint().apply {
        color = Color.parseColor("#6200EE")
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#F0F8FF") // Light blue background
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E0E0E0") // Light gray for grid lines
        strokeWidth = 1f
        style = Paint.Style.STROKE
        alpha = 128
    }

    private var trailPath: Path? = null
    private var bounds: RectF? = null

    fun setTrailLocations(locations: List<TrailLocation>) {
        if (locations.isEmpty()) {
            trailPath = null
            bounds = null
            invalidate()
            return
        }

        // Find the bounds of the trail
        val minLat = locations.minOf { it.latitude }
        val maxLat = locations.maxOf { it.latitude }
        val minLng = locations.minOf { it.longitude }
        val maxLng = locations.maxOf { it.longitude }

        bounds = RectF(
            minLng.toFloat(),
            minLat.toFloat(),
            maxLng.toFloat(),
            maxLat.toFloat()
        )

        // Create the path
        trailPath = Path().apply {
            val firstLocation = locations.first()
            moveTo(firstLocation.longitude.toFloat(), firstLocation.latitude.toFloat())
            
            locations.drop(1).forEach { location ->
                lineTo(location.longitude.toFloat(), location.latitude.toFloat())
            }
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw a simple grid for map-like appearance
        val gridSpacing = 40f
        for (i in 0..(width / gridSpacing).toInt()) {
            val x = i * gridSpacing
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (i in 0..(height / gridSpacing).toInt()) {
            val y = i * gridSpacing
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        val path = trailPath ?: return
        val pathBounds = bounds ?: return

        // Calculate scaling and translation to fit the trail in the view
        val padding = 20f
        val viewWidth = width - 2 * padding
        val viewHeight = height - 2 * padding

        val pathWidth = pathBounds.width()
        val pathHeight = pathBounds.height()

        if (pathWidth == 0f || pathHeight == 0f) return

        val scaleX = viewWidth / pathWidth
        val scaleY = viewHeight / pathHeight
        val scale = minOf(scaleX, scaleY)

        // Center the path in the view
        val scaledWidth = pathWidth * scale
        val scaledHeight = pathHeight * scale
        val offsetX = padding + (viewWidth - scaledWidth) / 2 - pathBounds.left * scale
        val offsetY = padding + (viewHeight - scaledHeight) / 2 - pathBounds.top * scale

        canvas.save()
        
        // Apply transformations
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        
        // Draw the trail path
        canvas.drawPath(path, pathPaint)
        
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        
        val desiredWidth = 300
        val desiredHeight = 160

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}
