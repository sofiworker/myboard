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
import androidx.viewpager2.widget.ViewPager2
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

    data class Category(
        val categoryId: String,
        val name: String,
        val symbols: List<String>,
    )

    var onBack: (() -> Unit)? = null
    var onCommitSymbol: ((String) -> Unit)? = null
    var onLockChanged: ((Boolean) -> Unit)? = null

    private val categories: List<Category> = builtInCategories()
    private var selectedCategoryIndex: Int = 0
    private var locked: Boolean = false

    private val pager: ViewPager2
    private val pagerAdapter: SymbolsPageAdapter
    private val categoryList: RecyclerView
    private val categoryAdapter: CategoryAdapter

    private val btnBack: ImageButton
    private val btnPrev: ImageButton
    private val btnNext: ImageButton
    private val btnLock: ImageButton

    private var iconTint: ColorStateList = ColorStateList.valueOf(Color.WHITE)
    private var symbolTextTint: ColorStateList = ColorStateList.valueOf(Color.BLACK)

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

        pager = ViewPager2(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            offscreenPageLimit = 1
            orientation = ViewPager2.ORIENTATION_VERTICAL
        }

        pagerAdapter = SymbolsPageAdapter(
            onSymbolClick = { symbol -> onCommitSymbol?.invoke(symbol) },
        )
        pager.adapter = pagerAdapter

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
        btnPrev.setOnClickListener { pager.currentItem = (pager.currentItem - 1).coerceAtLeast(0) }
        btnNext.setOnClickListener { pager.currentItem = (pager.currentItem + 1).coerceAtMost((pagerAdapter.itemCount - 1).coerceAtLeast(0)) }
        btnLock.setOnClickListener { setLocked(!locked, notify = true) }

        rightBar.addView(btnBack)
        rightBar.addView(btnPrev)
        rightBar.addView(btnNext)
        rightBar.addView(btnLock)

        top.addView(pager)
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
        updateButtons()

        pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateButtons()
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
        pagerAdapter.setTextTint(symbolTextTint)
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
        pagerAdapter.submit(symbols)
        pager.setCurrentItem(0, false)
        updateButtons()
    }

    private fun updateButtons() {
        btnPrev.isEnabled = pager.currentItem > 0
        btnNext.isEnabled = pager.currentItem < pagerAdapter.itemCount - 1
        btnPrev.alpha = if (btnPrev.isEnabled) 1f else 0.35f
        btnNext.alpha = if (btnNext.isEnabled) 1f else 0.35f
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private class SymbolsPageAdapter(
        private val onSymbolClick: (String) -> Unit,
    ) : RecyclerView.Adapter<SymbolsPageViewHolder>() {
        private var textTint: ColorStateList = ColorStateList.valueOf(Color.BLACK)

        private val pageSize = 24
        private var pages: List<List<String>> = emptyList()

        fun submit(symbols: List<String>) {
            pages = symbols
                .filter { it.isNotBlank() }
                .chunked(pageSize)
            notifyDataSetChanged()
        }

        fun setTextTint(tint: ColorStateList) {
            textTint = tint
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolsPageViewHolder {
            val rv = RecyclerView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                overScrollMode = View.OVER_SCROLL_NEVER
                layoutManager = GridLayoutManager(parent.context, 6)
            }
            return SymbolsPageViewHolder(rv, onSymbolClick)
        }

        override fun onBindViewHolder(holder: SymbolsPageViewHolder, position: Int) {
            holder.bind(pages.getOrNull(position).orEmpty(), textTint)
        }

        override fun getItemCount(): Int = pages.size
    }

    private class SymbolsPageViewHolder(
        private val recyclerView: RecyclerView,
        private val onSymbolClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(recyclerView) {
        private val adapter = SymbolsGridAdapter { symbol -> onSymbolClick(symbol) }

        init {
            recyclerView.adapter = adapter
        }

        fun bind(symbols: List<String>, tint: ColorStateList) {
            adapter.setTint(tint)
            adapter.submit(symbols)
        }
    }

    private class SymbolsGridAdapter(
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<SymbolCellViewHolder>() {
        private var symbols: List<String> = emptyList()
        private var tint: ColorStateList = ColorStateList.valueOf(Color.BLACK)

        fun submit(list: List<String>) {
            symbols = list
            notifyDataSetChanged()
        }

        fun setTint(tint: ColorStateList) {
            this.tint = tint
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolCellViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(parent.context, 44f).toInt(),
                ).apply {
                    leftMargin = dp(parent.context, 4f).toInt()
                    rightMargin = dp(parent.context, 4f).toInt()
                    topMargin = dp(parent.context, 4f).toInt()
                    bottomMargin = dp(parent.context, 4f).toInt()
                }
                gravity = Gravity.CENTER
                textSize = 18f
                typeface = Typeface.DEFAULT
                setTextColor(tint)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(parent.context, 10f)
                    setColor(Color.parseColor("#FFFFFFFF"))
                    setStroke(dp(parent.context, 1f).toInt(), Color.parseColor("#22000000"))
                }
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

    private fun builtInCategories(): List<Category> {
        val common =
            listOf(
                "，", "。", "？", "！", "、", "；", "：", "“", "”", "‘", "’", "（", "）", "《", "》", "【", "】", "—", "…", "·",
                "～", "￥", "％", "@", "#", "&", "*", "+", "=", "/", "\\",
                "😀", "😂", "🥹", "😭", "❤️", "👍",
            )
        val zh =
            listOf(
                "，", "。", "？", "！", "、", "；", "：", "“", "”", "‘", "’", "（", "）", "《", "》", "【", "】", "「", "」", "『", "』",
                "—", "…", "·", "～",
            )
        val en =
            listOf(
                ",", ".", "?", "!", ";", ":", "\"", "'", "(", ")", "[", "]", "{", "}", "<", ">", "-", "—", "_", "…",
            )
        val math =
            listOf(
                "+", "−", "×", "÷", "=", "≠", "≈", "≤", "≥", "±", "∞", "√", "∑", "∏", "∫", "π", "°", "‰", "‱",
                "∠", "⊥", "∥", "∈", "∉", "⊂", "⊃", "∩", "∪",
            )
        val net =
            listOf(
                "@", "#", "$", "%", "&", "*", "_", "-", "+", "=", "/", "\\", "|", "~", "^", ":", ";", "?", "!", ".", ",",
                "…", "—", "→", "←", "↑", "↓",
            )
        val corner =
            listOf(
                "⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹",
                "₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉",
                "ᵃ", "ᵇ", "ᶜ", "ᵈ", "ᵉ", "ᶠ", "ᵍ", "ʰ", "ᶦ", "ʲ", "ᵏ", "ˡ", "ᵐ", "ⁿ", "ᵒ", "ᵖ", "ʳ", "ˢ", "ᵗ", "ᵘ", "ᵛ", "ʷ", "ˣ", "ʸ", "ᶻ",
            )
        val pinyin =
            listOf(
                "ā", "á", "ǎ", "à",
                "ē", "é", "ě", "è",
                "ī", "í", "ǐ", "ì",
                "ō", "ó", "ǒ", "ò",
                "ū", "ú", "ǔ", "ù",
                "ǖ", "ǘ", "ǚ", "ǜ",
                "ü", "ê",
            )
        return listOf(
            Category("common", "常用", common),
            Category("zh", "中文", zh),
            Category("en", "英文", en),
            Category("math", "数学", math),
            Category("net", "网络", net),
            Category("corner", "角标", corner),
            Category("pinyin", "拼音", pinyin),
        )
    }
}
