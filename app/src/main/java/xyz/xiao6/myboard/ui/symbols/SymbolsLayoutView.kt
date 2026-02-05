package xyz.xiao6.myboard.ui.symbols

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.AppFont
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime

/**
 * 符号布局：覆盖 toolbar+keyboard 的整块区域。
 *
 * UI:
 * - 上半部分：左侧可滑动的符号网格（ViewPager 风格分页），右侧控制栏（返回 / 上一页 / 下一页 / 锁定）
 * - 底部：分类列表（常用/中文/英文/数学/网络/角标/拼音...）
 */
class SymbolsLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBack: (() -> Unit)? = null
    var onCommitSymbol: ((String) -> Unit)? = null
    var onLockChanged: ((Boolean) -> Unit)? = null

    private val categories: List<SymbolCategory> = AssetSymbolCatalogProvider(context).load()
    private var selectedCategoryIndex: Int = 0
    private var locked: Boolean = false

    private val symbolGrid: RecyclerView
    private val symbolAdapter: SymbolsGridAdapter
    private val symbolLayoutManager: GridLayoutManager
    private val symbolGridDecoration: SymbolsGridDecoration
    private val categoryList: RecyclerView
    private val categoryAdapter: CategoryAdapter

    private val btnBack: ImageButton
    private val btnPrev: ImageButton
    private val btnNext: ImageButton
    private val btnLock: ImageButton

    private var iconTint: ColorStateList = ColorStateList.valueOf(Color.BLACK)
    private var symbolTextTint: ColorStateList = ColorStateList.valueOf(Color.parseColor("#3C4043"))
    private var symbolGridDividerColor: Int = Color.TRANSPARENT
    private var symbolGridDividerWidthPx: Float = 0f

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#F1F3F4"))
        }

        val root = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
        }

        val top = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
            orientation = LinearLayout.HORIZONTAL
        }

        symbolLayoutManager = GridLayoutManager(context, 8)
        symbolGridDecoration = SymbolsGridDecoration(Color.TRANSPARENT, 0f)
        symbolGrid = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutManager = symbolLayoutManager
            itemAnimator = null
            setPadding(dp(4f).toInt(), dp(4f).toInt(), dp(4f).toInt(), dp(4f).toInt())
            clipToPadding = false
        }

        symbolAdapter = SymbolsGridAdapter(
            onClick = { symbol -> onCommitSymbol?.invoke(symbol) },
        )
        symbolGrid.adapter = symbolAdapter
        symbolGrid.addItemDecoration(symbolGridDecoration)
        symbolAdapter.attachDecoration(symbolGridDecoration)

        val rightBar = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(64f).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(8f).toInt(), dp(8f).toInt(), dp(8f).toInt(), dp(8f).toInt())
            setBackgroundColor(Color.parseColor("#08000000")) // Subtle sidebar separation
        }

        fun controlButton(@androidx.annotation.DrawableRes iconResId: Int, desc: String, rotate: Float = 0f): ImageButton {
            return ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48f).toInt(), 0, 1f).apply {
                    topMargin = dp(4f).toInt()
                    bottomMargin = dp(4f).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(12f)
                    setColor(Color.parseColor("#DEE3EB"))
                }
                setImageResource(iconResId)
                rotation = rotate
                contentDescription = desc
                imageTintList = ColorStateList.valueOf(Color.parseColor("#3C4043"))
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(12f).toInt(), dp(12f).toInt(), dp(12f).toInt(), dp(12f).toInt())
            }
        }

        btnBack = controlButton(R.drawable.delete_back_2_line, "Back")
        btnPrev = controlButton(R.drawable.arrow_down_s_line, "Previous page (up)", rotate = 180f)
        btnNext = controlButton(R.drawable.arrow_down_s_line, "Next page (down)")
        btnLock = controlButton(R.drawable.ic_symbols_unlock, "Lock")

        btnBack.setOnClickListener { onBack?.invoke() }
        btnPrev.setOnClickListener { pageScrollSymbols(up = true) }
        btnNext.setOnClickListener { pageScrollSymbols(up = false) }
        btnLock.setOnClickListener { setLocked(!locked, notify = true) }

        rightBar.addView(btnBack)
        rightBar.addView(btnPrev)
        rightBar.addView(btnNext)
        rightBar.addView(btnLock)

        top.addView(symbolGrid)
        top.addView(rightBar)

        categoryList = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52f).toInt(),
            )
            overScrollMode = OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setPadding(dp(8f).toInt(), 0, dp(8f).toInt(), 0)
            clipToPadding = false
        }

        categoryAdapter = CategoryAdapter(
            onClick = { idx -> selectCategory(idx) },
        )
        categoryList.adapter = categoryAdapter

        root.addView(top)
        root.addView(categoryList)
        addView(root)

        categoryAdapter.submit(categories.map { it.name })
        selectCategory(0)
        updatePageButtons()

        symbolGrid.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updatePageButtons()
                }
            },
        )
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        val bg = runtime?.resolveColor(theme?.layout?.background?.color, Color.parseColor("#F1F3F4"))
            ?: Color.parseColor("#F1F3F4")
        (background as? GradientDrawable)?.setColor(bg)

        val fnBg = runtime?.resolveColor("colors.key_bg_function", Color.parseColor("#DEE3EB"))
            ?: Color.parseColor("#DEE3EB")
        val fgColor = runtime?.resolveColor("colors.key_text", Color.parseColor("#3C4043"))
            ?: Color.parseColor("#3C4043")
        
        iconTint = ColorStateList.valueOf(fgColor)
        symbolTextTint = ColorStateList.valueOf(fgColor)
        
        listOf(btnBack, btnPrev, btnNext, btnLock).forEach { btn ->
            (btn.background as? GradientDrawable)?.setColor(fnBg)
            btn.imageTintList = iconTint
        }
        
        symbolAdapter.setTint(symbolTextTint)
        val surfaceColor = runtime?.resolveColor("colors.key_bg", Color.WHITE) ?: Color.WHITE
        symbolAdapter.setCellBackground(surfaceColor)
        symbolGrid.invalidateItemDecorations()
        categoryAdapter.setTheme(runtime, theme)
    }

    fun isLocked(): Boolean = locked

    fun setLocked(value: Boolean, notify: Boolean = false) {
        locked = value
        btnLock.setImageResource(if (locked) R.drawable.ic_symbols_lock else R.drawable.ic_symbols_unlock)
        btnLock.contentDescription = if (locked) "Unlock" else "Lock"
        btnLock.imageTintList = iconTint
        if (notify) onLockChanged?.invoke(locked)
    }

    private fun selectCategory(index: Int) {
        val idx = index.coerceIn(0, categories.lastIndex.coerceAtLeast(0))
        selectedCategoryIndex = idx
        categoryAdapter.setSelected(idx)

        val symbols = categories[idx].symbols
        symbolAdapter.submit(symbols)
        symbolGrid.scrollToPosition(0)
        updatePageButtons()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
    private fun pageScrollSymbols(up: Boolean) {
        val distance = symbolGrid.height - symbolGrid.paddingTop - symbolGrid.paddingBottom
        if (distance <= 0) return
        symbolGrid.smoothScrollBy(0, if (up) -distance else distance)
    }

    private fun updatePageButtons() {
        val canUp = symbolGrid.canScrollVertically(-1)
        val canDown = symbolGrid.canScrollVertically(1)
        btnPrev.isEnabled = canUp
        btnNext.isEnabled = canDown
        btnPrev.alpha = if (canUp) 1f else 0.35f
        btnNext.alpha = if (canDown) 1f else 0.35f
    }

    private class SymbolsGridAdapter(
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<SymbolCellViewHolder>() {
        private var symbols: List<String> = emptyList()
        private var tint: ColorStateList = ColorStateList.valueOf(Color.BLACK)
        private var cellBgColor: Int = Color.WHITE
        private var decoration: SymbolsGridDecoration? = null

        fun submit(list: List<String>) {
            symbols = list.filter { it.isNotBlank() }
            notifyDataSetChanged()
        }

        fun setTint(tint: ColorStateList) {
            this.tint = tint
            notifyDataSetChanged()
        }

        fun setCellBackground(color: Int) {
            this.cellBgColor = color
            notifyDataSetChanged()
        }

        fun setDivider(color: Int, widthPx: Float) {
            decoration?.updateStyle(color, widthPx)
        }

        fun attachDecoration(decoration: SymbolsGridDecoration) {
            this.decoration = decoration
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolCellViewHolder {
            val root = FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(parent.context, 52f).toInt(),
                ).apply {
                    val m = dp(parent.context, 3f).toInt()
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
                textSize = 20f
                typeface = AppFont.symbols(parent.context)
                setTextColor(tint)
            }
            root.addView(tv)
            return SymbolCellViewHolder(root, tv, onClick)
        }

        override fun onBindViewHolder(holder: SymbolCellViewHolder, position: Int) {
            val symbol = symbols.getOrNull(position).orEmpty()
            (holder.itemView.background as? GradientDrawable)?.setColor(cellBgColor)
            holder.bind(symbol, tint)
        }

        override fun getItemCount(): Int = symbols.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class SymbolCellViewHolder(
        private val root: View,
        private val tv: TextView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(root) {
        fun bind(symbol: String, tint: ColorStateList) {
            tv.text = symbol
            tv.setTextColor(tint)
            tv.typeface =
                if (isMathSymbol(symbol)) AppFont.math(tv.context) else AppFont.symbols(tv.context)
            root.setOnClickListener { if (symbol.isNotBlank()) onClick(symbol) }
        }
    }

    private class CategoryAdapter(
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.Adapter<CategoryViewHolder>() {
        private var items: List<String> = emptyList()
        private var selectedIndex: Int = 0
        private var themeRuntime: ThemeRuntime? = null
        private var themeSpec: ThemeSpec? = null

        fun submit(names: List<String>) {
            items = names
            notifyDataSetChanged()
        }

        fun setSelected(index: Int) {
            val prev = selectedIndex
            selectedIndex = index
            if (prev != index) {
                notifyItemChanged(prev)
                notifyItemChanged(index)
            } else {
                notifyItemChanged(index)
            }
        }

        fun setTheme(runtime: ThemeRuntime?, theme: ThemeSpec?) {
            themeRuntime = runtime
            themeSpec = theme
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ).apply {
                    leftMargin = dp(parent.context, 4f).toInt()
                    rightMargin = dp(parent.context, 4f).toInt()
                    topMargin = dp(parent.context, 8f).toInt()
                    bottomMargin = dp(parent.context, 8f).toInt()
                }
                gravity = Gravity.CENTER
                setPadding(dp(parent.context, 16f).toInt(), 0, dp(parent.context, 16f).toInt(), 0)
                textSize = 14f
                applyAppFont(bold = true)
            }
            return CategoryViewHolder(tv) { idx -> onClick(idx) }
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val selected = position == selectedIndex
            holder.bind(
                text = items.getOrNull(position).orEmpty(),
                selected = selected,
                runtime = themeRuntime,
                theme = themeSpec,
            )
        }

        override fun getItemCount(): Int = items.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class CategoryViewHolder(
        private val tv: TextView,
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(tv) {
        fun bind(text: String, selected: Boolean, runtime: ThemeRuntime?, theme: ThemeSpec?) {
            tv.text = text
            val bg = (tv.background as? GradientDrawable) ?: GradientDrawable().also { tv.background = it }

            val unselectedBg = runtime?.resolveColor("colors.key_bg_function", Color.parseColor("#DEE3EB"))
                ?: Color.parseColor("#DEE3EB")
            val selectedBg = runtime?.resolveColor("colors.accent", Color.parseColor("#1A73E8")) ?: Color.parseColor("#1A73E8")
            val unselectedFg = runtime?.resolveColor("colors.key_text", Color.parseColor("#3C4043")) ?: Color.parseColor("#3C4043")
            val selectedFg = Color.WHITE

            bg.shape = GradientDrawable.RECTANGLE
            bg.cornerRadius = tv.resources.displayMetrics.density * 20f
            bg.setColor(if (selected) selectedBg else unselectedBg)
            tv.setTextColor(if (selected) selectedFg else unselectedFg)

            tv.setOnClickListener {
                val pos = adapterPosition
                if (pos >= 0) onClick(pos)
            }
        }
    }

    private class SymbolsGridDecoration(
        dividerColor: Int,
        dividerWidthPx: Float,
    ) : RecyclerView.ItemDecoration() {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.FILL
            color = dividerColor
        }
        private var w = dividerWidthPx

        fun updateStyle(color: Int, widthPx: Float) {
            paint.color = color
            w = widthPx
        }

        override fun onDrawOver(c: android.graphics.Canvas, parent: RecyclerView, state: RecyclerView.State) {
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
                c.drawRect(right, top, right + w, bottom, paint)
                c.drawRect(left, bottom, right, bottom + w, paint)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val targetCellPx = dp(60f)
        val available = (w - dp(56f)).coerceAtLeast(1f)
        val span = (available / targetCellPx).toInt().coerceIn(6, 10)
        symbolLayoutManager.spanCount = span
    }

    private companion object {
        fun isMathSymbol(text: String): Boolean {
            if (text.isBlank()) return false
            return text.codePoints().anyMatch { cp ->
                when (cp) {
                    0x002B, // +
                    0x2212, // −
                    0x00D7, // ×
                    0x00F7, // ÷
                    0x003D, // =
                    0x003C, // <
                    0x003E, // >
                    -> true
                    else ->
                        (cp in 0x2200..0x22FF) || // Mathematical Operators
                            (cp in 0x27C0..0x27EF) || // Misc Math Symbols-A
                            (cp in 0x2980..0x29FF) || // Misc Math Symbols-B
                            (cp in 0x2A00..0x2AFF) // Supplemental Mathematical Operators
                }
            }
        }
    }
}
