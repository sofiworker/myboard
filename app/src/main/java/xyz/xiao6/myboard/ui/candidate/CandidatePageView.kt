package xyz.xiao6.myboard.ui.candidate

import android.content.Context
import android.content.res.ColorStateList
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.AppFont
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import kotlin.math.ceil
import kotlin.math.max

/**
 * Expanded candidate page overlay:
 * - Left: vertical scroll pinyin segments
 * - Center: grid candidates (card style)
 * - Right: actions (back/delete/retype with modern icons)
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
    private val leftDividerDecoration = SimpleDividerDecoration(Color.TRANSPARENT, 0f)
    private val gridDividerDecoration = ExcelGridDecoration(Color.TRANSPARENT, 0f)

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#F1F3F4"))
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
            setBackgroundColor(Color.parseColor("#08000000"))
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
            setBackgroundColor(Color.TRANSPARENT)
            itemAnimator = null
            setPadding(dp(4f).toInt(), dp(4f).toInt(), dp(4f).toInt(), dp(4f).toInt())
            clipToPadding = false
            addItemDecoration(gridDividerDecoration)
        }

        rightActions = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64f).toInt(), LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8f).toInt(), dp(8f).toInt(), dp(8f).toInt(), dp(8f).toInt())
            setBackgroundColor(Color.parseColor("#08000000"))
            
            addView(actionIconButton(R.drawable.arrow_down_wide_line) { onBack?.invoke() })
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(1, 0, 1f) })
            addView(actionIconButton(R.drawable.delete_back_2_line) { onBackspace?.invoke() })
            addView(actionIconButton(R.drawable.ic_symbols_back) { onRetype?.invoke() })
        }

        root.addView(leftList)
        root.addView(centerGrid)
        root.addView(rightActions)
        addView(root)
    }

    private fun actionIconButton(@androidx.annotation.DrawableRes id: Int, onClick: () -> Unit): View {
        return ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48f).toInt(), dp(48f).toInt()).apply {
                topMargin = dp(4f).toInt()
                bottomMargin = dp(4f).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(12f)
                setColor(Color.parseColor("#DEE3EB"))
            }
            setImageResource(id)
            imageTintList = ColorStateList.valueOf(Color.parseColor("#3C4043"))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(12f).toInt(), dp(12f).toInt(), dp(12f).toInt(), dp(12f).toInt())
            setOnClickListener { onClick() }
        }
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
        val contentWidthPx = (centerGrid.width - centerGrid.paddingLeft - centerGrid.paddingRight).takeIf { it > 0 }
            ?: (resources.displayMetrics.widthPixels * 0.65f).toInt()
        val packed = packCandidates(candidates, availableWidthPx = contentWidthPx)
        candidateAdapter.submitList(packed)
        centerGrid.scrollToPosition(0)
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        val bg = runtime?.resolveColor(theme?.layout?.background?.color, Color.parseColor("#F1F3F4")) ?: Color.parseColor("#F1F3F4")
        (background as? GradientDrawable)?.setColor(bg)
        
        val fnBg = runtime?.resolveColor("colors.key_bg_function", Color.parseColor("#DEE3EB")) ?: Color.parseColor("#DEE3EB")
        val fgColor = runtime?.resolveColor("colors.key_text", Color.parseColor("#3C4043")) ?: Color.parseColor("#3C4043")
        val accentColor = runtime?.resolveColor("colors.accent", Color.parseColor("#1A73E8")) ?: Color.parseColor("#1A73E8")
        
        for (i in 0 until rightActions.childCount) {
            val child = rightActions.getChildAt(i)
            if (child is ImageButton) {
                (child.background as? GradientDrawable)?.setColor(fnBg)
                child.imageTintList = ColorStateList.valueOf(fgColor)
            }
        }
        
        val cellBg = runtime?.resolveColor("colors.key_bg", Color.WHITE) ?: Color.WHITE
        candidateAdapter.setColors(cellBg, fgColor, accentColor)
        
        gridDividerDecoration.updateStyle(Color.TRANSPARENT, 0f)
        leftDividerDecoration.updateStyle(Color.TRANSPARENT, 0f)
        leftList.invalidateItemDecorations()
        centerGrid.invalidateItemDecorations()
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
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(parent.context, 52f).toInt())
                gravity = Gravity.CENTER
                textSize = 18f
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
            val accent = Color.parseColor("#1A73E8")
            tv.setTextColor(if (selected) accent else Color.parseColor("#3C4043"))
            tv.typeface = if (selected) AppFont.bold(tv.context) else AppFont.regular(tv.context)
            tv.setOnClickListener { onClick(index) }
        }
    }

    private class CandidateGridAdapter(
        private val onClick: (String) -> Unit,
    ) : ListAdapter<CandidateCell, CandidateVH>(DIFF) {
        var onLongPress: ((anchor: View, text: String) -> Unit)? = null
        var onPreviewDismiss: (() -> Unit)? = null
        private var cellBgColor: Int = Color.WHITE
        private var textColor: Int = Color.BLACK
        private var accentColor: Int = Color.parseColor("#1A73E8")

        fun setColors(bg: Int, text: Int, accent: Int) {
            cellBgColor = bg
            textColor = text
            accentColor = accent
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateVH {
            val root = FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(parent.context, 56f).toInt(),
                ).apply {
                    val m = dp(parent.context, 2f).toInt()
                    setMargins(m, m, m, m)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(parent.context, 8f)
                    setColor(cellBgColor)
                }
            }
            val tv = TextView(parent.context).apply {
                layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                gravity = Gravity.CENTER
                textSize = 22f
                setTextColor(textColor)
                maxLines = 1
                isSingleLine = true
                ellipsize = null
                val hp = dp(parent.context, 8f).toInt()
                setPadding(hp, 0, hp, 0)
                applyAppFont()
            }
            root.addView(tv)
            return CandidateVH(root, tv, onClick)
        }

        override fun onBindViewHolder(holder: CandidateVH, position: Int) {
            (holder.itemView.background as? GradientDrawable)?.setColor(cellBgColor)
            holder.tv.setTextColor(if (position == 0) accentColor else textColor)
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
        private val root: View,
        val tv: TextView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(root) {
        fun bind(cell: CandidateCell, onLongPress: ((View, String) -> Unit)?, onPreviewDismiss: (() -> Unit)?) {
            tv.text = cell.text
            tv.ellipsize = if (cell.ellipsize) TextUtils.TruncateAt.END else null
            root.setOnClickListener { onClick(cell.text) }

            var previewShown = false
            root.setOnTouchListener { _, ev ->
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
            root.setOnLongClickListener {
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
        }
    }
}