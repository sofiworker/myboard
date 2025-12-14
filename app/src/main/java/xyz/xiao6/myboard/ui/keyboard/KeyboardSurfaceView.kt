package xyz.xiao6.myboard.ui.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.controller.LayoutState
import xyz.xiao6.myboard.controller.ShiftState
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.KeyAction
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyTrigger
import xyz.xiao6.myboard.model.ActionType
import xyz.xiao6.myboard.ui.popup.PopupView
import xyz.xiao6.myboard.util.MLog

/**
 * 键盘核心区骨架：Canvas 自绘 + onLayout 中计算按键几何。
 * Keyboard core skeleton: Canvas rendering + compute key geometry in onLayout.
 */
class KeyboardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val logTag = "KeyboardView"

    private var layoutModel: KeyboardLayout? = null
    private var keys: List<Key> = emptyList()
    private var keyRects: Map<String, RectF> = emptyMap()
    private var layoutState: LayoutState = LayoutState()
    private var popupView: PopupView? = null

    var onTrigger: ((keyId: String, trigger: KeyTrigger) -> Unit)? = null
    var onAction: ((KeyAction) -> Unit)? = null

    private val previewDelayMs = 100L
    private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()
    private var activeKeyId: String? = null
    private var activeKeyRect: RectF? = null
    private val previewRunnable = Runnable {
        val popup = popupView ?: return@Runnable
        val keyId = activeKeyId ?: return@Runnable
        val key = keys.firstOrNull { it.keyId == keyId } ?: return@Runnable
        val rect = activeKeyRect ?: return@Runnable
        popup.showKeyPreview(this, rect, resolveLabel(key))
    }
    private val longPressRunnable = Runnable {
        val popup = popupView ?: return@Runnable
        val keyId = activeKeyId ?: return@Runnable
        val key = keys.firstOrNull { it.keyId == keyId } ?: return@Runnable
        val rect = activeKeyRect ?: return@Runnable

        val action = key.behaviors[KeyTrigger.LONG_PRESS]
        if (action?.actionType != ActionType.SHOW_POPUP) {
            MLog.d(logTag, "longPress keyId=$keyId no SHOW_POPUP behavior")
            return@Runnable
        }
        val candidates = action.values?.filter { it.isNotBlank() }.orEmpty()
        if (candidates.isEmpty()) {
            MLog.d(logTag, "longPress keyId=$keyId SHOW_POPUP but candidates empty")
            return@Runnable
        }

        MLog.d(logTag, "longPress keyId=$keyId candidates=${candidates.size}")
        popup.dismissPreview()
        popup.showLongPressCandidates(this, rect, candidates) { selected ->
            onAction?.invoke(KeyAction(actionType = ActionType.COMMIT, value = selected))
        }
    }

    private val keyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.purple_200)
    }
    private val keyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        color = ContextCompat.getColor(context, R.color.black)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.black)
        textAlign = Paint.Align.CENTER
        textSize = dp(16f)
    }

    fun setLayout(layout: KeyboardLayout) {
        layoutModel = layout
        keys = layout.rows.flatMap { it.keys }
        requestLayout()
        invalidate()
    }

    /**
     * State Change：只更新状态并进行局部刷新，禁止触发 requestLayout()。
     * State change: update state and do partial invalidation; must NOT call requestLayout().
     */
    fun setLayoutState(state: LayoutState, invalidateKeyIds: Set<String>) {
        layoutState = state
        if (invalidateKeyIds.isEmpty()) {
            invalidate()
        } else {
            invalidateKeys(invalidateKeyIds)
        }
    }

    fun setPopupView(popupView: PopupView?) {
        this.popupView?.dismissAll()
        this.popupView = popupView
    }

    fun setKeys(keys: List<Key>) {
        this.keys = keys
        requestLayout()
        invalidate()
    }

    fun invalidateKey(keyId: String) {
        val rect = keyRects[keyId] ?: return
        val r = expand(rect)
        ViewCompat.postInvalidateOnAnimation(this, r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
    }

    fun invalidateKeys(keyIds: Set<String>) {
        for (keyId in keyIds) {
            val rect = keyRects[keyId] ?: continue
            val r = expand(rect)
            ViewCompat.postInvalidateOnAnimation(this, r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        keyRects = computeKeyRects()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        popupView?.dismissAll()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        val popup = popupView
        if (popup != null && popup.isCandidatesShowing() && popup.dispatchCandidatesTouch(this, event)) {
            if (event.actionMasked == android.view.MotionEvent.ACTION_UP ||
                event.actionMasked == android.view.MotionEvent.ACTION_CANCEL
            ) {
                popup.dismissCandidates()
            }
            return true
        }

        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                val hit = hitTestKey(event.x, event.y) ?: return false
                activeKeyId = hit.keyId
                activeKeyRect = keyRects[hit.keyId]

                schedulePreview()
                scheduleLongPressPopup()
                return true
            }

            android.view.MotionEvent.ACTION_MOVE -> {
                val keyId = activeKeyId ?: return false
                val rect = activeKeyRect ?: return false
                if (!rect.contains(event.x, event.y)) {
                    cancelPreviewAndLongPress()
                    popup?.dismissPreview()
                }
                return true
            }

            android.view.MotionEvent.ACTION_UP -> {
                val keyId = activeKeyId
                val rect = activeKeyRect
                cancelPreviewAndLongPress()
                popup?.dismissPreview()

                if (keyId != null && rect != null && rect.contains(event.x, event.y)) {
                    onTrigger?.invoke(keyId, KeyTrigger.TAP)
                }
                activeKeyId = null
                activeKeyRect = null
                return true
            }

            android.view.MotionEvent.ACTION_CANCEL -> {
                cancelPreviewAndLongPress()
                popup?.dismissAll()
                activeKeyId = null
                activeKeyRect = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (keyRects.isEmpty()) return

        for (key in keys) {
            if (layoutState.hiddenKeyIds.contains(key.keyId)) continue
            val rect = keyRects[key.keyId] ?: continue
            val fillPaint = if (layoutState.highlightedKeyIds.contains(key.keyId)) {
                keyFillPaint.apply { color = ContextCompat.getColor(context, R.color.teal_200) }
            } else {
                keyFillPaint.apply { color = ContextCompat.getColor(context, R.color.purple_200) }
            }
            canvas.drawRoundRect(rect, dp(6f), dp(6f), fillPaint)
            canvas.drawRoundRect(rect, dp(6f), dp(6f), keyStrokePaint)

            val x = rect.centerX()
            val y = rect.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2f
            canvas.drawText(resolveLabel(key), x, y, labelPaint)
        }
    }

    private fun resolveLabel(key: Key): String {
        val label = key.label
        if (label.length != 1) return label
        val c = label[0]
        if (!c.isLetter()) return label
        return when (layoutState.shift) {
            ShiftState.OFF -> label.lowercase()
            ShiftState.ON, ShiftState.CAPS_LOCK -> label.uppercase()
        }
    }

    private fun hitTestKey(x: Float, y: Float): Key? {
        if (keys.isEmpty() || keyRects.isEmpty()) return null
        for (key in keys) {
            if (layoutState.hiddenKeyIds.contains(key.keyId)) continue
            val rect = keyRects[key.keyId] ?: continue
            if (rect.contains(x, y)) return key
        }
        return null
    }

    private fun schedulePreview() {
        if (popupView == null) return
        removeCallbacks(previewRunnable)
        postDelayed(previewRunnable, previewDelayMs)
    }

    private fun scheduleLongPressPopup() {
        if (popupView == null) return
        removeCallbacks(longPressRunnable)
        postDelayed(longPressRunnable, longPressTimeoutMs)
    }

    private fun cancelPreviewAndLongPress() {
        removeCallbacks(previewRunnable)
        removeCallbacks(longPressRunnable)
    }

    private fun expand(rect: RectF): RectF {
        val p = dp(2f)
        return RectF(rect.left - p, rect.top - p, rect.right + p, rect.bottom + p)
    }

    private fun computeKeyRects(): Map<String, RectF> {
        val layout = layoutModel ?: return emptyMap()
        if (keys.isEmpty()) return emptyMap()

        val padding = layout.defaults.padding
        val leftPx = padding.leftDp.dpToPx()
        val topPx = padding.topDp.dpToPx()
        val rightPx = padding.rightDp.dpToPx()
        val bottomPx = padding.bottomDp.dpToPx()
        val availableWidth = (width - leftPx - rightPx).coerceAtLeast(0f)
        val availableHeight = (height - topPx - bottomPx).coerceAtLeast(0f)

        val rowCount = layout.rows.size.coerceAtLeast(1)
        val verticalGapPx = layout.defaults.verticalGapDp.dpToPx()
        val totalGapHeight = verticalGapPx * (rowCount - 1)
        val keyboardContentHeight = (availableHeight - totalGapHeight).coerceAtLeast(0f)

        val rowTops = mutableMapOf<String, Float>()
        val rowHeights = mutableMapOf<String, Float>()

        var y = topPx
        layout.rows.forEach { row ->
            val rowHeight = (keyboardContentHeight * row.heightRatio) + row.heightDpOffset.dpToPx()
            rowTops[row.rowId] = y
            rowHeights[row.rowId] = rowHeight
            y += rowHeight + verticalGapPx
        }

        val result = LinkedHashMap<String, RectF>(keys.size)
        layout.rows.forEach { row ->
            val rowTop = rowTops[row.rowId] ?: return@forEach
            val rowHeight = rowHeights[row.rowId] ?: return@forEach

            val rowGapPx = (row.horizontalGapDp ?: layout.defaults.horizontalGapDp).dpToPx()
            val rowStartPaddingPx = (row.startPaddingDp ?: padding.leftDp).dpToPx()
            val rowEndPaddingPx = (row.endPaddingDp ?: padding.rightDp).dpToPx()

            val rowAvailableWidth =
                (availableWidth * row.widthRatio) + row.widthDpOffset.dpToPx() - rowStartPaddingPx - rowEndPaddingPx
            val keysInRow = row.keys.sortedBy { it.gridPosition.startCol }
            if (keysInRow.isEmpty()) return@forEach

            val fixedWidthSum = keysInRow.sumOf { (it.widthDp ?: 0f).toDouble() }.toFloat().dpToPx()
            val totalWeight = keysInRow.filter { it.widthDp == null }.sumOf { it.widthWeight.toDouble() }.toFloat()
            val totalGaps = rowGapPx * (keysInRow.size - 1)
            val remaining =
                (rowAvailableWidth - fixedWidthSum - totalGaps).coerceAtLeast(0f)

            val widths = keysInRow.map { key ->
                key.widthDp?.dpToPx()
                    ?: if (totalWeight > 0f) remaining * (key.widthWeight / totalWeight) else 0f
            }

            val rowTotalWidth = widths.sum() + totalGaps
            val startX = when (row.alignment) {
                xyz.xiao6.myboard.model.RowAlignment.LEFT -> leftPx + rowStartPaddingPx
                xyz.xiao6.myboard.model.RowAlignment.CENTER ->
                    leftPx + rowStartPaddingPx + ((rowAvailableWidth - rowTotalWidth) / 2f).coerceAtLeast(0f)
                xyz.xiao6.myboard.model.RowAlignment.JUSTIFY -> leftPx + rowStartPaddingPx
            }

            var x = startX
            keysInRow.forEachIndexed { index, key ->
                val keyWidth = widths[index]
                val keyHeight = rowHeight
                val rect = RectF(x, rowTop, x + keyWidth, rowTop + keyHeight)
                result[key.keyId] = rect
                x += keyWidth + rowGapPx
            }
        }

        return result
    }

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density
    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
