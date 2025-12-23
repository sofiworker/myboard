package xyz.xiao6.myboard.ui.popup

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import kotlin.math.max

/**
 * Floating text preview popup used for showing full candidate text on long-press.
 */
class FloatingTextPreviewPopup(
    context: Context,
) {
    private val displayMetrics = context.resources.displayMetrics

    private val textView = TextView(context).apply {
        textSize = 18f
        setTextColor(Color.BLACK)
        applyAppFont(bold = true)
        setPadding(dpInt(14f), dpInt(10f), dpInt(14f), dpInt(10f))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12f)
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
        val corner = dp(theme?.composingPopup?.surface?.cornerRadiusDp ?: 12f)
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

    fun showAbove(anchor: View, text: String, marginPx: Int) {
        val t = text.trim()
        if (t.isBlank()) {
            dismiss()
            return
        }
        if (anchor.width == 0 || anchor.height == 0) {
            anchor.post { showAbove(anchor, text, marginPx) }
            return
        }

        textView.text = t
        measureContent(maxWidthPx = (displayMetrics.widthPixels * 0.92f).toInt())

        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val desiredX = loc[0] + anchor.width / 2 - popup.width / 2
        val x = desiredX.coerceIn(marginPx, max(marginPx, displayMetrics.widthPixels - popup.width - marginPx))
        val y = loc[1] - popup.height - marginPx
        showOrUpdate(anchor, x, y)
    }

    private fun measureContent(maxWidthPx: Int) {
        val wSpec = View.MeasureSpec.makeMeasureSpec(maxWidthPx, View.MeasureSpec.AT_MOST)
        val hSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        textView.measure(wSpec, hSpec)
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

