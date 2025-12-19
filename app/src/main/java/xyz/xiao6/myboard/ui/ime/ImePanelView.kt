package xyz.xiao6.myboard.ui.ime

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.model.KeyboardLayout
import kotlin.math.roundToInt

/**
 * A unified panel that contains toolbar / candidate area and the keyboard layout region.
 * The keyboard region height is driven by [KeyboardLayout.totalHeightRatio] and [KeyboardLayout.totalHeightDpOffset].
 */
class ImePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var topBarSlot: View? = null
    private var contentSlot: View? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        topBarSlot = findViewById(R.id.topBarSlot)
        contentSlot = findViewById(R.id.contentSlot)
    }

    fun applyKeyboardLayoutSize(layout: KeyboardLayout) {
        val display = resources.displayMetrics
        val density = display.density
        val screenHeightPx = display.heightPixels
        val screenWidthPx = display.widthPixels

        val targetKeyboardHeightPx =
            (screenHeightPx * layout.totalHeightRatio + (layout.totalHeightDpOffset * density)).roundToInt()
                .coerceAtLeast((density * 160f).roundToInt())

        val targetKeyboardWidthPx =
            (screenWidthPx * layout.totalWidthRatio + (layout.totalWidthDpOffset * density)).roundToInt()
                .coerceIn((density * 240f).roundToInt(), screenWidthPx)

        val slot = contentSlot ?: return
        val slotLp = slot.layoutParams ?: return
        var changed = false
        if (slotLp.height != targetKeyboardHeightPx) {
            slotLp.height = targetKeyboardHeightPx
            changed = true
        }
        if (slotLp.width != targetKeyboardWidthPx) {
            slotLp.width = targetKeyboardWidthPx
            changed = true
        }
        if (changed) {
            slot.layoutParams = slotLp
            slot.requestLayout()
        }

        val topBarHeightPx = resolveSlotHeightPx(topBarSlot, fallbackDp = 48f)
        val panelLp = layoutParams ?: return
        val targetPanelHeightPx = targetKeyboardHeightPx + topBarHeightPx
        var panelChanged = false
        if (panelLp.height != targetPanelHeightPx) {
            panelLp.height = targetPanelHeightPx
            panelChanged = true
        }
        if (panelLp.width != targetKeyboardWidthPx) {
            panelLp.width = targetKeyboardWidthPx
            panelChanged = true
        }
        if (panelLp is FrameLayout.LayoutParams) {
            val desiredGravity = Gravity.CENTER_HORIZONTAL
            if (panelLp.gravity != desiredGravity) {
                panelLp.gravity = desiredGravity
                panelChanged = true
            }
        }
        if (panelChanged) {
            layoutParams = panelLp
            requestLayout()
        }
    }

    private fun resolveSlotHeightPx(slot: View?, fallbackDp: Float): Int {
        val fromLp = slot?.layoutParams?.height ?: 0
        if (fromLp > 0) return fromLp
        val measured = slot?.measuredHeight ?: 0
        if (measured > 0) return measured
        return (resources.displayMetrics.density * fallbackDp).roundToInt()
    }
}
