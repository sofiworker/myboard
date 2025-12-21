package xyz.xiao6.myboard.ui.emoji

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
 * Emoji/Kaomoji panel:
 * - Top bar: back | (Emoji / 顔文字) | search
 * - Content: swipeable grid (ViewPager2)
 * - Bottom: per-menu categories
 */
class EmojiLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBack: (() -> Unit)? = null
    var onCommit: ((String) -> Unit)? = null

    private val controller = EmojiController(AssetEmojiCatalogProvider(context))
    private var isUpdatingSearch: Boolean = false

    private val btnBack: ImageButton
    private val btnSearch: ImageButton
    private val tabEmoji: TextView
    private val tabKaomoji: TextView
    private val tabsContainer: LinearLayout
    private val searchField: EditText

    private val pager: ViewPager2
    private val pagerAdapter: PagesAdapter
    private val categoryList: RecyclerView
    private val categoryAdapter: CategoryAdapter

    private var iconTint: ColorStateList = ColorStateList.valueOf(Color.WHITE)
    private var surfaceColor: Int = Color.parseColor("#F2F2F7")
    private var pillColor: Int = Color.parseColor("#1F000000")
    private var gridDividerColor: Int = Color.parseColor("#22000000")
    private var gridDividerWidthPx: Float = dp(1f)

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(surfaceColor)
        }

        val root = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
        }

        val topBar = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48f).toInt())
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8f).toInt(), 0, dp(8f).toInt(), 0)
        }

        btnBack =
            ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40f).toInt(), dp(40f).toInt())
                setBackgroundResource(android.R.color.transparent)
                setImageResource(R.drawable.ic_symbols_back)
                imageTintList = iconTint
                contentDescription = "Back"
                setOnClickListener { onBack?.invoke() }
            }

        btnSearch =
            ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(40f).toInt(), dp(40f).toInt())
                setBackgroundResource(android.R.color.transparent)
                setImageResource(android.R.drawable.ic_menu_search)
                imageTintList = iconTint
                contentDescription = "Search"
                setOnClickListener { toggleSearch() }
            }

        tabsContainer =
            LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(36f).toInt(), 1f).apply {
                    leftMargin = dp(10f).toInt()
                    rightMargin = dp(10f).toInt()
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(18f)
                        setColor(pillColor)
                    }
            }

        fun tab(text: String): TextView {
            return TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                gravity = Gravity.CENTER
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                this.text = text
            }
        }

        tabEmoji = tab("Emoji").apply { setOnClickListener { controller.selectMenu(EmojiMenu.EMOJI) } }
        tabKaomoji = tab("颜文字").apply { setOnClickListener { controller.selectMenu(EmojiMenu.KAOMOJI) } }
        tabsContainer.addView(tabEmoji)
        tabsContainer.addView(tabKaomoji)

        searchField =
            EditText(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(36f).toInt(), 1f).apply {
                    leftMargin = dp(10f).toInt()
                    rightMargin = dp(10f).toInt()
                }
                setPadding(dp(12f).toInt(), 0, dp(12f).toInt(), 0)
                setSingleLine(true)
                hint = "Search"
                textSize = 14f
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(18f)
                        setColor(Color.WHITE)
                        setStroke(dp(1f).toInt(), Color.parseColor("#22000000"))
                    }
                visibility = View.GONE
                addTextChangedListener(
                    object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                        override fun afterTextChanged(s: Editable?) {
                            if (isUpdatingSearch) return
                            controller.updateQuery(s?.toString().orEmpty())
                        }
                    },
                )
            }

        topBar.addView(btnBack)
        topBar.addView(tabsContainer)
        topBar.addView(searchField)
        topBar.addView(btnSearch)

        pager =
            ViewPager2(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                offscreenPageLimit = 1
            }

        pagerAdapter = PagesAdapter(
            onClick = { item -> if (item.isNotBlank()) onCommit?.invoke(item) },
        )
        pager.adapter = pagerAdapter

        categoryList =
            RecyclerView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44f).toInt())
                overScrollMode = OVER_SCROLL_NEVER
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }

        categoryAdapter = CategoryAdapter(onClick = { idx -> controller.selectCategory(idx) })
        categoryList.adapter = categoryAdapter

        root.addView(topBar)
        root.addView(pager)
        root.addView(categoryList)
        addView(root)

        controller.attach { ui, keepPage -> renderUi(ui, keepPage) }
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        surfaceColor = runtime?.resolveColor(theme?.layout?.background?.color, Color.parseColor("#F2F2F7"))
            ?: Color.parseColor("#F2F2F7")
        pillColor = runtime?.resolveColor(theme?.toolbar?.surface?.background?.color, Color.parseColor("#1F000000"))
            ?: Color.parseColor("#1F000000")
        iconTint = ColorStateList.valueOf(runtime?.resolveColor(theme?.toolbar?.itemIcon?.tint, Color.WHITE) ?: Color.WHITE)

        (background as? GradientDrawable)?.setColor(surfaceColor)
        btnBack.imageTintList = iconTint
        btnSearch.imageTintList = iconTint
        (tabsContainer.background as? GradientDrawable)?.setColor(pillColor)
        val divider = theme?.candidates?.divider
        gridDividerColor = runtime?.resolveColor(divider?.color, gridDividerColor) ?: gridDividerColor
        gridDividerWidthPx = dp(divider?.widthDp ?: 1f)
        pagerAdapter.setDivider(gridDividerColor, gridDividerWidthPx)
        categoryAdapter.setTheme(runtime, theme)
        controller.refresh(keepPage = true)
    }

    private fun toggleSearch() {
        controller.toggleSearch()
    }

    private fun renderUi(state: EmojiUiState, keepPage: Boolean) {
        val selectedTextColor = Color.WHITE
        val normalTextColor = Color.parseColor("#E5FFFFFF")
        when (state.menu) {
            EmojiMenu.EMOJI -> {
                tabEmoji.setTextColor(selectedTextColor)
                tabKaomoji.setTextColor(normalTextColor)
            }
            EmojiMenu.KAOMOJI -> {
                tabEmoji.setTextColor(normalTextColor)
                tabKaomoji.setTextColor(selectedTextColor)
            }
        }

        tabsContainer.visibility = if (state.isSearching) View.GONE else View.VISIBLE
        searchField.visibility = if (state.isSearching) View.VISIBLE else View.GONE
        if (searchField.visibility == View.VISIBLE) {
            if (searchField.text?.toString().orEmpty() != state.query) {
                isUpdatingSearch = true
                searchField.setText(state.query)
                searchField.setSelection(state.query.length)
                isUpdatingSearch = false
            }
        }

        categoryAdapter.submit(state.categories.map { it.name })
        categoryAdapter.setSelected(state.selectedCategoryIndex)

        val oldPage = pager.currentItem
        pagerAdapter.submit(state.items, state.gridConfig)
        pager.setCurrentItem(if (keepPage) oldPage.coerceIn(0, (pagerAdapter.itemCount - 1).coerceAtLeast(0)) else 0, false)
    }

    private class PagesAdapter(
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<PageVH>() {
        private var pages: List<List<String>> = emptyList()
        private var cfg: EmojiGridConfig = EmojiGridConfig(columns = 8, rows = 4, textSizeSp = 24f, cellHeightDp = 60f)
        private var dividerColor: Int = Color.parseColor("#22000000")
        private var dividerWidthPx: Float = 1f

        fun submit(items: List<String>, cfg: EmojiGridConfig) {
            this.cfg = cfg
            val pageSize = (cfg.columns * cfg.rows).coerceAtLeast(1)
            pages = items.filter { it.isNotBlank() }.chunked(pageSize)
            notifyDataSetChanged()
        }

        fun setDivider(color: Int, widthPx: Float) {
            dividerColor = color
            dividerWidthPx = widthPx
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
            val rv = RecyclerView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            return PageVH(rv, onClick, dividerColor, dividerWidthPx)
        }

        override fun onBindViewHolder(holder: PageVH, position: Int) {
            holder.bind(pages.getOrNull(position).orEmpty(), cfg)
            holder.updateDivider(dividerColor, dividerWidthPx)
        }

        override fun getItemCount(): Int = pages.size
    }

    private class PageVH(
        private val recyclerView: RecyclerView,
        private val onClick: (String) -> Unit,
        dividerColor: Int,
        dividerWidthPx: Float,
    ) : RecyclerView.ViewHolder(recyclerView) {
        private val adapter = GridAdapter(onClick)
        private val decoration = GridDecoration(dividerColor, dividerWidthPx)

        init {
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(decoration)
        }

        fun bind(items: List<String>, cfg: EmojiGridConfig) {
            recyclerView.layoutManager = GridLayoutManager(recyclerView.context, cfg.columns.coerceAtLeast(1))
            adapter.setConfig(cfg)
            adapter.submit(items)
        }

        fun updateDivider(color: Int, widthPx: Float) {
            decoration.updateStyle(color, widthPx)
            recyclerView.invalidateItemDecorations()
        }
    }

    private class GridAdapter(
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<CellVH>() {
        private var items: List<String> = emptyList()
        private var cfg: EmojiGridConfig = EmojiGridConfig(columns = 8, rows = 4, textSizeSp = 24f, cellHeightDp = 60f)

        fun setConfig(cfg: EmojiGridConfig) {
            this.cfg = cfg
        }

        fun submit(items: List<String>) {
            this.items = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellVH {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(parent.context, cfg.cellHeightDp).toInt())
                gravity = Gravity.CENTER
                textSize = cfg.textSizeSp
                setTextColor(Color.BLACK)
                setBackgroundColor(Color.WHITE)
            }
            return CellVH(tv, onClick)
        }

        override fun onBindViewHolder(holder: CellVH, position: Int) {
            holder.bind(items.getOrNull(position).orEmpty())
        }

        override fun getItemCount(): Int = items.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class CellVH(
        private val tv: TextView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(tv) {
        fun bind(text: String) {
            tv.text = text
            tv.setOnClickListener { if (text.isNotBlank()) onClick(text) }
        }
    }

    private class CategoryAdapter(
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.Adapter<CategoryVH>() {
        private var items: List<String> = emptyList()
        private var selectedIndex: Int = 0
        private var themeRuntime: ThemeRuntime? = null
        private var themeSpec: ThemeSpec? = null

        fun submit(items: List<String>) {
            this.items = items
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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    leftMargin = dp(parent.context, 8f).toInt()
                    topMargin = dp(parent.context, 6f).toInt()
                    bottomMargin = dp(parent.context, 6f).toInt()
                }
                gravity = Gravity.CENTER
                setPadding(dp(parent.context, 12f).toInt(), 0, dp(parent.context, 12f).toInt(), 0)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            return CategoryVH(tv, onClick)
        }

        override fun onBindViewHolder(holder: CategoryVH, position: Int) {
            holder.bind(items.getOrNull(position).orEmpty(), position == selectedIndex, themeRuntime, themeSpec)
        }

        override fun getItemCount(): Int = items.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class CategoryVH(
        private val tv: TextView,
        private val onClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(tv) {
        fun bind(text: String, selected: Boolean, runtime: ThemeRuntime?, theme: ThemeSpec?) {
            tv.text = text
            val bg = (tv.background as? GradientDrawable) ?: GradientDrawable().also { tv.background = it }

            val surface = runtime?.resolveColor(theme?.toolbar?.surface?.background?.color, Color.parseColor("#EE1F1F1F"))
                ?: Color.parseColor("#EE1F1F1F")
            val selectedBg = runtime?.resolveColor("colors.accent", Color.parseColor("#007AFF")) ?: Color.parseColor("#007AFF")
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

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private class GridDecoration(
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
}
