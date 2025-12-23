package xyz.xiao6.myboard.ui.candidate

import android.content.Context
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
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
import xyz.xiao6.myboard.ui.theme.AppFont
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import kotlin.math.ceil
import kotlin.math.max

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

    private val gridSpanCount = 12
    private val candidateTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sp(26f)
            applyAppFont(context)
        }
    private val leftDividerDecoration = SimpleDividerDecoration(Color.parseColor("#14000000"), dp(1f))
    private val gridDividerDecoration = ExcelGridDecoration(Color.parseColor("#22000000"), dp(1f))

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
            addItemDecoration(leftDividerDecoration)
        }

        centerGrid = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            overScrollMode = OVER_SCROLL_NEVER
            val lm =
                GridLayoutManager(context, gridSpanCount).apply {
                    spanSizeLookup =
                        object : GridLayoutManager.SpanSizeLookup() {
                            override fun getSpanSize(position: Int): Int {
                                return candidateAdapter.currentList.getOrNull(position)?.spanSize?.coerceIn(1, gridSpanCount)
                                    ?: 1
                            }
                        }
                }
            layoutManager = lm
            adapter =
                candidateAdapter.apply {
                    onLongPress = { anchor, text -> onCandidateLongPress?.invoke(anchor, text) }
                    onPreviewDismiss = { onCandidatePreviewDismiss?.invoke() }
                }
            setBackgroundColor(Color.WHITE)
            itemAnimator = null
            addItemDecoration(gridDividerDecoration)
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
        // If called before layout, fallback to a reasonable width guess; GridLayout will relayout later anyway.
        val contentWidthPx = (centerGrid.width - centerGrid.paddingLeft - centerGrid.paddingRight).takeIf { it > 0 }
            ?: (resources.displayMetrics.widthPixels * 0.65f).toInt()
        val packed = packCandidates(candidates, availableWidthPx = contentWidthPx)
        candidateAdapter.submitList(packed)
        centerGrid.scrollToPosition(0)
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        val bg = runtime?.resolveColor(theme?.colors?.get("background"), Color.parseColor("#F2F2F7")) ?: Color.parseColor("#F2F2F7")
        (background as? GradientDrawable)?.setColor(bg)
        val divider = theme?.candidates?.divider
        val fallbackLeft = Color.parseColor("#14000000")
        val fallbackGrid = Color.parseColor("#22000000")
        val dividerColor = runtime?.resolveColor(divider?.color, fallbackGrid) ?: fallbackGrid
        val dividerWidth = dp(divider?.widthDp ?: 1f)
        leftDividerDecoration.updateStyle(
            color = runtime?.resolveColor(divider?.color, fallbackLeft) ?: fallbackLeft,
            heightPx = dividerWidth,
        )
        gridDividerDecoration.updateStyle(color = dividerColor, widthPx = dividerWidth)
        leftList.invalidateItemDecorations()
        centerGrid.invalidateItemDecorations()
    }

    private fun actionButton(text: String, onClick: () -> Unit): View {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(56f).toInt())
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.parseColor("#3C3C43"))
            this.text = text
            applyAppFont(bold = true)
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

    data class CandidateCell(
        val text: String,
        val spanSize: Int,
        val ellipsize: Boolean,
    )

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
                applyAppFont()
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
            tv.typeface = if (selected) AppFont.bold(tv.context) else AppFont.regular(tv.context)
            tv.setOnClickListener { onClick(index) }
        }
    }

    private class CandidateGridAdapter(
        private val onClick: (String) -> Unit,
    ) : ListAdapter<CandidateCell, CandidateVH>(DIFF) {
        var onLongPress: ((anchor: View, text: String) -> Unit)? = null
        var onPreviewDismiss: (() -> Unit)? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateVH {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(parent.context, 64f).toInt(),
                )
                gravity = Gravity.CENTER
                textSize = 26f
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
                maxLines = 1
                isSingleLine = true
                ellipsize = null
                val hp = dp(parent.context, 10f).toInt()
                setPadding(hp, 0, hp, 0)
                applyAppFont()
            }
            return CandidateVH(tv, onClick)
        }

        override fun onBindViewHolder(holder: CandidateVH, position: Int) {
            holder.bind(getItem(position), onLongPress, onPreviewDismiss)
        }

        companion object {
            private val DIFF =
                object : DiffUtil.ItemCallback<CandidateCell>() {
                    override fun areItemsTheSame(oldItem: CandidateCell, newItem: CandidateCell): Boolean = oldItem.text == newItem.text
                    override fun areContentsTheSame(oldItem: CandidateCell, newItem: CandidateCell): Boolean = oldItem == newItem
                }

            private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
        }
    }

    private class CandidateVH(
        private val tv: TextView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(tv) {
        fun bind(cell: CandidateCell, onLongPress: ((View, String) -> Unit)?, onPreviewDismiss: (() -> Unit)?) {
            tv.text = cell.text
            tv.ellipsize = if (cell.ellipsize) TextUtils.TruncateAt.END else null
            tv.setOnClickListener { onClick(cell.text) }

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
                        true
                    }

                    else -> false
                }
            }
            tv.setOnLongClickListener {
                val available = (tv.width - tv.paddingLeft - tv.paddingRight).toFloat().coerceAtLeast(0f)
                val needed = tv.paint.measureText(cell.text)
                if (needed > available + 1f) {
                    onLongPress?.invoke(tv, cell.text)
                    previewShown = true
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private fun packCandidates(candidates: List<String>, availableWidthPx: Int): List<CandidateCell> {
        val rowWidthPx = max(1, availableWidthPx)
        val colWidthPx = max(1, rowWidthPx / gridSpanCount)
        val cellPaddingPx = dp(14f)

        data class Item(var text: String, var span: Int, val textWidthPx: Float)

        fun baseSpanFor(textWidthPx: Float): Int {
            val desiredPx = textWidthPx + cellPaddingPx * 2
            return ceil(desiredPx / colWidthPx.toFloat()).toInt().coerceIn(1, gridSpanCount)
        }

        fun finalizeRow(row: MutableList<Item>) {
            if (row.isEmpty()) return
            val sum = row.sumOf { it.span }
            val remaining = (gridSpanCount - sum).coerceAtLeast(0)
            if (remaining == 0) return

            var rem = remaining
            // First, use remaining spans to satisfy items that would otherwise truncate.
            // neededSpan = ceil((textWidth + padding*2) / colWidth)
            while (rem > 0) {
                var bestIndex = -1
                var bestDeficit = 0
                for (i in row.indices) {
                    val needed = baseSpanFor(row[i].textWidthPx)
                    val deficit = (needed - row[i].span).coerceAtLeast(0)
                    if (deficit > bestDeficit) {
                        bestDeficit = deficit
                        bestIndex = i
                    }
                }
                if (bestIndex < 0 || bestDeficit <= 0) break
                row[bestIndex].span = (row[bestIndex].span + 1).coerceAtMost(gridSpanCount)
                rem -= 1
            }

            // Then, spread any leftover evenly to keep rows visually "filled".
            if (rem > 0) {
                val n = row.size
                val each = rem / n
                var extra = rem % n
                for (i in 0 until n) {
                    var add = each
                    if (extra > 0) {
                        add += 1
                        extra -= 1
                    }
                    row[i].span = (row[i].span + add).coerceAtMost(gridSpanCount)
                }
            }
        }

        val out = ArrayList<CandidateCell>(candidates.size)
        var row = mutableListOf<Item>()
        var rowSpan = 0

        fun flushRow() {
            finalizeRow(row)
            for (it in row) {
                val availableForItem = (it.span * colWidthPx).toFloat() - cellPaddingPx * 2
                val ellipsize = it.textWidthPx > availableForItem + 1f
                out += CandidateCell(text = it.text, spanSize = it.span, ellipsize = ellipsize)
            }
            row = mutableListOf()
            rowSpan = 0
        }

        for (raw in candidates) {
            val text = raw.trim()
            if (text.isEmpty()) continue
            val textW = candidateTextPaint.measureText(text)
            val baseSpan = baseSpanFor(textW)

            if (row.isNotEmpty() && rowSpan + baseSpan > gridSpanCount) {
                flushRow()
            }
            row += Item(text = text, span = baseSpan, textWidthPx = textW)
            rowSpan += baseSpan

            if (rowSpan == gridSpanCount) {
                flushRow()
            }
        }
        if (row.isNotEmpty()) flushRow()

        return out
    }

    private class SimpleDividerDecoration(
        dividerColor: Int,
        dividerHeightPx: Float,
    ) : RecyclerView.ItemDecoration() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = dividerColor
        }
        private var h = dividerHeightPx

        fun updateStyle(color: Int, heightPx: Float) {
            paint.color = color
            h = heightPx
        }

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

    private class ExcelGridDecoration(
        dividerColor: Int,
        dividerWidthPx: Float,
    ) : RecyclerView.ItemDecoration() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = dividerColor
        }
        private var w = dividerWidthPx

        fun updateStyle(color: Int, widthPx: Float) {
            paint.color = color
            w = widthPx
        }

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val lm = parent.layoutManager as? GridLayoutManager ?: return
            val spanCount = lm.spanCount.coerceAtLeast(1)
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as? RecyclerView.LayoutParams ?: continue
                val position = parent.getChildAdapterPosition(child)
                if (position == RecyclerView.NO_POSITION) continue

                val spanSize = lm.spanSizeLookup.getSpanSize(position).coerceAtLeast(1)
                val spanIndex = lm.spanSizeLookup.getSpanIndex(position, spanCount)
                val groupIndex = lm.spanSizeLookup.getSpanGroupIndex(position, spanCount)
                val endsRow = (spanIndex + spanSize) >= spanCount

                val left = (child.left - params.leftMargin).toFloat()
                val right = (child.right + params.rightMargin).toFloat()
                val top = (child.top - params.topMargin).toFloat()
                val bottom = (child.bottom + params.bottomMargin).toFloat()

                if (spanIndex == 0) {
                    c.drawRect(left, top, left + w, bottom, paint)
                }
                if (groupIndex == 0) {
                    c.drawRect(left, top, right, top + w, paint)
                }

                // Always draw right/bottom edges; this creates internal lines once (neighbor doesn't draw left/top).
                c.drawRect(right, top, right + w, bottom, paint)
                c.drawRect(left, bottom, right, bottom + w, paint)

                // Outer border for the row end: already covered by the right edge above.
                // Note: for merged cells (spanSize>1), internal lines are intentionally not drawn.
                if (endsRow) {
                    // no-op; kept for readability
                }
            }
        }
    }
}
