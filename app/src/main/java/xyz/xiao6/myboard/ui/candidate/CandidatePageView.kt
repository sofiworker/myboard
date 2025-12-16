package xyz.xiao6.myboard.ui.candidate

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.ThemeRuntime

/**
 * Expanded candidate page overlay (does not change IME window height):
 * - Left: vertical scroll pinyin segments
 * - Center: grid candidates
 * - Right: actions (back/delete/retype placeholders)
 */
class CandidatePageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val leftList: RecyclerView
    private val centerGrid: RecyclerView
    private val rightActions: LinearLayout

    private val pinyinAdapter = PinyinAdapter { index -> onPinyinSelected?.invoke(index) }
    private val candidateAdapter = CandidateGridAdapter { text -> onCandidateClick?.invoke(text) }

    var onCandidateClick: ((String) -> Unit)? = null
    var onCandidateLongPress: ((anchor: View, text: String) -> Unit)? = null
    var onCandidatePreviewDismiss: (() -> Unit)? = null
    var onPinyinSelected: ((Int) -> Unit)? = null
    var onBack: (() -> Unit)? = null
    var onBackspace: (() -> Unit)? = null
    var onRetype: (() -> Unit)? = null

    private var selectedPinyinIndex: Int = 0

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#F2F2F7"))
        }

        val root = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.HORIZONTAL
        }

        leftList = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72f).toInt(), LayoutParams.MATCH_PARENT)
            overScrollMode = OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = pinyinAdapter
            setBackgroundColor(Color.parseColor("#FAFAFA"))
            addItemDecoration(SimpleDividerDecoration(Color.parseColor("#14000000"), dp(1f)))
        }

        centerGrid = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            overScrollMode = OVER_SCROLL_NEVER
            layoutManager = GridLayoutManager(context, 4)
            adapter = candidateAdapter.apply {
                onLongPress = { anchor, text -> onCandidateLongPress?.invoke(anchor, text) }
                onPreviewDismiss = { onCandidatePreviewDismiss?.invoke() }
            }
            setBackgroundColor(Color.WHITE)
            addItemDecoration(GridDividerDecoration(Color.parseColor("#14000000"), dp(1f)))
        }

        rightActions = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(84f).toInt(), LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FAFAFA"))
            addView(actionButton("返回") { onBack?.invoke() })
            addView(actionSpacer())
            addView(actionButton("⌫") { onBackspace?.invoke() })
            addView(actionSpacer())
            addView(actionButton("重输") { onRetype?.invoke() })
        }

        root.addView(leftList)
        root.addView(dividerVertical())
        root.addView(centerGrid)
        root.addView(dividerVertical())
        root.addView(rightActions)
        addView(root)
    }

    fun submitPinyinSegments(segments: List<String>, selectedIndex: Int = 0) {
        selectedPinyinIndex = selectedIndex.coerceIn(0, (segments.size - 1).coerceAtLeast(0))
        pinyinAdapter.submitList(segments)
        pinyinAdapter.setSelectedIndex(selectedPinyinIndex)
        if (selectedPinyinIndex >= 0) {
            leftList.scrollToPosition(selectedPinyinIndex)
        }
    }

    fun submitCandidates(candidates: List<String>) {
        candidateAdapter.submitList(candidates)
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        val bg = runtime?.resolveColor(theme?.colors?.get("background"), Color.parseColor("#F2F2F7")) ?: Color.parseColor("#F2F2F7")
        (background as? GradientDrawable)?.setColor(bg)
    }

    private fun actionButton(text: String, onClick: () -> Unit): View {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(56f).toInt())
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.parseColor("#3C3C43"))
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun actionSpacer(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1f).toInt())
        setBackgroundColor(Color.parseColor("#14000000"))
    }

    private fun dividerVertical(): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(1f).toInt(), LayoutParams.MATCH_PARENT)
        setBackgroundColor(Color.parseColor("#14000000"))
    }

    private class PinyinAdapter(
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.Adapter<PinyinVH>() {
        private var items: List<String> = emptyList()
        private var selectedIndex: Int = 0

        fun submitList(list: List<String>) {
            items = list
            notifyDataSetChanged()
        }

        fun setSelectedIndex(index: Int) {
            selectedIndex = index
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinyinVH {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(parent.context, 56f).toInt())
                gravity = Gravity.CENTER
                textSize = 22f
            }
            return PinyinVH(tv, onClick)
        }

        override fun onBindViewHolder(holder: PinyinVH, position: Int) {
            val selected = position == selectedIndex
            holder.bind(items[position], position, selected)
        }

        override fun getItemCount(): Int = items.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class PinyinVH(
        private val tv: TextView,
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(tv) {
        fun bind(text: String, index: Int, selected: Boolean) {
            tv.text = text
            tv.setTextColor(if (selected) Color.parseColor("#007AFF") else Color.parseColor("#3C3C43"))
            tv.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            tv.setOnClickListener { onClick(index) }
        }
    }

    private class CandidateGridAdapter(
        private val onClick: (String) -> Unit,
    ) : ListAdapter<String, CandidateVH>(DIFF) {
        var onLongPress: ((anchor: View, text: String) -> Unit)? = null
        var onPreviewDismiss: (() -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateVH {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(parent.context, 64f).toInt())
                gravity = Gravity.CENTER
                textSize = 26f
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, 0, 0, 0)
            }
            return CandidateVH(tv, onClick)
        }

        override fun onBindViewHolder(holder: CandidateVH, position: Int) {
            holder.bind(getItem(position), onLongPress, onPreviewDismiss)
        }

        companion object {
            private val DIFF =
                object : DiffUtil.ItemCallback<String>() {
                    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
                    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
                }
        }

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class CandidateVH(
        private val tv: TextView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(tv) {
        fun bind(text: String, onLongPress: ((View, String) -> Unit)?, onPreviewDismiss: (() -> Unit)?) {
            tv.text = text
            tv.setOnClickListener { onClick(text) }

            var previewShown = false
            tv.setOnTouchListener { _, ev ->
                if (!previewShown) return@setOnTouchListener false
                when (ev.actionMasked) {
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL,
                    MotionEvent.ACTION_OUTSIDE,
                    -> {
                        previewShown = false
                        onPreviewDismiss?.invoke()
                        true // consume so long-press doesn't also trigger click on release
                    }

                    else -> false
                }
            }
            tv.setOnLongClickListener {
                val available = (tv.width - tv.paddingLeft - tv.paddingRight).toFloat().coerceAtLeast(0f)
                val needed = tv.paint.measureText(text)
                if (needed > available + 1f) {
                    onLongPress?.invoke(tv, text)
                    previewShown = true
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private class SimpleDividerDecoration(
        dividerColor: Int,
        dividerHeightPx: Float,
    ) : RecyclerView.ItemDecoration() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = dividerColor
        }
        private val h = dividerHeightPx

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val left = parent.paddingLeft.toFloat()
            val right = (parent.width - parent.paddingRight).toFloat()
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as? RecyclerView.LayoutParams ?: continue
                val bottom = child.bottom + params.bottomMargin
                c.drawRect(left, bottom.toFloat(), right, bottom + h, paint)
            }
        }
    }

    private class GridDividerDecoration(
        dividerColor: Int,
        dividerWidthPx: Float,
    ) : RecyclerView.ItemDecoration() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = dividerColor
        }
        private val w = dividerWidthPx

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val lm = parent.layoutManager as? GridLayoutManager ?: return
            val spanCount = lm.spanCount.coerceAtLeast(1)
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as? RecyclerView.LayoutParams ?: continue
                val position = parent.getChildAdapterPosition(child)
                if (position == RecyclerView.NO_POSITION) continue
                val column = position % spanCount

                // right divider (except last column)
                if (column != spanCount - 1) {
                    val x = (child.right + params.rightMargin).toFloat()
                    c.drawRect(x, child.top.toFloat(), x + w, child.bottom.toFloat(), paint)
                }
                // bottom divider
                val y = (child.bottom + params.bottomMargin).toFloat()
                c.drawRect(child.left.toFloat(), y, child.right.toFloat(), y + w, paint)
            }
        }
    }
}
