package xyz.xiao6.myboard.ui.handwriting

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import xyz.xiao6.myboard.manager.HandwritingRecognitionManager
import xyz.xiao6.myboard.util.MLog
import kotlin.math.abs

/**
 * Overlay view for handwriting input
 * 手写输入overlay视图
 */
class HandwritingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val logTag = "HandwritingOverlay"

    var onRecognitionResult: ((List<String>) -> Unit)? = null
    var onStrokeCountChanged: ((Int) -> Unit)? = null

    private val strokePaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private var currentPath = Path()
    private val allPaths = mutableListOf<Pair<Path, Paint>>()
    private var lastX = 0f
    private var lastY = 0f
    private var startTime = 0L

    private var recognitionManager: HandwritingRecognitionManager? = null
    private var autoRecognizeEnabled = true
    private val autoRecognizeDelayMs = 500L
    private var autoRecognizeRunnable: Runnable? = null

    private val touchSlop = 12f

    fun setRecognitionManager(manager: HandwritingRecognitionManager) {
        recognitionManager = manager
    }

    fun setStrokeColor(color: Int) {
        strokePaint.color = color
    }

    fun setStrokeWidth(width: Float) {
        strokePaint.strokeWidth = width
    }

    fun setAutoRecognize(enabled: Boolean) {
        autoRecognizeEnabled = enabled
    }

    fun clearCanvas() {
        currentPath.reset()
        allPaths.clear()
        recognitionManager?.clear()
        onStrokeCountChanged?.invoke(0)
        invalidate()
        cancelAutoRecognize()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw all completed strokes
        allPaths.forEach { (path, paint) ->
            canvas.drawPath(path, paint)
        }

        // Draw current stroke
        canvas.drawPath(currentPath, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                startTime = event.eventTime
                currentPath.moveTo(event.x, event.y)

                recognitionManager?.startStroke(event.x, event.y, event.eventTime)

                cancelAutoRecognize()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - lastX)
                val dy = abs(event.y - lastY)

                if (dx > touchSlop || dy > touchSlop) {
                    currentPath.lineTo(event.x, event.y)
                    lastX = event.x
                    lastY = event.y

                    recognitionManager?.addPoint(event.x, event.y, event.eventTime)

                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                currentPath.lineTo(event.x, event.y)
                recognitionManager?.addPoint(event.x, event.y, event.eventTime)
                recognitionManager?.endStroke()

                // Save current path
                allPaths.add(Pair(Path(currentPath), Paint(strokePaint)))
                currentPath.reset()

                invalidate()

                // Notify stroke count changed
                val count = recognitionManager?.getStrokeCount() ?: 0
                onStrokeCountChanged?.invoke(count)

                // Auto recognize if enabled
                if (autoRecognizeEnabled) {
                    scheduleAutoRecognize()
                }

                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun scheduleAutoRecognize() {
        cancelAutoRecognize()
        autoRecognizeRunnable = Runnable {
            performRecognition()
        }
        postDelayed(autoRecognizeRunnable!!, autoRecognizeDelayMs)
    }

    private fun cancelAutoRecognize() {
        autoRecognizeRunnable?.let {
            removeCallbacks(it)
        }
        autoRecognizeRunnable = null
    }

    private fun performRecognition() {
        val manager = recognitionManager ?: return
        if (!manager.hasStrokes()) return

        MLog.d(logTag, "Performing recognition...")
        // This will be async in production with coroutines
        // For now, we'll trigger it via the controller
        onRecognitionResult?.invoke(emptyList()) // Placeholder
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelAutoRecognize()
    }
}
