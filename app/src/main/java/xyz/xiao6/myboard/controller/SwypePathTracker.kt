package xyz.xiao6.myboard.controller

import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import xyz.xiao6.myboard.model.Key
import kotlin.math.abs

/**
 * Tracks swype gesture path across keyboard keys
 * 追踪滑行输入手势路径
 */
class SwypePathTracker(
    private val keyRects: Map<String, RectF>,
    private val onPathUpdated: (List<String>, Path) -> Unit,
) {
    private val logTag = "SwypePathTracker"

    private var isTracking = false
    private var trackedKeys = mutableListOf<String>()
    private val path = Path()

    // Touch tracking
    private var startPoint = Pair(0f, 0f)
    private var lastPoint = Pair(0f, 0f)
    private var downTime = 0L

    // Configuration
    private val minMoveDistancePx = 30f // Minimum distance to consider as move
    private val maxDurationMs = 800L // Maximum gesture duration

    /**
     * Start tracking a new swype gesture
     */
    fun start(event: MotionEvent, startKey: Key) {
        isTracking = true
        trackedKeys.clear()
        trackedKeys.add(startKey.keyId)

        startPoint = Pair(event.x, event.y)
        lastPoint = startPoint
        downTime = event.eventTime

        path.reset()
        path.moveTo(event.x, event.y)
    }

    /**
     * Update tracking with motion move
     */
    fun update(event: MotionEvent): Boolean {
        if (!isTracking) return false

        // Check timeout
        if (event.eventTime - downTime > maxDurationMs) {
            cancel()
            return false
        }

        val currentPoint = Pair(event.x, event.y)

        // Check if moved enough
        val dx = currentPoint.first - lastPoint.first
        val dy = currentPoint.second - lastPoint.second
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance < minMoveDistancePx) {
            return false
        }

        // Update path
        path.lineTo(event.x, event.y)
        lastPoint = currentPoint

        // Find which key is under current point
        val currentKey = findKeyAt(event.x, event.y)
        if (currentKey != null && currentKey !in trackedKeys) {
            trackedKeys.add(currentKey)
            onPathUpdated(trackedKeys.toList(), path)
            return true
        }

        // Still update path for visual feedback
        onPathUpdated(trackedKeys.toList(), path)
        return false
    }

    /**
     * End tracking and return final result
     */
    fun end(): List<String>? {
        if (!isTracking) return null
        isTracking = false

        val result = trackedKeys.toList()
        reset()
        return result
    }

    /**
     * Cancel current tracking
     */
    fun cancel() {
        isTracking = false
        reset()
    }

    /**
     * Get current path for rendering
     */
    fun getPath(): Path = path

    /**
     * Check if currently tracking
     */
    fun isTracking(): Boolean = isTracking

    /**
     * Reset tracker state
     */
    private fun reset() {
        trackedKeys.clear()
        path.reset()
        startPoint = Pair(0f, 0f)
        lastPoint = Pair(0f, 0f)
        downTime = 0L
    }

    /**
     * Find key at given coordinates
     */
    private fun findKeyAt(x: Float, y: Float): String? {
        // Use touch rects which are slightly larger than visual rects
        for ((keyId, rect) in keyRects) {
            if (rect.contains(x, y)) {
                return keyId
            }
        }
        return null
    }

    /**
     * Get gesture duration in milliseconds
     */
    fun getDurationMs(): Long {
        if (!isTracking) return 0
        return System.currentTimeMillis() - downTime
    }
}
