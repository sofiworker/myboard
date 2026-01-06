package xyz.xiao6.myboard.ui.candidate

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime

/**
 * 候选词/预测栏骨架（RecyclerView 横向滚动）。
 * Candidate bar skeleton (horizontal RecyclerView).
 */
class CandidateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var textColor: Int = Color.BLACK
    private var surfaceBackground: Int = Color.parseColor("#F2F2F7")
    private var surfaceStroke: Int = Color.parseColor("#14000000")
    private var embeddedInToolbar: Boolean = false
    private var fontSizeSp: Float = 16f
    private var fontWeight: Int = 400

    private val surfaceDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10f)
            setColor(surfaceBackground)
            setStroke(dp(1f).toInt(), surfaceStroke)
        }

    private val recyclerView: RecyclerView
    private val adapter: CandidateAdapter
    private val settingsStore: SettingsStore

    var onCandidateClick: ((String) -> Unit)? = null
    var onCandidateLongPress: ((anchor: View, text: String) -> Unit)? = null

    init {
        settingsStore = SettingsStore(context)
        background = surfaceDrawable
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
            overScrollMode = OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
            setHasFixedSize(true)
        }
        adapter = CandidateAdapter(
            resolveTextColor = { textColor },
            resolveFontSize = { fontSizeSp },
            resolveFontWeight = { fontWeight },
            onClick = { text -> onCandidateClick?.invoke(text) },
            onLongClick = { anchor, text -> onCandidateLongPress?.invoke(anchor, text) },
        )
        recyclerView.adapter = adapter

        addView(recyclerView)
    }

    fun setEmbeddedInToolbar(embedded: Boolean) {
        embeddedInToolbar = embedded
        background = if (embedded) null else surfaceDrawable
        invalidate()
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        surfaceBackground = runtime?.resolveColor(theme?.candidates?.surface?.background?.color, surfaceBackground) ?: surfaceBackground
        surfaceStroke = runtime?.resolveColor(theme?.candidates?.surface?.stroke?.color, surfaceStroke) ?: surfaceStroke
        textColor = runtime?.resolveColor(theme?.candidates?.candidateText?.color, textColor) ?: textColor
        fontSizeSp = settingsStore.candidateFontSizeSp
        fontWeight = settingsStore.candidateFontWeight
        surfaceDrawable.apply {
            setColor(surfaceBackground)
            setStroke(dp(1f).toInt(), surfaceStroke)
        }
        background = if (embeddedInToolbar) null else surfaceDrawable
        adapter.notifyDataSetChanged()
    }

    fun submitCandidates(candidates: List<String>) {
        adapter.submitList(candidates.toList())
    }

    private class CandidateAdapter(
        private val resolveTextColor: () -> Int,
        private val resolveFontSize: () -> Float,
        private val resolveFontWeight: () -> Int,
        private val onClick: (String) -> Unit,
        private val onLongClick: (View, String) -> Unit,
    ) : ListAdapter<String, CandidateViewHolder>(DIFF) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
            val textView = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                minWidth = (parent.context.resources.displayMetrics.density * 48f).toInt()
                gravity = Gravity.CENTER
                setPaddingRelative(
                    (parent.context.resources.displayMetrics.density * 12f).toInt(),
                    0,
                    (parent.context.resources.displayMetrics.density * 12f).toInt(),
                    0,
                )
                applyAppFont()
            }
            return CandidateViewHolder(textView, onClick, onLongClick)
        }

        override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
            holder.bind(
                getItem(position),
                resolveTextColor(),
                resolveFontSize(),
                resolveFontWeight()
            )
        }

        companion object {
            private val DIFF =
                object : DiffUtil.ItemCallback<String>() {
                    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
                    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
                }
        }
    }

    private class CandidateViewHolder(
        private val textView: TextView,
        private val onClick: (String) -> Unit,
        private val onLongClick: (View, String) -> Unit,
    ) : RecyclerView.ViewHolder(textView) {

        fun bind(text: String, textColor: Int, fontSizeSp: Float, fontWeight: Int) {
            textView.text = text
            textView.setTextColor(textColor)
            textView.textSize = fontSizeSp
            textView.setTypeface(null, when {
                fontWeight >= 700 -> Typeface.BOLD
                fontWeight >= 500 -> Typeface.BOLD
                else -> Typeface.NORMAL
            })
            textView.setOnClickListener { onClick(text) }
            textView.setOnLongClickListener {
                onLongClick(textView, text)
                true
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
