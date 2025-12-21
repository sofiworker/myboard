package xyz.xiao6.myboard.ui.symbols

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.model.ThemeSpec
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

    private var iconTint: ColorStateList = ColorStateList.valueOf(Color.WHITE)
    private var symbolTextTint: ColorStateList = ColorStateList.valueOf(Color.BLACK)
    private var symbolGridDividerColor: Int = Color.parseColor("#22000000")
    private var symbolGridDividerWidthPx: Float = dp(1f)

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#F2F2F7"))
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
        symbolGridDecoration = SymbolsGridDecoration(symbolGridDividerColor, symbolGridDividerWidthPx)
        symbolGrid = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutManager = symbolLayoutManager
            itemAnimator = null
        }

        symbolAdapter = SymbolsGridAdapter(
            onClick = { symbol -> onCommitSymbol?.invoke(symbol) },
        )
        symbolGrid.adapter = symbolAdapter
        symbolGrid.addItemDecoration(symbolGridDecoration)
        symbolAdapter.attachDecoration(symbolGridDecoration)

        val rightBar = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(56f).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4f).toInt(), dp(6f).toInt(), dp(4f).toInt(), dp(6f).toInt())
        }

        fun controlButton(@androidx.annotation.DrawableRes iconResId: Int, desc: String): ImageButton {
            return ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48f).toInt(), 0, 1f).apply {
                    topMargin = dp(4f).toInt()
                    bottomMargin = dp(4f).toInt()
                }
                setBackgroundResource(android.R.color.transparent)
                setImageResource(iconResId)
                contentDescription = desc
                imageTintList = iconTint
                scaleType = android.widget.ImageView.ScaleType.CENTER
            }
        }

        btnBack = controlButton(R.drawable.ic_symbols_back, "Back")
        btnPrev = controlButton(R.drawable.ic_symbols_prev, "Previous page (up)")
        btnNext = controlButton(R.drawable.ic_symbols_next, "Next page (down)")
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
                dp(44f).toInt(),
            )
            overScrollMode = OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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
        val bg = runtime?.resolveColor(theme?.layout?.background?.color, Color.parseColor("#F2F2F7"))
            ?: Color.parseColor("#F2F2F7")
        (background as? GradientDrawable)?.setColor(bg)

        iconTint = ColorStateList.valueOf(
            runtime?.resolveColor(theme?.toolbar?.itemIcon?.tint, Color.WHITE) ?: Color.WHITE,
        )
        symbolTextTint = ColorStateList.valueOf(
            runtime?.resolveColor("colors.key_text", Color.BLACK) ?: Color.BLACK,
        )
        btnBack.imageTintList = iconTint
        btnPrev.imageTintList = iconTint
        btnNext.imageTintList = iconTint
        btnLock.imageTintList = iconTint
        symbolAdapter.setTint(symbolTextTint)
        val divider = theme?.candidates?.divider
        symbolGridDividerColor =
            runtime?.resolveColor(divider?.color, Color.parseColor("#22000000")) ?: Color.parseColor("#22000000")
        symbolGridDividerWidthPx = dp(divider?.widthDp ?: 1f)
        symbolAdapter.setDivider(symbolGridDividerColor, symbolGridDividerWidthPx)
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
        private var decoration: SymbolsGridDecoration? = null

        fun submit(list: List<String>) {
            symbols = list.filter { it.isNotBlank() }
            notifyDataSetChanged()
        }

        fun setTint(tint: ColorStateList) {
            this.tint = tint
        }

        fun setDivider(color: Int, widthPx: Float) {
            decoration?.updateStyle(color, widthPx)
        }

        fun attachDecoration(decoration: SymbolsGridDecoration) {
            this.decoration = decoration
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolCellViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(parent.context, 60f).toInt(),
                )
                gravity = Gravity.CENTER
                textSize = 20f
                typeface = Typeface.DEFAULT
                setTextColor(tint)
                setBackgroundColor(Color.WHITE)
            }
            return SymbolCellViewHolder(tv, onClick)
        }

        override fun onBindViewHolder(holder: SymbolCellViewHolder, position: Int) {
            holder.bind(symbols.getOrNull(position).orEmpty(), tint)
        }

        override fun getItemCount(): Int = symbols.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class SymbolCellViewHolder(
        private val tv: TextView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(tv) {
        fun bind(symbol: String, tint: ColorStateList) {
            tv.text = symbol
            tv.setTextColor(tint)
            tv.setOnClickListener { if (symbol.isNotBlank()) onClick(symbol) }
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
                    leftMargin = dp(parent.context, 8f).toInt()
                    rightMargin = dp(parent.context, 0f).toInt()
                    topMargin = dp(parent.context, 6f).toInt()
                    bottomMargin = dp(parent.context, 6f).toInt()
                }
                gravity = Gravity.CENTER
                setPadding(dp(parent.context, 12f).toInt(), 0, dp(parent.context, 12f).toInt(), 0)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
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

            val surface = runtime?.resolveColor(theme?.toolbar?.surface?.background?.color, Color.parseColor("#EE1F1F1F"))
                ?: Color.parseColor("#EE1F1F1F")
            val selectedBg = runtime?.resolveColor("colors.accent", Color.parseColor("#FF3B30")) ?: Color.parseColor("#FF3B30")
            val fg = runtime?.resolveColor(theme?.toolbar?.itemText?.color, Color.WHITE) ?: Color.WHITE

            bg.shape = GradientDrawable.RECTANGLE
            bg.cornerRadius = tv.resources.displayMetrics.density * 12f
            bg.setColor(if (selected) selectedBg else surface)
            tv.setTextColor(fg)
            tv.alpha = if (selected) 1f else 0.85f

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

}
