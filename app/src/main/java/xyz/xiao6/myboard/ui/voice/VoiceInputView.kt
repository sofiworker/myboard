package xyz.xiao6.myboard.ui.voice

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.ui.theme.applyAppFont
import kotlin.math.sin

/**
 * Modern voice input overlay view.
 * Features a pulsing microphone icon and dynamic text feedback.
 */
class VoiceInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val micIcon: ImageView
    private val hintText: TextView
    private val rippleView: View
    private val container: LinearLayout
    
    private var isListening = false
    private val animationRunnable = object : Runnable {
        var startTime = 0L
        override fun run() {
            if (!isListening) return
            if (startTime == 0L) startTime = System.currentTimeMillis()
            val elapsed = System.currentTimeMillis() - startTime
            val scale = 1.0f + 0.1f * sin(elapsed / 150.0).toFloat()
            micIcon.scaleX = scale
            micIcon.scaleY = scale
            rippleView.scaleX = scale * 1.4f
            rippleView.scaleY = scale * 1.4f
            rippleView.alpha = 0.3f * (1.0f - (scale - 1.0f) * 5f).coerceIn(0f, 1f)
            postDelayed(this, 16)
        }
    }

    init {
        setBackgroundColor(Color.parseColor("#E6FFFFFF")) // Semi-transparent overlay
        isClickable = true
        isFocusable = true

        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }

        val iconSize = dp(80f).toInt()
        val rippleSize = dp(120f).toInt()

        val iconContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(rippleSize, rippleSize).apply {
                bottomMargin = dp(24f).toInt()
            }
        }

        rippleView = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3B82F6"))
            }
            alpha = 0f
        }

        micIcon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            setImageResource(R.drawable.mic_line)
            imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3B82F6"))
            }
            setPadding(dp(20f).toInt(), dp(20f).toInt(), dp(20f).toInt(), dp(20f).toInt())
            elevation = dp(8f)
        }

        iconContainer.addView(rippleView)
        iconContainer.addView(micIcon)

        hintText = TextView(context).apply {
            text = "Listening..."
            textSize = 20f
            applyAppFont(bold = true)
            setTextColor(Color.parseColor("#3C4043"))
            gravity = Gravity.CENTER
        }

        container.addView(iconContainer)
        container.addView(hintText)
        addView(container)
    }

    fun startListening() {
        isListening = true
        visibility = VISIBLE
        hintText.text = "Listening..."
        hintText.setTextColor(Color.parseColor("#3C4043"))
        micIcon.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#3B82F6"))
        }
        animationRunnable.startTime = 0
        removeCallbacks(animationRunnable)
        post(animationRunnable)
    }

    fun stopListening() {
        isListening = false
        removeCallbacks(animationRunnable)
        micIcon.scaleX = 1f
        micIcon.scaleY = 1f
        rippleView.scaleX = 1f
        rippleView.scaleY = 1f
        rippleView.alpha = 0f
        visibility = GONE
    }

    fun updateStatus(text: String) {
        hintText.text = text
    }

    fun setError(error: String) {
        hintText.text = error
        hintText.setTextColor(Color.parseColor("#EF4444"))
        micIcon.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#EF4444"))
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
