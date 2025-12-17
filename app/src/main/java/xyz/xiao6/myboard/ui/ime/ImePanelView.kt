package xyz.xiao6.myboard.ui.ime

import android.content.Context
import android.util.AttributeSet
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

        val targetKeyboardHeightPx =
            (screenHeightPx * layout.totalHeightRatio + (layout.totalHeightDpOffset * density)).roundToInt()
                .coerceAtLeast((density * 160f).roundToInt())

        val slot = contentSlot ?: return
        val slotLp = slot.layoutParams ?: return
        if (slotLp.height != targetKeyboardHeightPx) {
            slotLp.height = targetKeyboardHeightPx
            slot.layoutParams = slotLp
            slot.requestLayout()
        }

        val topBarHeightPx = resolveSlotHeightPx(topBarSlot, fallbackDp = 48f)
        val panelLp = layoutParams ?: return
        val targetPanelHeightPx = targetKeyboardHeightPx + topBarHeightPx
        if (panelLp.height != targetPanelHeightPx) {
            panelLp.height = targetPanelHeightPx
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
