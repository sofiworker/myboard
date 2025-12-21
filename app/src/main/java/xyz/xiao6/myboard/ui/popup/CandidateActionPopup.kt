package xyz.xiao6.myboard.ui.popup

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import xyz.xiao6.myboard.R

class CandidateActionPopup(
    private val context: Context,
) {
    private val displayMetrics = context.resources.displayMetrics

    var onBlock: ((String) -> Unit)? = null
    var onDemote: ((String) -> Unit)? = null

    private var currentText: String = ""

    private val content: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10f)
            setColor(Color.WHITE)
            setStroke(dpInt(1f), Color.parseColor("#22000000"))
        }
        setPadding(dpInt(10f), dpInt(8f), dpInt(10f), dpInt(8f))
    }

    private val blockButton = actionButton(R.string.suggestions_action_block) {
        val text = currentText
        if (text.isNotBlank()) onBlock?.invoke(text)
        dismiss()
    }

    private val demoteButton = actionButton(R.string.suggestions_action_demote) {
        val text = currentText
        if (text.isNotBlank()) onDemote?.invoke(text)
        dismiss()
    }

    private val popup =
        PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            isOutsideTouchable = true
            isClippingEnabled = false
            elevation = dp(4f)
        }

    init {
        content.addView(blockButton)
        content.addView(divider())
        content.addView(demoteButton)
    }

    fun dismiss() {
        popup.dismiss()
    }

    fun showAbove(anchor: View, text: String, marginPx: Int) {
        val clean = text.trim()
        if (clean.isBlank()) return
        currentText = clean
        measureContent()
        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val x = loc[0]
        val y = loc[1] - popup.height - marginPx
        if (!popup.isShowing) {
            popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        } else {
            popup.update(x, y, popup.width, popup.height)
        }
    }

    private fun actionButton(labelRes: Int, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dpInt(36f))
            text = context.getString(labelRes)
            setTextColor(Color.parseColor("#1C1C1E"))
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpInt(8f), 0, dpInt(8f), 0)
            setOnClickListener { onClick() }
        }
    }

    private fun divider(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpInt(1f))
            setBackgroundColor(Color.parseColor("#22000000"))
        }
    }

    private fun measureContent() {
        content.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        popup.width = content.measuredWidth
        popup.height = content.measuredHeight
    }

    private fun dp(value: Float): Float = value * displayMetrics.density
    private fun dpInt(value: Float): Int = (value * displayMetrics.density).toInt()
}
