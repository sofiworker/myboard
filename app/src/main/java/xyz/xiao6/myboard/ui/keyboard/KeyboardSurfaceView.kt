package xyz.xiao6.myboard.ui.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
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
import xyz.xiao6.myboard.model.GestureType
import xyz.xiao6.myboard.model.KeyStyle
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.popup.PopupView
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import xyz.xiao6.myboard.util.MLog
import kotlin.math.abs
import kotlin.math.hypot

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
    private var keyTouchRects: Map<String, RectF> = emptyMap()
    private var layoutState: LayoutState = LayoutState()
    private var popupView: PopupView? = null

    private var themeSpec: ThemeSpec? = null
    private var theme: ThemeRuntime? = null
    private var layoutBackgroundColor: Int = Color.parseColor("#F2F2F7")

    var onTrigger: ((keyId: String, trigger: GestureType) -> Unit)? = null
    var onAction: ((KeyAction) -> Unit)? = null

    private val previewDelayMs = 100L
    private val longPressTimeoutMs = ViewConfiguration.getLongPressTimeout().toLong()
    private val touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val swipeThresholdPx = touchSlopPx * 2.5f
    private var activeKeyId: String? = null
    private var activeKeyRect: RectF? = null
    private var activeKeyTouchRect: RectF? = null
    private var activePointerId: Int = -1
    private var downX = 0f
    private var downY = 0f
    private var downAbsX = 0f
    private var downAbsY = 0f
    private var resolvedTrigger: GestureType? = null
    private var movedSinceDown: Boolean = false
    private var longPressPopupKeyId: String? = null
    private var longPressPopupCommitted: Boolean = false
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
        MLog.d(logTag, "longPress keyId=$keyId")
        popup.dismissPreview()
        longPressPopupKeyId = keyId
        longPressPopupCommitted = false
        onTrigger?.invoke(keyId, GestureType.LONG_PRESS)
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
        textSize = sp(16f)
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AA000000")
        textSize = sp(10f)
    }

    fun setTheme(theme: ThemeSpec?) {
        themeSpec = theme
        this.theme = theme?.let { ThemeRuntime(it) }
        layoutBackgroundColor =
            this.theme?.resolveColor(theme?.layout?.background?.color ?: theme?.colors?.get("background"), layoutBackgroundColor)
                ?: layoutBackgroundColor
        // Shadows drawn via Paint#setShadowLayer require software rendering.
        val needsShadow = themeSpec?.keyStyles?.values?.any { it.shadow != null } == true
        if (needsShadow) setLayerType(LAYER_TYPE_SOFTWARE, null)
        invalidate()
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
        keyTouchRects = computeTouchRects(keyRects)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        popupView?.dismissAll()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        val popup = popupView
        if (popup != null && popup.isCandidatesShowing()) {
            val handledByPopup = popup.dispatchCandidatesTouch(this, event)
            if (handledByPopup) {
                if (event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
                ) {
                    popup.dismissCandidates()
                    cancelPreviewAndLongPress()
                    val fallbackKeyId = longPressPopupKeyId
                    val fallbackCommitted = longPressPopupCommitted
                    activeKeyId = null
                    activeKeyRect = null
                    activeKeyTouchRect = null
                    activePointerId = -1
                    resolvedTrigger = null
                    movedSinceDown = false
                    longPressPopupKeyId = null
                    longPressPopupCommitted = false

                    if (!fallbackCommitted && fallbackKeyId != null && event.actionMasked == MotionEvent.ACTION_UP) {
                        MLog.d(logTag, "popup fallback TAP keyId=$fallbackKeyId")
                        onTrigger?.invoke(fallbackKeyId, GestureType.TAP)
                    }
                }
                return true
            } else if (event.actionMasked == MotionEvent.ACTION_DOWN ||
                event.actionMasked == MotionEvent.ACTION_UP ||
                event.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                // Touch outside popup: dismiss it and continue processing as a normal keyboard touch.
                popup.dismissCandidates()
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = hitTestKey(event.x, event.y) ?: return false
                activeKeyId = hit.keyId
                activeKeyRect = keyRects[hit.keyId]
                activeKeyTouchRect = keyTouchRects[hit.keyId] ?: activeKeyRect
                activePointerId = event.getPointerId(0)
                downX = event.x
                downY = event.y
                val abs = absXY(event, 0)
                downAbsX = abs.first
                downAbsY = abs.second
                resolvedTrigger = null
                movedSinceDown = false

                MLog.d(
                    logTag,
                    "DOWN keyId=${hit.keyId} label=${hit.label} x=${event.x.toInt()} y=${event.y.toInt()} absX=${downAbsX.toInt()} absY=${downAbsY.toInt()} pid=$activePointerId pc=${event.pointerCount}",
                )

                schedulePreview()
                scheduleLongPressPopup()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val keyId = activeKeyId ?: return false
                val touchRect = activeKeyTouchRect ?: return false
                val pointerIndex = event.findPointerIndex(activePointerId)
                if (pointerIndex < 0) return false
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val abs = absXY(event, pointerIndex)
                val absX = abs.first
                val absY = abs.second

                if (resolvedTrigger == null) {
                    val dx = absX - downAbsX
                    val dy = absY - downAbsY
                    if (!movedSinceDown && hypot(dx.toDouble(), dy.toDouble()) > touchSlopPx) {
                        movedSinceDown = true
                        MLog.d(logTag, "MOVE started keyId=$keyId dx=${dx.toInt()} dy=${dy.toInt()}")
                        // Moving cancels long-press to avoid accidental popup during fast typing/swipes.
                        removeCallbacks(longPressRunnable)
                        popup?.dismissPreview()
                    }
                    if (abs(dy) > swipeThresholdPx && abs(dy) > abs(dx)) {
                        resolvedTrigger = if (dy < 0f) GestureType.FLICK_UP else GestureType.FLICK_DOWN
                        cancelPreviewAndLongPress()
                        popup?.dismissPreview()
                        MLog.d(logTag, "SWIPE trigger=$resolvedTrigger keyId=$keyId dy=${dy.toInt()} dx=${dx.toInt()}")
                        return true
                    }
                }

                if (resolvedTrigger == null && movedSinceDown) {
                    val hit = hitTestKey(x, y)
                    if (hit != null && hit.keyId != keyId) {
                        MLog.d(logTag, "MOVE keyChange from=$keyId to=${hit.keyId}")
                        activeKeyId = hit.keyId
                        activeKeyRect = keyRects[hit.keyId]
                        activeKeyTouchRect = keyTouchRects[hit.keyId] ?: activeKeyRect
                        cancelPreviewAndLongPress()
                        popup?.dismissPreview()
                        schedulePreview()
                        scheduleLongPressPopup()
                        return true
                    }

                    if (hit == null && !touchRect.contains(x, y)) {
                        cancelPreviewAndLongPress()
                        popup?.dismissPreview()
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                val keyId = activeKeyId
                val touchRect = activeKeyTouchRect
                val trigger = resolvedTrigger
                val pointerIndex = event.findPointerIndex(activePointerId).takeIf { it >= 0 } ?: event.actionIndex
                val abs = absXY(event, pointerIndex)
                val absX = abs.first
                val absY = abs.second
                val absDist = hypot(absX - downAbsX, absY - downAbsY)
                val upX = event.getX(pointerIndex)
                val upY = event.getY(pointerIndex)
                cancelPreviewAndLongPress()
                popup?.dismissPreview()

                if (keyId != null && touchRect != null) {
                    if (trigger != null) {
                        MLog.d(
                            logTag,
                            "UP keyId=$keyId trigger=$trigger absDist=${absDist.toInt()} pid=$activePointerId ai=${event.actionIndex} pc=${event.pointerCount}",
                        )
                        onTrigger?.invoke(keyId, trigger)
                    } else {
                        MLog.d(
                            logTag,
                            "UP keyId=$keyId trigger=TAP moved=$movedSinceDown inRect=${touchRect.contains(upX, upY)} absDist=${absDist.toInt()} " +
                                "x=${upX.toInt()} y=${upY.toInt()} absX=${absX.toInt()} absY=${absY.toInt()} pid=$activePointerId ai=${event.actionIndex} pc=${event.pointerCount}",
                        )
                        onTrigger?.invoke(keyId, GestureType.TAP)
                    }
                }
                activeKeyId = null
                activeKeyRect = null
                activeKeyTouchRect = null
                activePointerId = -1
                resolvedTrigger = null
                movedSinceDown = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                cancelPreviewAndLongPress()
                popup?.dismissAll()
                activeKeyId = null
                activeKeyRect = null
                activeKeyTouchRect = null
                activePointerId = -1
                resolvedTrigger = null
                MLog.d(logTag, "CANCEL")
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun absXY(event: MotionEvent, pointerIndex: Int): Pair<Float, Float> {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        return (loc[0] + event.getX(pointerIndex)) to (loc[1] + event.getY(pointerIndex))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (keyRects.isEmpty()) return

        canvas.drawColor(layoutBackgroundColor)

        for (key in keys) {
            if (layoutState.hiddenKeyIds.contains(key.keyId)) continue
            val rect = keyRects[key.keyId] ?: continue
            val pressed = layoutState.highlightedKeyIds.contains(key.keyId)

            val style = resolveKeyStyle(key.ui.styleId)
            val cornerRadius = dp((style?.cornerRadiusDp ?: themeSpec?.global?.paint?.cornerRadiusDp ?: 10f))

            applyKeyPaints(style, pressed)

            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyFillPaint)
            if (keyStrokePaint.color != Color.TRANSPARENT && keyStrokePaint.strokeWidth > 0f) {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyStrokePaint)
            }

            val hints = resolveHints(key)
            if (hints.isNotEmpty()) {
                for (h in hints) {
                    if (h.text.isBlank()) continue
                    drawHint(canvas, rect, h.anchor, h.text)
                }
            }

            val x = rect.centerX()
            val y = rect.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2f
            canvas.drawText(resolveLabel(key), x, y, labelPaint)
        }
    }

    private fun resolveKeyStyle(styleId: String): KeyStyle? = themeSpec?.keyStyles?.get(styleId)

    private fun applyKeyPaints(style: KeyStyle?, pressed: Boolean) {
        val theme = theme
        val defaultKeyBg = Color.WHITE
        val defaultPressedBg = Color.parseColor("#E5E5EA")
        val defaultFunctionBg = Color.parseColor("#E5E5EA")
        val defaultStroke = Color.parseColor("#1A000000")

        val isFunction = style?.label?.sizeSp?.let { it < 16f } == true || style?.textSizeSp?.let { it < 16f } == true
        val fallbackBg = if (isFunction) defaultFunctionBg else defaultKeyBg

        val bgColor =
            if (pressed) {
                theme?.resolveColor(style?.backgroundPressed?.color ?: style?.background?.color, defaultPressedBg)
                    ?: defaultPressedBg
            } else {
                theme?.resolveColor(style?.background?.color, fallbackBg) ?: fallbackBg
            }

        keyFillPaint.color = bgColor
        val shadow = style?.shadow
        if (shadow != null && theme != null) {
            keyFillPaint.setShadowLayer(
                dp(shadow.radiusDp ?: 2f),
                dp(shadow.dxDp ?: 0f),
                dp(shadow.dyDp ?: 1f),
                theme.resolveShadowColor(shadow, Color.parseColor("#33000000")),
            )
        } else {
            keyFillPaint.clearShadowLayer()
        }

        val stroke = (if (pressed) style?.strokePressed else null) ?: style?.stroke
        if (stroke?.widthDp != null && stroke.widthDp > 0f) {
            keyStrokePaint.strokeWidth = dp(stroke.widthDp)
            keyStrokePaint.color = theme?.resolveStrokeColor(stroke, defaultStroke) ?: defaultStroke
        } else {
            keyStrokePaint.strokeWidth = dp(1f)
            keyStrokePaint.color = theme?.resolveStrokeColor(stroke, Color.TRANSPARENT) ?: Color.TRANSPARENT
        }

        val labelColor =
            theme?.resolveColor(style?.label?.color ?: style?.textColor ?: "colors.key_text", Color.BLACK) ?: Color.BLACK
        val labelSizeSp = style?.label?.sizeSp ?: style?.textSizeSp ?: 16f
        labelPaint.color = labelColor
        labelPaint.textSize = sp(labelSizeSp)

        val hintColor =
            theme?.resolveColor(style?.hint?.color ?: "colors.key_hint", Color.parseColor("#8E8E93")) ?: Color.parseColor("#8E8E93")
        hintPaint.color = hintColor
        hintPaint.textSize = sp(style?.hint?.sizeSp ?: 10f)
    }

    private fun resolveLabel(key: Key): String {
        val label = layoutState.labelOverrides[key.keyId] ?: (key.ui.label ?: key.label).orEmpty()
        if (label.length != 1) return label
        val c = label[0]
        if (!c.isLetter()) return label
        return when (layoutState.shift) {
            ShiftState.OFF -> label.lowercase()
            ShiftState.ON, ShiftState.CAPS_LOCK -> label.uppercase()
        }
    }

    private enum class HintGridPosition {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        CENTER_LEFT,
        CENTER,
        CENTER_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT,
    }

    private sealed interface HintAnchor {
        data class Grid(val position: HintGridPosition) : HintAnchor

        /**
         * Relative anchor inside the key rect.
         * - x,y are ratios in [0,1] (0=left/top, 1=right/bottom).
         * - align controls textAlign; default is CENTER.
         */
        data class Relative(val x: Float, val y: Float, val align: Paint.Align = Paint.Align.CENTER) : HintAnchor
    }

    private data class HintEntry(
        val anchor: HintAnchor,
        val text: String,
    )

    private fun resolveHints(key: Key): List<HintEntry> {
        fun parseHints(raw: Map<String, String>): List<HintEntry> {
            val out = ArrayList<HintEntry>(raw.size)
            for ((k, v) in raw) {
                val keyRaw = k.trim()
                val text = v
                if (keyRaw.isBlank() || text.isBlank()) continue

                val grid = runCatching { HintGridPosition.valueOf(keyRaw) }.getOrNull()
                if (grid != null) {
                    out += HintEntry(anchor = HintAnchor.Grid(grid), text = text)
                    continue
                }

                // Coordinate format: "x,y" or "x,y,ALIGN" (ratios in [0,1]).
                val parts = keyRaw.split(',').map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    val x = parts[0].toFloatOrNull()
                    val y = parts[1].toFloatOrNull()
                    if (x != null && y != null) {
                        val align =
                            when (parts.getOrNull(2)?.uppercase()) {
                                "LEFT" -> Paint.Align.LEFT
                                "RIGHT" -> Paint.Align.RIGHT
                                "CENTER", null -> Paint.Align.CENTER
                                else -> Paint.Align.CENTER
                            }
                        out += HintEntry(
                            anchor = HintAnchor.Relative(x = x.coerceIn(0f, 1f), y = y.coerceIn(0f, 1f), align = align),
                            text = text,
                        )
                    }
                }
            }
            return out
        }

        // Override wins, and should NOT stack on top of base hints (avoid "symbol pile-up" when switching locale).
        val overrideRaw: Map<String, String> = layoutState.hintOverrides[key.keyId] ?: emptyMap()
        val merged: Map<String, String> = key.hints + overrideRaw
        return parseHints(merged)
    }

    private fun drawHint(canvas: Canvas, rect: RectF, anchor: HintAnchor, text: String) {
        val paddingX = dp(6f)
        val paddingY = dp(4f)
        val fm = hintPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent

        val (x, y, align) =
            when (anchor) {
                is HintAnchor.Relative -> {
                    val px = rect.left + rect.width() * anchor.x
                    val py = rect.top + rect.height() * anchor.y
                    // Use baseline positioning; nudge away from edges a bit.
                    val xClamped = px.coerceIn(rect.left + paddingX, rect.right - paddingX)
                    val yClamped = (py - fm.descent).coerceIn(rect.top + paddingY - fm.ascent, rect.bottom - paddingY - fm.descent)
                    Triple(xClamped, yClamped, anchor.align)
                }
                is HintAnchor.Grid ->
                    when (anchor.position) {
                        HintGridPosition.TOP_LEFT ->
                            Triple(rect.left + paddingX, rect.top + paddingY - fm.ascent, Paint.Align.LEFT)

                        HintGridPosition.TOP_CENTER ->
                            Triple(rect.centerX(), rect.top + paddingY - fm.ascent, Paint.Align.CENTER)

                        HintGridPosition.TOP_RIGHT ->
                            Triple(rect.right - paddingX, rect.top + paddingY - fm.ascent, Paint.Align.RIGHT)

                        HintGridPosition.CENTER_LEFT ->
                            Triple(
                                rect.left + paddingX,
                                rect.centerY() + textHeight / 2f - fm.descent,
                                Paint.Align.LEFT,
                            )

                        HintGridPosition.CENTER ->
                            Triple(
                                rect.centerX(),
                                rect.centerY() + textHeight / 2f - fm.descent,
                                Paint.Align.CENTER,
                            )

                        HintGridPosition.CENTER_RIGHT ->
                            Triple(
                                rect.right - paddingX,
                                rect.centerY() + textHeight / 2f - fm.descent,
                                Paint.Align.RIGHT,
                            )

                        HintGridPosition.BOTTOM_LEFT ->
                            Triple(rect.left + paddingX, rect.bottom - paddingY - fm.descent, Paint.Align.LEFT)

                        HintGridPosition.BOTTOM_CENTER ->
                            Triple(rect.centerX(), rect.bottom - paddingY - fm.descent, Paint.Align.CENTER)

                        HintGridPosition.BOTTOM_RIGHT ->
                            Triple(rect.right - paddingX, rect.bottom - paddingY - fm.descent, Paint.Align.RIGHT)
                    }
            }

        hintPaint.textAlign = align
        canvas.drawText(text, x, y, hintPaint)
    }

    private fun hitTestKey(x: Float, y: Float): Key? {
        if (keys.isEmpty() || keyTouchRects.isEmpty()) return null
        for (key in keys) {
            if (layoutState.hiddenKeyIds.contains(key.keyId)) continue
            val rect = keyTouchRects[key.keyId] ?: continue
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
            val keysInRow = row.keys.sortedBy { it.ui.gridPosition.startCol }
            if (keysInRow.isEmpty()) return@forEach

            val usesWeight = keysInRow.any { it.ui.widthWeight != 1f }

            val startX = when (row.alignment) {
                xyz.xiao6.myboard.model.RowAlignment.LEFT -> leftPx + rowStartPaddingPx
                xyz.xiao6.myboard.model.RowAlignment.CENTER ->
                    if (usesWeight) {
                        leftPx + rowStartPaddingPx
                    } else {
                        val colCount =
                            keysInRow.maxOf {
                                val gp = it.ui.gridPosition
                                gp.startCol + gp.spanCols
                            }.coerceAtLeast(1)
                        val totalGaps = rowGapPx * (colCount - 1)
                        val cellWidth = ((rowAvailableWidth - totalGaps) / colCount).coerceAtLeast(0f)
                        val rowTotalWidth = (cellWidth * colCount) + totalGaps
                        leftPx + rowStartPaddingPx + ((rowAvailableWidth - rowTotalWidth) / 2f).coerceAtLeast(0f)
                    }
                xyz.xiao6.myboard.model.RowAlignment.JUSTIFY -> leftPx + rowStartPaddingPx
            }

            if (usesWeight) {
                val totalGaps = rowGapPx * (keysInRow.size - 1).coerceAtLeast(0)
                val available = (rowAvailableWidth - totalGaps).coerceAtLeast(0f)
                val totalWeight = keysInRow.sumOf { it.ui.widthWeight.toDouble() }.toFloat().coerceAtLeast(0.0001f)
                val unit = available / totalWeight
                var x = startX
                for (key in keysInRow) {
                    val w = unit * key.ui.widthWeight
                    result[key.keyId] = RectF(x, rowTop, x + w, rowTop + rowHeight)
                    x += w + rowGapPx
                }
            } else {
                val colCount =
                    keysInRow.maxOf {
                        val gp = it.ui.gridPosition
                        gp.startCol + gp.spanCols
                    }.coerceAtLeast(1)
                val totalGaps = rowGapPx * (colCount - 1)
                val cellWidth = ((rowAvailableWidth - totalGaps) / colCount).coerceAtLeast(0f)
                for (key in keysInRow) {
                    val gp = key.ui.gridPosition
                    val x = startX + gp.startCol * (cellWidth + rowGapPx)
                    val keyWidth = (cellWidth * gp.spanCols) + rowGapPx * (gp.spanCols - 1)
                    val rect = RectF(x, rowTop, x + keyWidth, rowTop + rowHeight)
                    result[key.keyId] = rect
                }
            }
        }

        return result
    }

    /**
     * Compute per-key touch rects (like FlorisBoard touchBounds) to avoid dead zones between keys.
     * Touch rects are in the same coordinate system as [keyRects] and cover the full keyboard area.
     */
    private fun computeTouchRects(visualRects: Map<String, RectF>): Map<String, RectF> {
        val layout = layoutModel ?: return emptyMap()
        if (visualRects.isEmpty()) return emptyMap()

        val rows = layout.rows
        val rowGapPx = layout.defaults.verticalGapDp.dpToPx()
        val rowBounds =
            rows.mapNotNull { row ->
                val rects = row.keys.mapNotNull { visualRects[it.keyId] }
                if (rects.isEmpty()) return@mapNotNull null
                val top = rects.minOf { it.top }
                val bottom = rects.maxOf { it.bottom }
                row.rowId to (top to bottom)
            }.toMap()

        val out = LinkedHashMap<String, RectF>(visualRects.size)
        rows.forEachIndexed { rowIndex, row ->
            val keysInRow = row.keys.sortedBy { it.ui.gridPosition.startCol }
            if (keysInRow.isEmpty()) return@forEachIndexed

            val (rowTop, rowBottom) = rowBounds[row.rowId] ?: return@forEachIndexed
            val touchTop =
                if (rowIndex == 0) 0f else (rowTop - rowGapPx / 2f).coerceAtLeast(0f)
            val touchBottom =
                if (rowIndex == rows.lastIndex) height.toFloat() else (rowBottom + rowGapPx / 2f).coerceAtMost(height.toFloat())

            val rects = keysInRow.mapNotNull { k -> visualRects[k.keyId]?.let { k.keyId to it } }
            rects.forEachIndexed { keyIndex, (keyId, rect) ->
                val left =
                    if (keyIndex == 0) 0f
                    else {
                        val prev = rects[keyIndex - 1].second
                        (prev.right + rect.left) / 2f
                    }
                val right =
                    if (keyIndex == rects.lastIndex) width.toFloat()
                    else {
                        val next = rects[keyIndex + 1].second
                        (rect.right + next.left) / 2f
                    }
                out[keyId] = RectF(left, touchTop, right, touchBottom)
            }
        }
        return out
    }

    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density
    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)
}
