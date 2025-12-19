package xyz.xiao6.myboard.ui.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.abs

/**
 * Transparent overlay which draws a border around [target] and allows resizing by dragging edges.
 *
 * Width mode:
 * - drag LEFT/RIGHT edge horizontally to change width
 *
 * Height mode:
 * - drag TOP/BOTTOM edge vertically to change height
 */
class KeyboardResizeOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var target: View? = null
        set(value) {
            field = value
            invalidate()
        }

    var onResizeDeltaPx: ((deltaWidthPx: Int, deltaHeightPx: Int) -> Unit)? = null
    var onReset: (() -> Unit)? = null
    var onConfirm: (() -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            color = Color.parseColor("#FF007AFF")
        }

    private val handlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FF007AFF")
        }

    private val handleStrokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2f)
            color = Color.WHITE
        }

    private val targetRect = RectF()
    private val resetRect = RectF()
    private val confirmRect = RectF()
    private val tmp = IntArray(2)

    private enum class Handle {
        NONE,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
    }

    private var activeHandle: Handle = Handle.NONE
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downInsideTarget: Boolean = false

    private val resetBgPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#E6000000")
        }

    private val resetTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = sp(14f)
            typeface = Typeface.DEFAULT_BOLD
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = resolveTargetRect() ?: return
        canvas.drawRect(rect, borderPaint)

        val radius = dp(10f)
        val inset = max(radius, borderPaint.strokeWidth / 2f + dp(2f))
        // Always draw all handles so users can see where to drag.
        val leftXAligned = rect.left + inset
        val rightXAligned = rect.right - inset
        val cx = rect.centerX()
        val topYAligned = rect.top + inset
        val bottomYAligned = rect.bottom - inset
        val cy = rect.centerY()

        // Left/Right (width)
        canvas.drawCircle(leftXAligned, cy, radius, handlePaint)
        canvas.drawCircle(leftXAligned, cy, radius, handleStrokePaint)
        canvas.drawCircle(rightXAligned, cy, radius, handlePaint)
        canvas.drawCircle(rightXAligned, cy, radius, handleStrokePaint)

        // Top/Bottom (height)
        canvas.drawCircle(cx, topYAligned, radius, handlePaint)
        canvas.drawCircle(cx, topYAligned, radius, handleStrokePaint)
        canvas.drawCircle(cx, bottomYAligned, radius, handlePaint)
        canvas.drawCircle(cx, bottomYAligned, radius, handleStrokePaint)

        drawCenterButtons(canvas, rect)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rect = resolveTargetRect()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastX = downX
                lastY = downY
                if (rect != null && (resetRect.contains(downX, downY) || confirmRect.contains(downX, downY))) {
                    // consume; click handled on ACTION_UP
                    activeHandle = Handle.NONE
                    downInsideTarget = true
                    return true
                }
                activeHandle = pickHandle(rect, event.x, event.y)
                downInsideTarget = rect?.contains(event.x, event.y) == true
                // Always consume to prevent keyboard input while resizing.
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.x - lastX).toInt()
                val dy = (event.y - lastY).toInt()
                lastX = event.x
                lastY = event.y

                if (activeHandle == Handle.NONE) return true

                // Allow resizing both width and height from any handle:
                // - LEFT/RIGHT: width driven by dx (directional), height driven by dy
                // - TOP/BOTTOM: height driven by dy (directional), width driven by dx
                val (dw, dh) =
                    when (activeHandle) {
                        Handle.LEFT -> (-dx) to dy
                        Handle.RIGHT -> dx to dy
                        Handle.TOP -> dx to (-dy)
                        Handle.BOTTOM -> dx to dy
                        Handle.NONE -> 0 to 0
                    }
                if (dw != 0 || dh != 0) {
                    onResizeDeltaPx?.invoke(dw, dh)
                    postInvalidateOnAnimation()
                }
                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            -> {
                val wasHandle = activeHandle != Handle.NONE
                activeHandle = Handle.NONE
                // Tap outside target: dismiss. Tap inside target but not on handle: also dismiss (toggle feel).
                val isClick = abs(event.x - downX) < dp(3f) && abs(event.y - downY) < dp(3f)
                if (rect != null && isClick && resetRect.contains(event.x, event.y)) {
                    onReset?.invoke()
                    return true
                }
                if (rect != null && isClick && confirmRect.contains(event.x, event.y)) {
                    onConfirm?.invoke()
                    return true
                }
                if (isClick && !wasHandle) onDismiss?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun pickHandle(rect: RectF?, x: Float, y: Float): Handle {
        rect ?: return Handle.NONE
        val hit = dp(22f)
        if (abs(x - rect.left) <= hit && y in (rect.top - hit)..(rect.bottom + hit)) return Handle.LEFT
        if (abs(x - rect.right) <= hit && y in (rect.top - hit)..(rect.bottom + hit)) return Handle.RIGHT
        if (abs(y - rect.top) <= hit && x in (rect.left - hit)..(rect.right + hit)) return Handle.TOP
        if (abs(y - rect.bottom) <= hit && x in (rect.left - hit)..(rect.right + hit)) return Handle.BOTTOM
        return Handle.NONE
    }

    private fun resolveTargetRect(): RectF? {
        val t = target ?: return null
        if (t.width <= 0 || t.height <= 0) return null

        // Convert target location to this view's coordinate system.
        t.getLocationOnScreen(tmp)
        val tx = tmp[0]
        val ty = tmp[1]
        getLocationOnScreen(tmp)
        val ox = tmp[0]
        val oy = tmp[1]

        targetRect.set(
            (tx - ox).toFloat(),
            (ty - oy).toFloat(),
            (tx - ox + t.width).toFloat(),
            (ty - oy + t.height).toFloat(),
        )
        return targetRect
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private fun drawCenterButtons(canvas: Canvas, rect: RectF) {
        val resetText = "重置"
        val confirmText = "确定"
        val padX = dp(14f)
        val h = dp(32f)
        val w =
            max(
                dp(72f),
                max(resetTextPaint.measureText(resetText), resetTextPaint.measureText(confirmText)) + padX * 2,
            )
        val spacing = dp(12f)
        val groupW = w * 2 + spacing
        val leftCx = rect.centerX() - groupW / 2f + w / 2f
        val rightCx = rect.centerX() + groupW / 2f - w / 2f
        val cy = rect.centerY()
        resetRect.set(leftCx - w / 2f, cy - h / 2f, leftCx + w / 2f, cy + h / 2f)
        confirmRect.set(rightCx - w / 2f, cy - h / 2f, rightCx + w / 2f, cy + h / 2f)
        val r = h / 2f
        canvas.drawRoundRect(resetRect, r, r, resetBgPaint)
        canvas.drawRoundRect(confirmRect, r, r, resetBgPaint)
        val textY = cy - (resetTextPaint.descent() + resetTextPaint.ascent()) / 2f
        canvas.drawText(resetText, resetRect.centerX(), textY, resetTextPaint)
        canvas.drawText(confirmText, confirmRect.centerX(), textY, resetTextPaint)
    }
}
