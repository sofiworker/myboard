package xyz.xiao6.myboard.ui.popup

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.graphics.drawable.ColorDrawable
import android.widget.PopupWindow
import android.widget.TextView
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.ThemeRuntime

/**
 * Floating composing popup (top-left bubble) shown above the keyboard via PopupWindow.
 */
class FloatingComposingPopup(
    context: Context,
) {
    private val displayMetrics = context.resources.displayMetrics

    private val textView = TextView(context).apply {
        textSize = 18f
        setTextColor(Color.BLACK)
        typeface = Typeface.DEFAULT
        setPadding(dpInt(14f), dpInt(10f), dpInt(14f), dpInt(10f))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10f)
            setColor(Color.WHITE)
            setStroke(dpInt(1f), Color.parseColor("#14000000"))
        }
    }

    private val popup =
        PopupWindow(textView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false).apply {
            isClippingEnabled = false
            isOutsideTouchable = false
            inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        val bg = runtime?.resolveColor(theme?.composingPopup?.surface?.background?.color, Color.WHITE) ?: Color.WHITE
        val strokeColor = runtime?.resolveColor(theme?.composingPopup?.surface?.stroke?.color, Color.parseColor("#14000000"))
            ?: Color.parseColor("#14000000")
        val strokeWidth = dpInt(theme?.composingPopup?.surface?.stroke?.widthDp ?: 1f)
        val corner = dp(theme?.composingPopup?.surface?.cornerRadiusDp ?: 10f)
        val textColor = runtime?.resolveColor(theme?.composingPopup?.text?.color, Color.BLACK) ?: Color.BLACK

        textView.setTextColor(textColor)
        (textView.background as? GradientDrawable)?.apply {
            cornerRadius = corner
            setColor(bg)
            setStroke(strokeWidth, strokeColor)
        }
    }

    fun dismiss() {
        popup.dismiss()
    }

    /**
     * Show composing bubble within an overall anchor (IME root), positioned by offsets from the anchor's top-left.
     */
    fun update(anchor: View, composing: String, xOffsetPx: Int, yOffsetPx: Int) {
        val text = composing.trim()
        if (text.isBlank()) {
            dismiss()
            return
        }

        if (anchor.width == 0 || anchor.height == 0) {
            anchor.post { update(anchor, composing, xOffsetPx, yOffsetPx) }
            return
        }

        textView.text = text
        measureContent()

        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val x = loc[0] + xOffsetPx
        val y = loc[1] + yOffsetPx
        showOrUpdate(anchor, x, y)
    }

    /**
     * Show composing bubble above the anchor's top-left (like Gboard's "ni'hao" bubble).
     */
    fun updateAbove(anchor: View, composing: String, xMarginPx: Int, yMarginPx: Int) {
        val text = composing.trim()
        if (text.isBlank()) {
            dismiss()
            return
        }
        if (anchor.width == 0 || anchor.height == 0) {
            anchor.post { updateAbove(anchor, composing, xMarginPx, yMarginPx) }
            return
        }

        textView.text = text
        measureContent()

        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val x = loc[0] + xMarginPx
        val y = loc[1] - popup.height - yMarginPx
        showOrUpdate(anchor, x, y)
    }

    private fun measureContent() {
        textView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        popup.width = textView.measuredWidth
        popup.height = textView.measuredHeight
    }

    private fun showOrUpdate(anchor: View, x: Int, y: Int) {
        if (!popup.isShowing) {
            popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        } else {
            popup.update(x, y, popup.width, popup.height)
        }
    }

    private fun dp(value: Float): Float = value * displayMetrics.density
    private fun dpInt(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).toInt()
}
