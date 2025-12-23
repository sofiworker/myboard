package xyz.xiao6.myboard.ui.theme

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import xyz.xiao6.myboard.R

object AppFont {
    @Volatile
    private var regular: Typeface? = null

    @Volatile
    private var bold: Typeface? = null

    @Volatile
    private var emoji: Typeface? = null

    @Volatile
    private var symbols: Typeface? = null

    @Volatile
    private var math: Typeface? = null

    fun regular(context: Context): Typeface {
        val cached = regular
        if (cached != null) return cached
        return synchronized(this) {
            regular ?: load(context, R.font.noto_sans_regular, Typeface.NORMAL).also { regular = it }
        }
    }

    fun bold(context: Context): Typeface {
        val cached = bold
        if (cached != null) return cached
        return synchronized(this) {
            bold ?: load(context, R.font.noto_sans_bold, Typeface.BOLD).also { bold = it }
        }
    }

    fun emoji(context: Context): Typeface {
        val cached = emoji
        if (cached != null) return cached
        return synchronized(this) {
            emoji ?: load(context, R.font.noto_color_emoji, Typeface.NORMAL).also { emoji = it }
        }
    }

    fun symbols(context: Context): Typeface {
        val cached = symbols
        if (cached != null) return cached
        return synchronized(this) {
            val preferred =
                ResourcesCompat.getFont(context, R.font.noto_sans_symbols2_regular)
                    ?: ResourcesCompat.getFont(context, R.font.noto_sans_symbols_regular)
            symbols = preferred ?: load(context, R.font.noto_sans_symbols_regular, Typeface.NORMAL)
            symbols!!
        }
    }

    fun math(context: Context): Typeface {
        val cached = math
        if (cached != null) return cached
        return synchronized(this) {
            math ?: load(context, R.font.noto_sans_math_regular, Typeface.NORMAL).also { math = it }
        }
    }

    private fun load(context: Context, resId: Int, style: Int): Typeface {
        return ResourcesCompat.getFont(context, resId)
            ?: Typeface.create(Typeface.SANS_SERIF, style)
    }
}

fun TextView.applyAppFont(bold: Boolean = false) {
    typeface = if (bold) AppFont.bold(context) else AppFont.regular(context)
}

fun Paint.applyAppFont(context: Context, bold: Boolean = false) {
    typeface = if (bold) AppFont.bold(context) else AppFont.regular(context)
}
