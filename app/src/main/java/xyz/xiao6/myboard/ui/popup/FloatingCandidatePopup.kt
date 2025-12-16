package xyz.xiao6.myboard.ui.popup

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupWindow
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.candidate.CandidateView
import kotlin.math.max

/**
 * Floating candidate popup that does NOT occupy IME layout height.
 * It is shown above the keyboard as a PopupWindow and only appears when candidates are non-empty.
 */
class FloatingCandidatePopup(
    context: Context,
) {
    private val displayMetrics = context.resources.displayMetrics
    private val fixedHeightPx = dpInt(48f)
    private val expandButtonWidthPx = dpInt(48f)

    private val candidateView = CandidateView(context).apply {
        visibility = View.VISIBLE
    }

    private val expandButton = ImageButton(context).apply {
        layoutParams = FrameLayout.LayoutParams(expandButtonWidthPx, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.END
        }
        setBackgroundColor(Color.TRANSPARENT)
        // Use platform drawable to avoid adding resources.
        setImageResource(android.R.drawable.arrow_down_float)
        contentDescription = "Expand candidates"
        scaleType = ImageView.ScaleType.CENTER
        setOnClickListener { onExpandToggle?.invoke() }
    }

    private val content = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addView(candidateView)
        addView(expandButton)
    }

    private val popup =
        PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false).apply {
            isClippingEnabled = false
            isOutsideTouchable = false
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

    var onCandidateClick: ((String) -> Unit)?
        get() = candidateView.onCandidateClick
        set(value) {
            candidateView.onCandidateClick = value
        }

    var onExpandToggle: (() -> Unit)? = null

    var isExpanded: Boolean = false
        private set

    var lastMeasuredHeightPx: Int = 0
        private set

    fun applyTheme(theme: ThemeSpec?) {
        candidateView.applyTheme(theme)
    }

    fun dismiss() {
        popup.dismiss()
        lastMeasuredHeightPx = 0
    }

    fun setExpanded(expanded: Boolean) {
        isExpanded = expanded
        expandButton.setImageResource(
            if (expanded) android.R.drawable.arrow_up_float else android.R.drawable.arrow_down_float,
        )
        expandButton.contentDescription = if (expanded) "Collapse candidates" else "Expand candidates"
    }

    /**
     * Show candidates in-place, aligned to the anchor's top-left (used to "replace toolbar").
     */
    fun updateInSlot(anchor: View, candidates: List<String>, showExpandButton: Boolean) {
        if (candidates.isEmpty()) {
            dismiss()
            return
        }

        if (anchor.width == 0 || anchor.height == 0) {
            anchor.post { updateInSlot(anchor, candidates, showExpandButton) }
            return
        }

        expandButton.visibility = if (showExpandButton) View.VISIBLE else View.GONE
        candidateView.submitCandidates(candidates)
        measureContent(widthPx = anchor.width)

        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val x = loc[0]
        val y = loc[1]

        showOrUpdate(anchor, x, y)
    }

    private fun measureContent(widthPx: Int) {
        val w = max(dpInt(160f), widthPx)
        // PopupWindow wraps content into a FrameLayout; ensure LayoutParams are MarginLayoutParams-compatible.
        candidateView.layoutParams = FrameLayout.LayoutParams((w - expandButtonWidthPx).coerceAtLeast(dpInt(80f)), fixedHeightPx).apply {
            gravity = Gravity.START
        }
        expandButton.layoutParams = FrameLayout.LayoutParams(expandButtonWidthPx, fixedHeightPx).apply {
            gravity = Gravity.END
        }
        content.layoutParams = FrameLayout.LayoutParams(w, fixedHeightPx)
        content.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(fixedHeightPx, View.MeasureSpec.EXACTLY),
        )
        lastMeasuredHeightPx = fixedHeightPx
        popup.width = w
        popup.height = fixedHeightPx
    }

    private fun showOrUpdate(anchor: View, x: Int, y: Int) {
        if (!popup.isShowing) {
            popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        } else {
            popup.update(x, y, popup.width, popup.height)
        }
    }

    private fun dpInt(value: Float): Int = (value * displayMetrics.density).toInt()
}
