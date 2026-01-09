package xyz.xiao6.myboard.ui.keyboard

import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import xyz.xiao6.myboard.controller.SwypePathTracker
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.util.MLog

/**
 * Helper class to integrate swype input into KeyboardSurfaceView
 * 滑行输入集成助手类
 */
class SwypeInputHelper(
    private val view: KeyboardSurfaceView,
) {
    private val logTag = "SwypeInputHelper"

    var tracker: SwypePathTracker? = null
    var enabled: Boolean = false
    var showTrail: Boolean = true

    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x80000000.toInt()
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /**
     * Initialize tracker with current key rects
     */
    fun initTracker(keyRects: Map<String, android.graphics.RectF>) {
        if (tracker == null) {
            tracker = SwypePathTracker(
                keyRects = keyRects,
                onPathUpdated = { keys, path ->
                    view.invalidate() // Redraw to show trail
                    MLog.d(logTag, "Swype path: ${keys.joinToString(" -> ")}")
                }
            )
        } else {
            // Update key rects if layout changed
            tracker = SwypePathTracker(
                keyRects = keyRects,
                onPathUpdated = { keys, path ->
                    view.invalidate()
                    MLog.d(logTag, "Swype path: ${keys.joinToString(" -> ")}")
                }
            )
        }
    }

    /**
     * Handle ACTION_DOWN event
     * @return true if swype mode is handling the event
     */
    fun handleDown(event: MotionEvent, startKey: Key): Boolean {
        if (!enabled) return false

        MLog.d(logTag, "Swype DOWN on key: ${startKey.keyId}")
        tracker?.start(event, startKey)
        view.invalidate()
        return true
    }

    /**
     * Handle ACTION_MOVE event
     * @return true if swype mode is handling the event
     */
    fun handleMove(event: MotionEvent): Boolean {
        if (!enabled) return false
        val track = tracker ?: return false
        if (!track.isTracking()) return false

        MLog.d(logTag, "Swype MOVE")
        track.update(event)
        view.invalidate()
        return true
    }

    /**
     * Handle ACTION_UP event
     * @return true if swype mode was handling the event
     */
    fun handleUp(event: MotionEvent): Boolean {
        if (!enabled) return false
        val track = tracker ?: return false
        if (!track.isTracking()) return false

        MLog.d(logTag, "Swype UP")
        val keys = track.end()
        if (keys != null && keys.isNotEmpty()) {
            MLog.d(logTag, "Swype result: ${keys.joinToString(" -> ")}")
            view.onSwypePath?.invoke(keys)
        }

        view.invalidate()
        return true
    }

    /**
     * Handle ACTION_CANCEL event
     * @return true if swype mode was handling the event
     */
    fun handleCancel(event: MotionEvent): Boolean {
        if (!enabled) return false
        val track = tracker ?: return false
        if (!track.isTracking()) return false

        MLog.d(logTag, "Swype CANCEL")
        track.cancel()
        view.invalidate()
        return true
    }

    /**
     * Render the swype trail on canvas
     */
    fun renderTrail(canvas: Canvas) {
        if (!showTrail) return
        val track = tracker ?: return
        if (!track.isTracking()) return

        val path = track.getPath()
        canvas.drawPath(path, trailPaint)
    }

    /**
     * Cancel current swype tracking
     */
    fun cancel() {
        tracker?.cancel()
        view.invalidate()
    }

    /**
     * Check if currently tracking
     */
    fun isTracking(): Boolean {
        return tracker?.isTracking() ?: false
    }
}
