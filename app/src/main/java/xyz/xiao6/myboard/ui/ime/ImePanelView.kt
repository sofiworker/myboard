package xyz.xiao6.myboard.ui.ime

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.util.KeyboardSizeConstraints
import kotlin.math.roundToInt

/**
 * A unified panel that contains toolbar, keyboard, and the floating composing area.
 */
class ImePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var layerTop: FrameLayout? = null
    private var layerMiddle: View? = null
    private var layerBottom: View? = null
    private var layersContainer: View? = null

    private val floatingAreaHeightDp = 60f

    override fun onFinishInflate() {
        super.onFinishInflate()
        layerTop = findViewById(R.id.layerTop)
        layerMiddle = findViewById(R.id.layerMiddle)
        layerBottom = findViewById(R.id.layerBottom)
        layersContainer = findViewById(R.id.layersContainer)
    }

    /**
     * Returns the height of the actual keyboard area (Toolbar + Keyboard),
     * excluding the floating area.
     */
    fun getContentHeight(): Int {
        val density = resources.displayMetrics.density
        val toolbarHeightPx = resolveSlotHeightPx(layerMiddle, fallbackDp = 48f)
        val dividerHeightPx = if (findViewById<View>(R.id.toolbarDivider)?.visibility == View.VISIBLE) (density * 1f).roundToInt() else 0
        val keyboardHeightPx = layerBottom?.layoutParams?.height ?: 0
        return toolbarHeightPx + dividerHeightPx + keyboardHeightPx
    }
    
    /**
     * Returns the height of the reserved floating area at the top.
     */
    fun getFloatingAreaHeight(): Int {
        return (resources.displayMetrics.density * floatingAreaHeightDp).roundToInt()
    }

    fun applyKeyboardLayoutSize(layout: KeyboardLayout) {
        val display = resources.displayMetrics
        val density = display.density
        val screenHeightPx = display.heightPixels
        val screenWidthPx = display.widthPixels

        // 1. Calculate target keyboard height (Bottom Layer)
        val targetKeyboardHeightPx =
            (screenHeightPx * layout.totalHeightRatio + (layout.totalHeightDpOffset * density)).roundToInt()
                .coerceAtLeast(KeyboardSizeConstraints.minKeyboardHeightPx(density))
                .coerceAtMost(
                    KeyboardSizeConstraints.maxKeyboardHeightPx(screenHeightPx, density)
                        .coerceAtLeast(KeyboardSizeConstraints.minKeyboardHeightPx(density)),
                )

        val targetKeyboardWidthPx =
            (screenWidthPx * layout.totalWidthRatio + (layout.totalWidthDpOffset * density)).roundToInt()
                .coerceIn(KeyboardSizeConstraints.minKeyboardWidthPx(density), screenWidthPx)

        // 2. Apply size to Bottom Layer
        val bottom = layerBottom ?: return
        val bottomLp = bottom.layoutParams ?: return
        var bottomChanged = false
        if (bottomLp.height != targetKeyboardHeightPx) {
            bottomLp.height = targetKeyboardHeightPx
            bottomChanged = true
        }
        if (bottomLp.width != targetKeyboardWidthPx) {
            bottomLp.width = targetKeyboardWidthPx
            bottomChanged = true
        }
        if (bottomChanged) {
            bottom.layoutParams = bottomLp
            bottom.requestLayout()
        }

        // 3. Middle Layer (Toolbar) uses its own size
        layerMiddle?.let { middle ->
            val middleLp = middle.layoutParams
            if (middleLp != null && middleLp.width != targetKeyboardWidthPx) {
                middleLp.width = targetKeyboardWidthPx
                middle.layoutParams = middleLp
            }
        }

        // 4. Update the layers container width
        layersContainer?.let { container ->
            val containerLp = container.layoutParams
            if (containerLp != null && containerLp.width != targetKeyboardWidthPx) {
                containerLp.width = targetKeyboardWidthPx
                container.layoutParams = containerLp
            }
        }

        // 5. Anchor layerTop to the top of the actual content
        val toolbarHeightPx = resolveSlotHeightPx(layerMiddle, fallbackDp = 48f)
        val dividerHeightPx = if (findViewById<View>(R.id.toolbarDivider)?.visibility == View.VISIBLE) (density * 1f).roundToInt() else 0
        val bottomOffsetPx = targetKeyboardHeightPx + toolbarHeightPx + dividerHeightPx

        layerTop?.let { top ->
            val topLp = top.layoutParams
            if (topLp is LayoutParams) {
                var topChanged = false
                if (topLp.bottomMargin != bottomOffsetPx) {
                    topLp.bottomMargin = bottomOffsetPx
                    topChanged = true
                }
                if (topLp.gravity != (Gravity.BOTTOM or Gravity.START)) {
                    topLp.gravity = (Gravity.BOTTOM or Gravity.START)
                    topChanged = true
                }
                if (topChanged) {
                    top.layoutParams = topLp
                }
            }
        }

        // 6. Update the Panel (this view) itself
        val panelLp = layoutParams ?: return
        var panelChanged = false
        
        // Total panel height = Keyboard Area + Floating Area
        val floatingAreaHeightPx = getFloatingAreaHeight()
        val totalPanelHeight = bottomOffsetPx + floatingAreaHeightPx
        
        if (panelLp.width != LayoutParams.WRAP_CONTENT) { 
            panelLp.width = LayoutParams.WRAP_CONTENT
            panelChanged = true
        }
        
        if (panelLp.height != totalPanelHeight) {
            panelLp.height = totalPanelHeight
            panelChanged = true
        }

        if (panelLp is FrameLayout.LayoutParams) {
            val desiredGravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
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
