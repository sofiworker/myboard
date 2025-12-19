package xyz.xiao6.myboard.ui.layout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.ThemeRuntime
import kotlin.math.max

class LayoutPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    data class LayoutOption(
        val localeTag: String,
        val layoutId: String,
        val name: String,
        val selected: Boolean = false,
        val layout: KeyboardLayout? = null,
    )

    data class LocaleSection(
        val localeTag: String,
        val label: String,
        val options: List<LayoutOption>,
    )

    private val recyclerView: RecyclerView
    private val adapter = LayoutAdapter { option -> onLayoutSelected?.invoke(option.localeTag, option.layoutId) }
    private val gridLayoutManager: GridLayoutManager

    var onLayoutSelected: ((String, String) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    init {
        isClickable = true
        setBackgroundColor(Color.parseColor("#F2F2F7"))

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        val header = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(48f).toInt())
            setPadding(dp(12f).toInt(), 0, dp(12f).toInt(), 0)
        }

        val title = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#3C3C43"))
            text = "Languages & Layouts"
        }

        val close = ImageButton(context).apply {
            layoutParams = FrameLayout.LayoutParams(dp(40f).toInt(), dp(40f).toInt()).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            contentDescription = "Close"
            setOnClickListener { onDismiss?.invoke() }
        }

        header.addView(title)
        header.addView(close)

        recyclerView = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            overScrollMode = OVER_SCROLL_NEVER
            setPadding(dp(10f).toInt(), dp(10f).toInt(), dp(10f).toInt(), dp(10f).toInt())
            clipToPadding = false
            gridLayoutManager = GridLayoutManager(context, 2)
            gridLayoutManager.spanSizeLookup =
                object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (this@LayoutPickerView.adapter.getItemViewType(position)) {
                            LayoutAdapter.VT_HEADER -> gridLayoutManager.spanCount
                            else -> 1
                        }
                    }
                }
            layoutManager = gridLayoutManager
            adapter = this@LayoutPickerView.adapter
        }

        root.addView(header)
        root.addView(recyclerView)
        addView(root)
    }

    fun submitSections(sections: List<LocaleSection>) {
        val items = buildList {
            for (section in sections) {
                add(RowItem.Header(section.localeTag, section.label))
                for (opt in section.options) add(RowItem.Option(opt))
            }
        }
        adapter.submitList(items)
        val optionCount = sections.sumOf { it.options.size }
        updateGrid(optionCount)
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        val bg =
            runtime?.resolveColor(theme?.colors?.get("background"), Color.parseColor("#F2F2F7"))
                ?: Color.parseColor("#F2F2F7")
        val accent = runtime?.resolveColor("colors.accent", Color.parseColor("#007AFF")) ?: Color.parseColor("#007AFF")
        val onSurface = runtime?.resolveColor("colors.key_text", Color.parseColor("#3C3C43")) ?: Color.parseColor("#3C3C43")
        val keyBg = runtime?.resolveColor("colors.key_bg", Color.WHITE) ?: Color.WHITE
        val stroke = runtime?.resolveColor("colors.stroke", Color.parseColor("#14000000")) ?: Color.parseColor("#14000000")
        val keyText = runtime?.resolveColor("colors.key_text", Color.parseColor("#3C3C43")) ?: Color.parseColor("#3C3C43")

        setBackgroundColor(bg)
        adapter.accentColor = accent
        adapter.textColor = onSurface
        adapter.keyColor = keyBg
        adapter.keyStrokeColor = stroke
        adapter.keyTextColor = keyText
        adapter.notifyDataSetChanged()
    }

    private fun updateGrid(optionCount: Int) {
        val span = when {
            optionCount <= 1 -> 1
            optionCount == 2 -> 2
            else -> 2
        }
        gridLayoutManager.spanCount = span
    }

    private class LayoutAdapter(
        private val onClick: (LayoutOption) -> Unit,
    ) : ListAdapter<RowItem, RecyclerView.ViewHolder>(DIFF) {
        var accentColor: Int = Color.parseColor("#007AFF")
        var textColor: Int = Color.parseColor("#3C3C43")
        var keyColor: Int = Color.WHITE
        var keyStrokeColor: Int = Color.parseColor("#14000000")
        var keyTextColor: Int = Color.parseColor("#3C3C43")

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is RowItem.Header -> VT_HEADER
                is RowItem.Option -> VT_OPTION
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                VT_HEADER -> {
                    val tv = TextView(parent.context).apply {
                        layoutParams =
                            RecyclerView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                            ).apply {
                                val m = dp(parent.context, 8f).toInt()
                                setMargins(m, m, m, dp(parent.context, 2f).toInt())
                            }
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                        setPadding(dp(parent.context, 6f).toInt(), dp(parent.context, 8f).toInt(), dp(parent.context, 6f).toInt(), dp(parent.context, 4f).toInt())
                    }
                    HeaderVH(tv)
                }
                else -> createOptionVH(parent)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is HeaderVH -> holder.bind((getItem(position) as RowItem.Header).label, textColor)
                is LayoutVH -> {
                    val option = (getItem(position) as RowItem.Option).option
                    holder.bind(option, textColor, accentColor)
                    holder.setPreviewColors(keyColor, keyStrokeColor, keyTextColor)
                }
            }
        }

        private fun createOptionVH(parent: ViewGroup): LayoutVH {
            fun dp(value: Float): Float = value * parent.context.resources.displayMetrics.density

            val root =
                LinearLayout(parent.context).apply {
                    layoutParams =
                        RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                            val m = dp(8f).toInt()
                            setMargins(m, m, m, m)
                        }
                    orientation = LinearLayout.VERTICAL
                    background =
                        GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = dp(12f)
                            setColor(Color.WHITE)
                            setStroke(dp(1f).toInt(), Color.parseColor("#14000000"))
                        }
                    clipToPadding = false
                    setPadding(dp(10f).toInt(), dp(10f).toInt(), dp(10f).toInt(), dp(10f).toInt())
                }
            val thumb = LayoutThumbnailView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100f).toInt())
            }
            val title = TextView(parent.context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
                textSize = 14f
                setPadding(0, dp(8f).toInt(), 0, 0)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            root.addView(thumb)
            root.addView(title)
            return LayoutVH(root, thumb, title, onClick)
        }

        companion object {
            const val VT_HEADER = 1
            const val VT_OPTION = 2

            private val DIFF =
                object : DiffUtil.ItemCallback<RowItem>() {
                    override fun areItemsTheSame(oldItem: RowItem, newItem: RowItem): Boolean {
                        return when {
                            oldItem is RowItem.Header && newItem is RowItem.Header ->
                                oldItem.localeTag == newItem.localeTag
                            oldItem is RowItem.Option && newItem is RowItem.Option ->
                                oldItem.option.localeTag == newItem.option.localeTag && oldItem.option.layoutId == newItem.option.layoutId
                            else -> false
                        }
                    }

                    override fun areContentsTheSame(oldItem: RowItem, newItem: RowItem): Boolean = oldItem == newItem
                }

            private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
        }
    }

    private class HeaderVH(private val title: TextView) : RecyclerView.ViewHolder(title) {
        fun bind(label: String, textColor: Int) {
            title.text = label
            title.setTextColor(textColor)
        }
    }

    private class LayoutVH(
        private val root: View,
        private val thumbnail: LayoutThumbnailView,
        private val title: TextView,
        private val onClick: (LayoutOption) -> Unit,
    ) : RecyclerView.ViewHolder(root) {
        private var keyColor: Int = Color.WHITE
        private var keyStroke: Int = Color.parseColor("#14000000")
        private var keyText: Int = Color.parseColor("#3C3C43")

        fun setPreviewColors(keyColor: Int, keyStrokeColor: Int, keyTextColor: Int) {
            this.keyColor = keyColor
            this.keyStroke = keyStrokeColor
            this.keyText = keyTextColor
        }

        fun bind(option: LayoutOption, textColor: Int, accentColor: Int) {
            title.text = option.name
            title.setTextColor(textColor)
            title.typeface = if (option.selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            thumbnail.setLayout(option.layout, keyColor, keyStroke, keyText)

            val bg = root.background as? GradientDrawable
            bg?.setStroke(
                (root.resources.displayMetrics.density * 1f).toInt().coerceAtLeast(1),
                if (option.selected) accentColor else Color.parseColor("#14000000"),
            )
            root.setOnClickListener { onClick(option) }
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private class LayoutThumbnailView(context: Context) : View(context) {
        private var layout: KeyboardLayout? = null
        private var keyColor: Int = Color.WHITE
        private var keyStrokeColor: Int = Color.parseColor("#14000000")
        private var keyTextColor: Int = Color.parseColor("#3C3C43")

        private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

        fun setLayout(layout: KeyboardLayout?, keyColor: Int, keyStrokeColor: Int, keyTextColor: Int) {
            this.layout = layout
            this.keyColor = keyColor
            this.keyStrokeColor = keyStrokeColor
            this.keyTextColor = keyTextColor
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val layout = layout ?: return

            val pad = dp(6f)
            val left = pad
            val top = pad
            val right = width.toFloat() - pad
            val bottom = height.toFloat() - pad
            val w = (right - left).coerceAtLeast(1f)
            val h = (bottom - top).coerceAtLeast(1f)

            val rows = layout.rows.take(4)
            if (rows.isEmpty()) return

                val colCounts =
                    rows.map { row ->
                    row.keys.maxOfOrNull { it.ui.gridPosition.startCol + it.ui.gridPosition.spanCols } ?: row.keys.size
                }.map { it.coerceAtLeast(1) }
            val maxCols = colCounts.maxOrNull()?.coerceAtLeast(1) ?: 1

            val rowGap = dp(4f)
            val rowH = ((h - rowGap * (rows.size - 1)).coerceAtLeast(1f) / rows.size)

            val radius = dp(6f)
            keyPaint.color = keyColor
            strokePaint.color = keyStrokeColor
            strokePaint.strokeWidth = dp(1f).coerceAtLeast(1f)
            textPaint.color = keyTextColor
            textPaint.textSize = max(dp(10f), h * 0.10f)

            for ((rowIndex, row) in rows.withIndex()) {
                val yTop = top + rowIndex * (rowH + rowGap)
                val yBottom = yTop + rowH
                val colW = w / maxCols

                val keys = row.keys.sortedBy { it.ui.gridPosition.startCol }
                for (k in keys) {
                    val start = k.ui.gridPosition.startCol
                    val span = k.ui.gridPosition.spanCols.coerceAtLeast(1)
                    val xLeft = left + start * colW
                    val xRight = left + (start + span) * colW - dp(2f)
                    val rx = (xRight - xLeft).coerceAtLeast(dp(6f))
                    val rectLeft = xLeft
                    val rectRight = rectLeft + rx

                    canvas.drawRoundRect(rectLeft, yTop, rectRight, yBottom, radius, radius, keyPaint)
                    canvas.drawRoundRect(rectLeft, yTop, rectRight, yBottom, radius, radius, strokePaint)

                    val label = (k.ui.label ?: k.label).orEmpty().take(1)
                    val cy = (yTop + yBottom) / 2f - (textPaint.ascent() + textPaint.descent()) / 2f
                    canvas.drawText(label, (rectLeft + rectRight) / 2f, cy, textPaint)
                }
            }
        }

        private fun dp(value: Float): Float = value * resources.displayMetrics.density
    }

    private sealed interface RowItem {
        data class Header(val localeTag: String, val label: String) : RowItem
        data class Option(val option: LayoutOption) : RowItem
    }
}
