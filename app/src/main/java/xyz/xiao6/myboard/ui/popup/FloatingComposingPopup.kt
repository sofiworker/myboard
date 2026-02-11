package xyz.xiao6.myboard.ui.popup

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.widget.TextView
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime

/**
 * Inline composing bubble/bar shown within layerTop.
 */
class FloatingComposingPopup(
    context: Context,
    private val container: ViewGroup,
) {
    private val displayMetrics = context.resources.displayMetrics
    private val screenWidth = displayMetrics.widthPixels

    private var lastTheme: ThemeSpec? = null

    // Background drawables
    private val bubbleBgDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(Color.WHITE)
    }

    private val editBgDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(Color.WHITE)
        cornerRadius = dp(8f)
        setStroke(dpInt(1.5f), Color.parseColor("#1A73E8"))
    }

    // Root container
    private val rootContainer = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM
        }
        visibility = View.GONE
        clipChildren = false
    }

    private val hScrollView = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    private val vScrollView = ScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    private val textView = TextView(context).apply {
        textSize = 15f
        setTextColor(Color.parseColor("#3C4043"))
        applyAppFont()
        setPadding(dpInt(12f), dpInt(8f), dpInt(12f), dpInt(8f))
        minHeight = dpInt(36f)
        gravity = Gravity.CENTER_VERTICAL
        background = bubbleBgDrawable
    }

    private val editWrapper = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        visibility = View.GONE
        background = editBgDrawable
    }

    private val backButton = ImageView(context).apply {
        // Use arrow_down_s_line to represent "collapse" and match toolbar line style
        setImageResource(R.drawable.arrow_down_s_line)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding(dpInt(12f), dpInt(10f), dpInt(12f), dpInt(10f))
        layoutParams = LinearLayout.LayoutParams(dpInt(48f), dpInt(48f))
        isClickable = true
        isFocusable = true
        
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        setBackgroundResource(outValue.resourceId)
    }

    init {
        hScrollView.addView(textView)
        rootContainer.addView(hScrollView)
        
        // Setup initial structure of editWrapper (will be re-ordered in update)
        rootContainer.addView(editWrapper)
        
        container.addView(rootContainer)
    }

    var onClick: (() -> Unit)? = null
        set(value) {
            field = value
            textView.isClickable = value != null
            textView.setOnClickListener { field?.invoke() }
        }
    
    var onBack: (() -> Unit)? = null
        set(value) {
            field = value
            backButton.setOnClickListener { field?.invoke() }
        }

    var onCursorMove: ((Int) -> Unit)? = null

    private var rawText: String = ""
    private var displayText: String = ""
    private var editing: Boolean = false
    private var cursorIndex: Int = 0

    fun applyTheme(theme: ThemeSpec?) {
        lastTheme = theme ?: lastTheme
        val themeToUse = lastTheme
        val runtime = themeToUse?.let { ThemeRuntime(it) }
        
        val bg = runtime?.resolveColor(themeToUse?.composingPopup?.surface?.background?.color, Color.WHITE) ?: Color.WHITE
        val strokeColor = runtime?.resolveColor(themeToUse?.composingPopup?.surface?.stroke?.color, Color.parseColor("#14000000"))
            ?: Color.parseColor("#14000000")
        val textColor = runtime?.resolveColor(themeToUse?.composingPopup?.text?.color, Color.BLACK) ?: Color.BLACK

        textView.setTextColor(textColor)
        backButton.setColorFilter(textColor)
        
        // Refresh bubble drawable
        bubbleBgDrawable.setColor(bg)
        bubbleBgDrawable.setStroke(dpInt(1f), strokeColor)
        val r = dp(12f)
        bubbleBgDrawable.cornerRadii = floatArrayOf(0f, 0f, r, r, 0f, 0f, 0f, 0f)

        // Refresh edit drawable
        editBgDrawable.setColor(bg)
        editBgDrawable.setStroke(dpInt(1.5f), Color.parseColor("#1A73E8"))
    }

    fun dismiss() {
        rootContainer.visibility = View.GONE
    }

    fun update(
        composing: String,
        displayText: String = composing,
        editing: Boolean = false,
        cursorIndex: Int = 0,
    ) {
        val display = displayText(displayText, composing, editing, cursorIndex)
        if (display.isBlank()) {
            dismiss()
            return
        }

        val wasEditing = this.editing
        this.editing = editing
        this.cursorIndex = cursorIndex
        
        textView.text = display
        rootContainer.visibility = View.VISIBLE

        if (editing != wasEditing || textView.parent == null) {
            (textView.parent as? ViewGroup)?.removeView(textView)
            (vScrollView.parent as? ViewGroup)?.removeView(vScrollView)
            (backButton.parent as? ViewGroup)?.removeView(backButton)
            
            if (editing) {
                hScrollView.visibility = View.GONE
                editWrapper.visibility = View.VISIBLE
                
                textView.background = null 
                vScrollView.addView(textView)
                
                // [ Text (Left) ] [ BackButton (Right) ]
                editWrapper.removeAllViews()
                editWrapper.addView(vScrollView)
                editWrapper.addView(backButton)
            } else {
                hScrollView.visibility = View.VISIBLE
                editWrapper.visibility = View.GONE
                
                textView.background = bubbleBgDrawable
                hScrollView.addView(textView)
            }
        }

        val rootLp = rootContainer.layoutParams as FrameLayout.LayoutParams
        if (editing) {
            rootLp.width = screenWidth - dpInt(24f)
            rootLp.leftMargin = dpInt(12f)
            rootLp.bottomMargin = dpInt(8f)
            
            textView.maxLines = Int.MAX_VALUE
            textView.setHorizontallyScrolling(false)
            textView.maxWidth = Int.MAX_VALUE
            textView.maxHeight = dpInt(84f)
            textView.setPadding(dpInt(16f), dpInt(10f), 0, dpInt(10f))
            
            vScrollView.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        } else {
            rootLp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            rootLp.leftMargin = 0
            rootLp.bottomMargin = 0
            
            textView.maxLines = 1
            textView.setHorizontallyScrolling(true)
            textView.maxWidth = screenWidth / 2
            textView.maxHeight = Int.MAX_VALUE
            textView.setPadding(dpInt(12f), dpInt(8f), dpInt(12f), dpInt(8f))
        }
        rootContainer.layoutParams = rootLp
        rootContainer.elevation = if (editing) dp(4f) else dp(3f)

        // Always re-apply styles to drawables to ensure they're up to date
        applyTheme(null) 

        updateEditingState(composing, displayText, editing, cursorIndex)
        
        textView.post {
            if (editing) vScrollView.fullScroll(View.FOCUS_DOWN) 
            else hScrollView.fullScroll(View.FOCUS_RIGHT)
        }
    }

    fun updateAbove(
        anchor: View,
        composing: String,
        displayText: String = composing,
        xMarginPx: Int,
        yMarginPx: Int,
        editing: Boolean = false,
        cursorIndex: Int = 0,
    ) {
        update(composing, displayText, editing, cursorIndex)
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
                    val index = resolveCursorIndex(ev.x, ev.y)
                    onCursorMove?.invoke(index)
                    true
                }
                else -> false
            }
        }
    }

    private fun resolveCursorIndex(x: Float, y: Float): Int {
        val text = displayText
        if (text.isBlank()) return 0
        val layout = textView.layout ?: return displayIndexToRaw(text, 0)
        
        val line = layout.getLineForVertical((y - textView.paddingTop).toInt().coerceAtLeast(0))
        val offsetInLine = layout.getOffsetForHorizontal(line, (x - textView.paddingLeft))
        
        return displayIndexToRaw(text, offsetInLine)
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
