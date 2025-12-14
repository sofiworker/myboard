package xyz.xiao6.myboard.ui.popup

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import xyz.xiao6.myboard.util.MLog
import kotlin.math.max

/**
 * 辅助气泡/浮动层（PopupWindow）：按键预览 + 长按候选。
 * Popup overlay (PopupWindow): key preview + long-press candidates.
 *
 * 约束 / Constraints:
 * - 预览气泡：ACTION_DOWN 后 100ms 内显示，ACTION_UP 或移出按键区域立刻消失
 * - 长按候选：可接收触摸，MOVE 高亮，UP 提交选中项
 */
class PopupView(
    private val context: Context,
) {
    private val logTag = "PopupView"
    private val previewTextView = buildPreviewTextView()
    private val previewWindow = PopupWindow(
        previewTextView,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        false,
    ).apply {
        isClippingEnabled = false
        // Ensure it can be shown from IME window.
        windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
    }

    private val candidatesContainer = CandidateStripView(context)
    private val candidatesWindow = PopupWindow(
        candidatesContainer,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true,
    ).apply {
        isClippingEnabled = false
        // Ensure it can be shown from IME window.
        windowLayoutType = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
        inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
    }

    private var candidatesWindowX: Int = 0
    private var candidatesWindowY: Int = 0

    fun dismissAll() {
        dismissPreview()
        dismissCandidates()
    }

    fun dismissPreview() {
        if (previewWindow.isShowing) previewWindow.dismiss()
    }

    fun dismissCandidates() {
        if (candidatesWindow.isShowing) candidatesWindow.dismiss()
        candidatesContainer.reset()
    }

    fun showKeyPreview(anchor: View, keyRectInAnchor: RectF, text: String) {
        previewTextView.text = text
        previewTextView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val w = previewTextView.measuredWidth
        val h = previewTextView.measuredHeight

        val location = IntArray(2)
        // IMPORTANT: in IME (InputMethodService) the PopupWindow is positioned in window coordinates,
        // not global screen coordinates, so use location-in-window.
        anchor.getLocationInWindow(location)

        val keyCenterX = location[0] + keyRectInAnchor.centerX()
        val desiredX = (keyCenterX - w / 2f).toInt()
        val aboveY = (location[1] + keyRectInAnchor.top - h - dp(6f)).toInt()
        val belowY = (location[1] + keyRectInAnchor.bottom + dp(6f)).toInt()

        val frame = visibleFrameInWindow(anchor)
        val x = clampX(desiredX, w, frame)
        val y = clampYPreferAbove(aboveY, belowY, h, frame)

        if (previewWindow.isShowing) {
            previewWindow.update(x, y, -1, -1)
        } else {
            runCatching {
                previewWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
            }.onFailure { t ->
                MLog.w(logTag, "showKeyPreview failed", t)
            }
        }
    }

    /**
     * 显示长按候选列表。
     * @param candidates 候选字符串列表
     * @param onCommit 手指抬起时提交选中项
     */
    fun showLongPressCandidates(
        anchor: View,
        keyRectInAnchor: RectF,
        candidates: List<String>,
        onCommit: (String) -> Unit,
    ) {
        candidatesContainer.setCandidates(candidates)
        candidatesContainer.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)

        val w = candidatesContainer.measuredWidth
        val h = candidatesContainer.measuredHeight

        val location = IntArray(2)
        anchor.getLocationInWindow(location)

        val keyCenterX = location[0] + keyRectInAnchor.centerX()
        val desiredX = (keyCenterX - w / 2f).toInt()
        val aboveY = (location[1] + keyRectInAnchor.top - h - dp(10f)).toInt()
        val belowY = (location[1] + keyRectInAnchor.bottom + dp(10f)).toInt()

        val frame = visibleFrameInWindow(anchor)
        val x = clampX(desiredX, w, frame)
        val y = clampYPreferAbove(aboveY, belowY, h, frame)

        candidatesContainer.onCommit = onCommit
        candidatesWindowX = x
        candidatesWindowY = y

        if (candidatesWindow.isShowing) {
            candidatesWindow.update(x, y, -1, -1)
        } else {
            runCatching {
                candidatesWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
            }.onFailure { t ->
                MLog.w(logTag, "showLongPressCandidates failed", t)
            }
        }
    }

    /**
     * 将触摸事件转发给候选列表（用于 MOVE 选择、UP 提交）。
     * Returns true if the candidates popup is consuming the gesture.
     */
    fun dispatchCandidatesTouch(anchor: View, event: MotionEvent): Boolean {
        if (!candidatesWindow.isShowing) return false
        val anchorLoc = IntArray(2)
        anchor.getLocationInWindow(anchorLoc)
        val windowX = anchorLoc[0] + event.x
        val windowY = anchorLoc[1] + event.y
        val localX = windowX - candidatesWindowX
        val localY = windowY - candidatesWindowY

        val adjusted = MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            event.actionMasked,
            localX,
            localY,
            event.metaState,
        )
        adjusted.source = event.source
        val handled = candidatesContainer.dispatchTouch(adjusted)
        adjusted.recycle()
        return handled
    }

    fun isCandidatesShowing(): Boolean = candidatesWindow.isShowing

    private fun buildPreviewTextView(): TextView {
        return TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dpInt(18f), dpInt(12f), dpInt(18f), dpInt(12f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12f)
                setColor(Color.parseColor("#CC000000"))
                setStroke(dpInt(1f), Color.parseColor("#55FFFFFF"))
            }
        }
    }

    private fun dp(value: Float): Float = value * context.resources.displayMetrics.density
    private fun dpInt(value: Float): Int = dp(value).toInt()

    private fun visibleFrameInWindow(anchor: View): Rect {
        // Constrain popups inside the IME window. Using screen-visible frame can push the popup
        // outside the IME window top boundary and make it invisible.
        val root = anchor.rootView ?: anchor
        val w = root.width.takeIf { it > 0 } ?: context.resources.displayMetrics.widthPixels
        val h = root.height.takeIf { it > 0 } ?: context.resources.displayMetrics.heightPixels
        return Rect(0, 0, w, h)
    }

    private fun clampX(desiredX: Int, popupWidth: Int, frame: Rect): Int {
        val minX = frame.left + dpInt(4f)
        val maxX = frame.right - popupWidth - dpInt(4f)
        return desiredX.coerceIn(minX, max(minX, maxX))
    }

    private fun clampYPreferAbove(aboveY: Int, belowY: Int, popupHeight: Int, frame: Rect): Int {
        val minY = frame.top + dpInt(4f)
        val maxY = frame.bottom - popupHeight - dpInt(4f)
        val clampedAbove = aboveY.coerceIn(minY, max(minY, maxY))
        if (aboveY >= minY) return clampedAbove
        return belowY.coerceIn(minY, max(minY, maxY))
        // IME UX: prefer showing popups above the key. If there's no space above, clamp to the top
        // of the IME window (which overlaps the candidate bar) instead of falling back below.
        // return aboveY.coerceIn(minY, max(minY, maxY))
    }

    private class CandidateStripView(context: Context) : LinearLayout(context) {
        var onCommit: ((String) -> Unit)? = null

        private var candidates: List<String> = emptyList()
        private var selectedIndex: Int = 0

        init {
            orientation = HORIZONTAL
            setPadding(dpInt(8f), dpInt(8f), dpInt(8f), dpInt(8f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12f)
                setColor(Color.parseColor("#EE1F1F1F"))
                setStroke(dpInt(1f), Color.parseColor("#55FFFFFF"))
            }
        }

        fun setCandidates(list: List<String>) {
            candidates = list
            selectedIndex = 0
            removeAllViews()
            for ((i, c) in list.withIndex()) {
                addView(buildItem(i, c))
            }
            refreshSelection()
        }

        fun reset() {
            onCommit = null
            candidates = emptyList()
            selectedIndex = 0
            removeAllViews()
        }

        fun dispatchTouch(event: MotionEvent): Boolean {
            if (candidates.isEmpty()) return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val index = hitTestIndex(event.x)
                    if (index != null) {
                        selectedIndex = index
                        refreshSelection()
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    val index = hitTestIndex(event.x) ?: selectedIndex
                    val text = candidates.getOrNull(index) ?: return true
                    onCommit?.invoke(text)
                    return true
                }

                MotionEvent.ACTION_CANCEL -> return true
            }
            return false
        }

        private fun hitTestIndex(x: Float): Int? {
            var left = paddingLeft.toFloat()
            for (i in candidates.indices) {
                val child = getChildAt(i) ?: continue
                val right = left + child.measuredWidth
                if (x in left..right) return i
                left = right
            }
            return null
        }

        private fun buildItem(index: Int, text: String): TextView {
            return TextView(context).apply {
                layoutParams =
                    LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                        marginEnd = if (index == candidates.lastIndex) 0 else dpInt(6f)
                    }
                minWidth = dpInt(44f)
                setPadding(dpInt(12f), dpInt(10f), dpInt(12f), dpInt(10f))
                gravity = Gravity.CENTER
                textSize = 18f
                setTextColor(Color.WHITE)
                this.text = text
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(10f)
                    setColor(Color.parseColor("#00000000"))
                }
            }
        }

        private fun refreshSelection() {
            for (i in 0 until childCount) {
                val child = getChildAt(i) as? TextView ?: continue
                val bg = (child.background as? GradientDrawable) ?: continue
                val selected = i == selectedIndex
                bg.setColor(if (selected) Color.parseColor("#FF3A7AFE") else Color.parseColor("#00000000"))
                child.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }

        private fun dp(value: Float): Float = value * context.resources.displayMetrics.density
        private fun dpInt(value: Float): Int = max(0, dp(value).toInt())
    }
}
