package xyz.xiao6.myboard.ui.popup

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.AppFont
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import kotlin.math.max

/**
 * 辅助气泡/浮动层（View overlay）：按键预览 + 长按候选。
 * Popup overlay (View overlay): key preview + long-press candidates.
 *
 * 约束 / Constraints:
 * - 预览气泡：ACTION_DOWN 后 100ms 内显示，ACTION_UP 或移出按键区域立刻消失
 * - 长按候选：可接收触摸，MOVE 高亮，UP 提交选中项
 */
class PopupView(
    private val host: FrameLayout,
) {
    private val context = host.context
    private var themeSpec: ThemeSpec? = null
    private var theme: ThemeRuntime? = null

    private val previewTextView = buildPreviewTextView()
    private val candidatesContainer = CandidateStripView(context)

    private var isPreviewShowing: Boolean = false
    private var isCandidatesShowing: Boolean = false

    fun applyTheme(theme: ThemeSpec?) {
        themeSpec = theme
        this.theme = theme?.let { ThemeRuntime(it) }

        val runtime = this.theme
        val surfaceBg = runtime?.resolveColor(theme?.keyPopup?.surface?.background?.color, Color.parseColor("#CC000000"))
            ?: Color.parseColor("#CC000000")
        val strokeColor = runtime?.resolveColor(theme?.keyPopup?.surface?.stroke?.color, Color.parseColor("#55FFFFFF"))
            ?: Color.parseColor("#55FFFFFF")
        val textColor = runtime?.resolveColor(theme?.keyPopup?.text?.color, Color.WHITE) ?: Color.WHITE
        previewTextView.setTextColor(textColor)
        (previewTextView.background as? GradientDrawable)?.apply {
            setColor(surfaceBg)
            setStroke(dpInt(theme?.keyPopup?.surface?.stroke?.widthDp ?: 1f), strokeColor)
        }

        candidatesContainer.applyTheme(theme, runtime)
    }

    fun dismissAll() {
        dismissPreview()
        dismissCandidates()
    }

    fun dismissPreview() {
        if (!isPreviewShowing) return
        isPreviewShowing = false
        previewTextView.visibility = View.GONE
    }

    fun dismissCandidates() {
        if (!isCandidatesShowing) return
        isCandidatesShowing = false
        candidatesContainer.visibility = View.GONE
        candidatesContainer.reset()
    }

    fun showKeyPreview(
        anchor: View,
        keyRectInAnchor: RectF,
        text: String,
        forceAbove: Boolean = false,
    ) {
        if (host.width == 0 || host.height == 0) {
            host.post { showKeyPreview(anchor, keyRectInAnchor, text, forceAbove) }
            return
        }

        ensureAttached(previewTextView)
        previewTextView.text = text
        previewTextView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
        val w = previewTextView.measuredWidth
        val h = previewTextView.measuredHeight

        val pos = computePopupPosition(
            anchor = anchor,
            keyRectInAnchor = keyRectInAnchor,
            popupWidth = w,
            popupHeight = h,
            preferAbovePadding = dpInt(6f),
            forceAbove = forceAbove,
        )

        (previewTextView.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
            lp.leftMargin = pos.x
            lp.topMargin = pos.y
            previewTextView.layoutParams = lp
        }

        previewTextView.visibility = View.VISIBLE
        previewTextView.bringToFront()
        isPreviewShowing = true
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
        if (host.width == 0 || host.height == 0) {
            host.post { showLongPressCandidates(anchor, keyRectInAnchor, candidates, onCommit) }
            return
        }

        ensureAttached(candidatesContainer)
        candidatesContainer.setCandidates(candidates)
        candidatesContainer.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)

        val w = candidatesContainer.measuredWidth
        val h = candidatesContainer.measuredHeight

        candidatesContainer.onCommit = onCommit
        val pos = computePopupPosition(
            anchor = anchor,
            keyRectInAnchor = keyRectInAnchor,
            popupWidth = w,
            popupHeight = h,
            preferAbovePadding = dpInt(10f),
        )

        (candidatesContainer.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
            lp.leftMargin = pos.x
            lp.topMargin = pos.y
            candidatesContainer.layoutParams = lp
        }

        candidatesContainer.visibility = View.VISIBLE
        candidatesContainer.bringToFront()
        isCandidatesShowing = true
    }

    /**
     * 将触摸事件转发给候选列表（用于 MOVE 选择、UP 提交）。
     * Returns true if the candidates popup is consuming the gesture.
     */
    fun dispatchCandidatesTouch(anchor: View, event: MotionEvent): Boolean {
        if (!isCandidatesShowing) return false
        if (candidatesContainer.visibility != View.VISIBLE) return false

        val anchorLoc = IntArray(2)
        anchor.getLocationInWindow(anchorLoc)
        val windowX = anchorLoc[0] + event.x
        val windowY = anchorLoc[1] + event.y

        val candidatesLoc = IntArray(2)
        candidatesContainer.getLocationInWindow(candidatesLoc)
        val localX = windowX - candidatesLoc[0]
        val localY = windowY - candidatesLoc[1]

        if (localX < 0f || localY < 0f || localX > candidatesContainer.width.toFloat() || localY > candidatesContainer.height.toFloat()) {
            return false
        }

        val adjusted =
            MotionEvent.obtain(
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

    fun isCandidatesShowing(): Boolean = isCandidatesShowing

    private fun buildPreviewTextView(): TextView {
        return TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 28f
            applyAppFont(bold = true)
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

    private data class PopupPosition(val x: Int, val y: Int)

    private fun ensureAttached(view: View) {
        if (view.parent === host) return
        if (view.parent is ViewGroup) (view.parent as ViewGroup).removeView(view)
        host.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        view.visibility = View.GONE
    }

    private fun computePopupPosition(
        anchor: View,
        keyRectInAnchor: RectF,
        popupWidth: Int,
        popupHeight: Int,
        preferAbovePadding: Int,
        forceAbove: Boolean = false,
    ): PopupPosition {
        val anchorLoc = IntArray(2)
        anchor.getLocationInWindow(anchorLoc)

        val hostLoc = IntArray(2)
        host.getLocationInWindow(hostLoc)

        val anchorXInHost = anchorLoc[0] - hostLoc[0]
        val anchorYInHost = anchorLoc[1] - hostLoc[1]

        val keyCenterX = anchorXInHost + keyRectInAnchor.centerX()
        val desiredX = (keyCenterX - popupWidth / 2f).toInt()

        val aboveY = (anchorYInHost + keyRectInAnchor.top - popupHeight - preferAbovePadding).toInt()
        val belowY = (anchorYInHost + keyRectInAnchor.bottom + preferAbovePadding).toInt()

        val x = clampX(desiredX, popupWidth)
        val y = if (forceAbove) clampYAboveOnly(aboveY, popupHeight) else clampYPreferAbove(aboveY, belowY, popupHeight)
        return PopupPosition(x = x, y = y)
    }

    private fun clampX(desiredX: Int, popupWidth: Int): Int {
        val minX = dpInt(4f)
        val maxX = host.width - popupWidth - dpInt(4f)
        return desiredX.coerceIn(minX, max(minX, maxX))
    }

    private fun clampYPreferAbove(aboveY: Int, belowY: Int, popupHeight: Int): Int {
        val minY = dpInt(4f)
        val maxY = host.height - popupHeight - dpInt(4f)
        val clampedAbove = aboveY.coerceIn(minY, max(minY, maxY))
        if (aboveY >= minY) return clampedAbove
        return belowY.coerceIn(minY, max(minY, maxY))
    }

    private fun clampYAboveOnly(aboveY: Int, popupHeight: Int): Int {
        val minY = dpInt(4f)
        val maxY = host.height - popupHeight - dpInt(4f)
        return aboveY.coerceIn(minY, max(minY, maxY))
    }

    private class CandidateStripView(context: Context) : LinearLayout(context) {
        var onCommit: ((String) -> Unit)? = null

        private var candidates: List<String> = emptyList()
        private var selectedIndex: Int = 0
        private var hasTouchedSelection: Boolean = false
        private var itemTextColor: Int = Color.WHITE
        private var itemSelectedBg: Int = Color.parseColor("#FF3A7AFE")
        private var itemSelectedText: Int = Color.WHITE

        init {
            orientation = HORIZONTAL
            setPadding(dpInt(8f), dpInt(8f), dpInt(8f), dpInt(8f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12f)
                setColor(Color.parseColor("#EE1F1F1F"))
                setStroke(dpInt(1f), Color.parseColor("#55FFFFFF"))
            }
            isClickable = true
        }

        fun applyTheme(theme: ThemeSpec?, runtime: ThemeRuntime?) {
            val surfaceBg = runtime?.resolveColor(theme?.keyPopup?.surface?.background?.color, Color.parseColor("#EE1F1F1F"))
                ?: Color.parseColor("#EE1F1F1F")
            val strokeColor = runtime?.resolveColor(theme?.keyPopup?.surface?.stroke?.color, Color.parseColor("#55FFFFFF"))
                ?: Color.parseColor("#55FFFFFF")
            val strokeWidth = dpInt(theme?.keyPopup?.surface?.stroke?.widthDp ?: 1f)
            (background as? GradientDrawable)?.apply {
                setColor(surfaceBg)
                setStroke(strokeWidth, strokeColor)
            }
            itemTextColor = runtime?.resolveColor(theme?.keyPopup?.text?.color, Color.WHITE) ?: Color.WHITE
            itemSelectedText = runtime?.resolveColor(theme?.keyPopup?.textSelected?.color, Color.WHITE) ?: Color.WHITE
            itemSelectedBg = runtime?.resolveColor("colors.accent", Color.parseColor("#FF3A7AFE")) ?: Color.parseColor("#FF3A7AFE")
            refreshSelection()
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            return dispatchTouch(event)
        }

        fun setCandidates(list: List<String>) {
            candidates = list
            selectedIndex = 0
            hasTouchedSelection = false
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
            hasTouchedSelection = false
            removeAllViews()
        }

        fun dispatchTouch(event: MotionEvent): Boolean {
            if (candidates.isEmpty()) return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val index = hitTestIndex(event.x)
                    if (index != null) {
                        selectedIndex = index
                        hasTouchedSelection = true
                        refreshSelection()
                        return true
                    }
                    // If user didn't actually touch a candidate, don't consume this gesture.
                    return hasTouchedSelection
                }

                MotionEvent.ACTION_UP -> {
                    val hitIndex = hitTestIndex(event.x)
                    if (hitIndex == null && !hasTouchedSelection) {
                        // No intentional selection gesture happened inside the popup, don't consume.
                        return false
                    }
                    val index = hitIndex ?: selectedIndex
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
                setTextColor(itemTextColor)
                this.text = text
                applyAppFont()
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
                bg.setColor(if (selected) itemSelectedBg else Color.parseColor("#00000000"))
                child.setTextColor(if (selected) itemSelectedText else itemTextColor)
                child.typeface = if (selected) AppFont.bold(child.context) else AppFont.regular(child.context)
            }
        }

        private fun dp(value: Float): Float = value * context.resources.displayMetrics.density
        private fun dpInt(value: Float): Int = max(0, dp(value).toInt())
    }
}
