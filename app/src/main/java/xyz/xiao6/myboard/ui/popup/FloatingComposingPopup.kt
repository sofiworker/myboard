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

    var onClick: (() -> Unit)? = null
        set(value) {
            field = value
            textView.isClickable = value != null
            textView.setOnClickListener { field?.invoke() }
        }
    var onCursorMove: ((Int) -> Unit)? = null

    private var rawText: String = ""
    private var displayText: String = ""
    private var editing: Boolean = false
    private var cursorIndex: Int = 0

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
    fun update(
        anchor: View,
        composing: String,
        displayText: String = composing,
        xOffsetPx: Int,
        yOffsetPx: Int,
        editing: Boolean = false,
        cursorIndex: Int = 0,
    ) {
        val display = displayText(displayText, composing, editing, cursorIndex)
        if (display.isBlank()) {
            dismiss()
            return
        }

        if (anchor.width == 0 || anchor.height == 0) {
            anchor.post { update(anchor, composing, displayText, xOffsetPx, yOffsetPx) }
            return
        }

        textView.text = display
        updateEditingState(composing, displayText, editing, cursorIndex)
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
    fun updateAbove(
        anchor: View,
        composing: String,
        displayText: String = composing,
        xMarginPx: Int,
        yMarginPx: Int,
        editing: Boolean = false,
        cursorIndex: Int = 0,
    ) {
        val display = displayText(displayText, composing, editing, cursorIndex)
        if (display.isBlank()) {
            dismiss()
            return
        }
        if (anchor.width == 0 || anchor.height == 0) {
            anchor.post { updateAbove(anchor, composing, displayText, xMarginPx, yMarginPx) }
            return
        }

        textView.text = display
        updateEditingState(composing, displayText, editing, cursorIndex)
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

    private fun displayText(displayText: String, rawText: String, editing: Boolean, cursorIndex: Int): String {
        val trimmed = displayText.trim()
        if (!editing) return trimmed
        if (trimmed.isBlank()) return "|"
        val rawLen = countRawCodePoints(trimmed)
        val safeCursor = cursorIndex.coerceIn(0, rawLen)
        val offset = rawCursorToDisplayOffset(trimmed, safeCursor)
        return trimmed.substring(0, offset) + "|" + trimmed.substring(offset)
    }

    private fun updateEditingState(rawText: String, displayText: String, editing: Boolean, cursorIndex: Int) {
        this.rawText = rawText.trim()
        this.displayText = displayText.trim()
        this.editing = editing
        val rawLen = countRawCodePoints(this.displayText)
        this.cursorIndex = cursorIndex.coerceIn(0, rawLen)
        if (!editing) {
            textView.setOnTouchListener(null)
            return
        }
        textView.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE,
                -> {
                    val index = resolveCursorIndex(ev.x)
                    onCursorMove?.invoke(index)
                    true
                }
                else -> false
            }
        }
    }

    private fun resolveCursorIndex(x: Float): Int {
        val text = displayText
        if (text.isBlank()) return 0
        val paint = textView.paint
        val contentX = (x - textView.paddingLeft).coerceAtLeast(0f)
        var offset = 0
        var index = 0
        var width = 0f
        while (offset < text.length) {
            val cp = text.codePointAt(offset)
            val ch = String(Character.toChars(cp))
            val w = paint.measureText(ch)
            if (contentX <= width + w / 2f) {
                return displayIndexToRaw(text, index)
            }
            width += w
            offset += Character.charCount(cp)
            index += 1
        }
        return displayIndexToRaw(text, index)
    }

    private fun countRawCodePoints(text: String): Int {
        var count = 0
        var offset = 0
        while (offset < text.length) {
            val cp = text.codePointAt(offset)
            if (cp != '\''.code) count += 1
            offset += Character.charCount(cp)
        }
        return count
    }

    private fun rawCursorToDisplayOffset(displayText: String, rawCursor: Int): Int {
        var rawCount = 0
        var offset = 0
        while (offset < displayText.length) {
            if (rawCount == rawCursor) return offset
            val cp = displayText.codePointAt(offset)
            if (cp != '\''.code) rawCount += 1
            offset += Character.charCount(cp)
        }
        return displayText.length
    }

    private fun displayIndexToRaw(displayText: String, displayIndex: Int): Int {
        var rawCount = 0
        var offset = 0
        var index = 0
        while (offset < displayText.length && index < displayIndex) {
            val cp = displayText.codePointAt(offset)
            if (cp != '\''.code) rawCount += 1
            offset += Character.charCount(cp)
            index += 1
        }
        return rawCount
    }

    private fun dp(value: Float): Float = value * displayMetrics.density
    private fun dpInt(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics).toInt()
}
