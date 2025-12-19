package xyz.xiao6.myboard.ui.emoji

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
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

    enum class Menu {
        EMOJI,
        KAOMOJI,
    }

    data class Category(
        val categoryId: String,
        val name: String,
        val items: List<String>,
    )

    var onBack: (() -> Unit)? = null
    var onCommit: ((String) -> Unit)? = null

    // Initialize to a different value so the first selectMenu(Menu.EMOJI) actually runs.
    private var currentMenu: Menu = Menu.KAOMOJI
    private var selectedCategoryIndex: Int = 0
    private var isSearching: Boolean = false
    private var searchQuery: String = ""

    private val emojiCategories: List<Category> = builtInEmojiCategories()
    private val kaomojiCategories: List<Category> = builtInKaomojiCategories()

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
    private var accentColor: Int = Color.parseColor("#007AFF")

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

        tabEmoji = tab("Emoji").apply { setOnClickListener { selectMenu(Menu.EMOJI) } }
        tabKaomoji = tab("颜文字").apply { setOnClickListener { selectMenu(Menu.KAOMOJI) } }
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
                            searchQuery = s?.toString().orEmpty()
                            refreshPages(keepPage = false)
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

        categoryAdapter = CategoryAdapter(onClick = { idx -> selectCategory(idx) })
        categoryList.adapter = categoryAdapter

        root.addView(topBar)
        root.addView(pager)
        root.addView(categoryList)
        addView(root)

        selectMenu(Menu.EMOJI)
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        surfaceColor = runtime?.resolveColor(theme?.layout?.background?.color, Color.parseColor("#F2F2F7"))
            ?: Color.parseColor("#F2F2F7")
        accentColor = runtime?.resolveColor("colors.accent", Color.parseColor("#007AFF")) ?: Color.parseColor("#007AFF")
        pillColor = runtime?.resolveColor(theme?.toolbar?.surface?.background?.color, Color.parseColor("#1F000000"))
            ?: Color.parseColor("#1F000000")
        iconTint = ColorStateList.valueOf(runtime?.resolveColor(theme?.toolbar?.itemIcon?.tint, Color.WHITE) ?: Color.WHITE)

        (background as? GradientDrawable)?.setColor(surfaceColor)
        btnBack.imageTintList = iconTint
        btnSearch.imageTintList = iconTint
        (tabsContainer.background as? GradientDrawable)?.setColor(pillColor)
        categoryAdapter.setTheme(runtime, theme)
        applyMenuUi()
        refreshPages(keepPage = true)
    }

    private fun toggleSearch() {
        isSearching = !isSearching
        if (!isSearching) {
            searchQuery = ""
            searchField.setText("")
        }
        tabsContainer.visibility = if (isSearching) View.GONE else View.VISIBLE
        searchField.visibility = if (isSearching) View.VISIBLE else View.GONE
        refreshPages(keepPage = false)
    }

    private fun selectMenu(menu: Menu) {
        if (currentMenu == menu) return
        currentMenu = menu
        selectedCategoryIndex = 0
        isSearching = false
        searchQuery = ""
        searchField.setText("")
        tabsContainer.visibility = View.VISIBLE
        searchField.visibility = View.GONE
        applyMenuUi()
        refreshCategories()
        refreshPages(keepPage = false)
    }

    private fun selectCategory(index: Int) {
        selectedCategoryIndex = index.coerceIn(0, currentCategories().lastIndex.coerceAtLeast(0))
        categoryAdapter.setSelected(selectedCategoryIndex)
        refreshPages(keepPage = false)
    }

    private fun currentCategories(): List<Category> {
        return when (currentMenu) {
            Menu.EMOJI -> emojiCategories
            Menu.KAOMOJI -> kaomojiCategories
        }
    }

    private fun refreshCategories() {
        val list = currentCategories()
        categoryAdapter.submit(list.map { it.name })
        categoryAdapter.setSelected(selectedCategoryIndex)
    }

    private fun applyMenuUi() {
        val selectedTextColor = Color.WHITE
        val normalTextColor = Color.parseColor("#E5FFFFFF")
        when (currentMenu) {
            Menu.EMOJI -> {
                tabEmoji.setTextColor(selectedTextColor)
                tabKaomoji.setTextColor(normalTextColor)
            }
            Menu.KAOMOJI -> {
                tabEmoji.setTextColor(normalTextColor)
                tabKaomoji.setTextColor(selectedTextColor)
            }
        }
        refreshCategories()
    }

    private fun refreshPages(keepPage: Boolean) {
        val list = currentCategories()
        val cat = list.getOrNull(selectedCategoryIndex)
        val raw = cat?.items.orEmpty()
        val q = searchQuery.trim()
        val items =
            if (q.isBlank()) raw
            else raw.filter { it.contains(q, ignoreCase = true) }

        val oldPage = pager.currentItem
        val cfg =
            when (currentMenu) {
                Menu.EMOJI -> GridConfig(columns = 8, rows = 4, textSizeSp = 22f, cellHeightDp = 48f)
                Menu.KAOMOJI -> GridConfig(columns = 2, rows = 6, textSizeSp = 18f, cellHeightDp = 52f)
            }
        pagerAdapter.submit(items, cfg)
        pager.setCurrentItem(if (keepPage) oldPage.coerceIn(0, (pagerAdapter.itemCount - 1).coerceAtLeast(0)) else 0, false)
    }

    private data class GridConfig(
        val columns: Int,
        val rows: Int,
        val textSizeSp: Float,
        val cellHeightDp: Float,
    )

    private class PagesAdapter(
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<PageVH>() {
        private var pages: List<List<String>> = emptyList()
        private var cfg: GridConfig = GridConfig(columns = 8, rows = 4, textSizeSp = 22f, cellHeightDp = 48f)

        fun submit(items: List<String>, cfg: GridConfig) {
            this.cfg = cfg
            val pageSize = (cfg.columns * cfg.rows).coerceAtLeast(1)
            pages = items.filter { it.isNotBlank() }.chunked(pageSize)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
            val rv = RecyclerView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            return PageVH(rv, onClick)
        }

        override fun onBindViewHolder(holder: PageVH, position: Int) {
            holder.bind(pages.getOrNull(position).orEmpty(), cfg)
        }

        override fun getItemCount(): Int = pages.size
    }

    private class PageVH(
        private val recyclerView: RecyclerView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(recyclerView) {
        private val adapter = GridAdapter(onClick)

        init {
            recyclerView.adapter = adapter
        }

        fun bind(items: List<String>, cfg: GridConfig) {
            recyclerView.layoutManager = GridLayoutManager(recyclerView.context, cfg.columns.coerceAtLeast(1))
            adapter.setConfig(cfg)
            adapter.submit(items)
        }
    }

    private class GridAdapter(
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<CellVH>() {
        private var items: List<String> = emptyList()
        private var cfg: GridConfig = GridConfig(columns = 8, rows = 4, textSizeSp = 22f, cellHeightDp = 48f)

        fun setConfig(cfg: GridConfig) {
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
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(parent.context, 10f)
                    setColor(Color.parseColor("#FFFFFFFF"))
                    setStroke(dp(parent.context, 1f).toInt(), Color.parseColor("#22000000"))
                }
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

    @Suppress("unused")
    private fun sp(value: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, resources.displayMetrics)

    private fun builtInEmojiCategories(): List<Category> {
        val recent = listOf("😀", "😂", "🥹", "😭", "❤️", "👍", "🔥", "🙏", "🎉", "🤔", "😅", "😡")
        val smileys = listOf("😀", "😁", "😂", "🤣", "😅", "😊", "😍", "😘", "😎", "🤔", "😴", "😭", "😡", "🥹", "🥲", "😇")
        val gestures = listOf("👍", "👎", "👌", "✌️", "🤞", "🤟", "👏", "🙏", "💪", "🫶", "🫰", "🤝")
        val objects = listOf("❤️", "💔", "🔥", "⭐", "🌙", "☀️", "⚡", "🎉", "🎁", "📌", "🔔", "✅", "❌")
        return listOf(
            Category("recent", "常用", recent),
            Category("smileys", "表情", smileys),
            Category("gestures", "手势", gestures),
            Category("objects", "符号", objects),
        )
    }

    private fun builtInKaomojiCategories(): List<Category> {
        val happy = listOf("(＾▽＾)", "(≧▽≦)", "ヾ(•ω•`)o", "(•‿•)", "(๑•̀ㅂ•́)و✧", "(*^_^*)", "(｡♥‿♥｡)")
        val sad = listOf("(；′⌒`)", "(╥﹏╥)", "(ಥ﹏ಥ)", "（；´д｀）ゞ", "(｡•́︿•̀｡)")
        val angry = listOf("(＃`Д´)", "(╬▔皿▔)╯", "(╯°□°）╯︵ ┻━┻", "ಠ_ಠ", "(눈_눈)")
        val action = listOf("m(_ _)m", "（づ￣3￣）づ", "ヽ(•̀ω•́ )ゝ", "(*´∀`)~♥", "٩(ˊᗜˋ*)و")
        return listOf(
            Category("happy", "开心", happy),
            Category("sad", "难过", sad),
            Category("angry", "生气", angry),
            Category("action", "动作", action),
        )
    }
}
