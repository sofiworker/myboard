package xyz.xiao6.myboard.ui.emoji

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.LruCache
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.GestureDetector
import android.view.MotionEvent
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.AppFont
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime

private fun Int.withAlpha(alpha: Int): Int {
    return (this and 0x00FFFFFF) or (alpha shl 24)
}

/**
 * Emoji/Kaomoji panel:
 * - Top bar: back | (Emoji / 顔文字) | search
 * - Content: scrollable grid
 * - Bottom: per-menu categories
 */
class EmojiLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBack: (() -> Unit)? = null
    var onCommit: ((String) -> Unit)? = null
    var onDelete: (() -> Unit)? = null

    private val controller = EmojiController(AssetEmojiCatalogProvider(context))
    private var isUpdatingSearch: Boolean = false

    private val btnBack: ImageButton
    private val btnSearch: ImageButton
    private val btnDelete: ImageButton
    private val tabEmoji: TextView
    private val tabKaomoji: TextView
    private val tabsContainer: LinearLayout
    private val searchField: EditText

    private val gridView: RecyclerView
    private val gridAdapter: GridAdapter
    private val gridDecoration: GridDecoration
    private val categoryList: RecyclerView
    private val categoryAdapter: CategoryAdapter

    private var iconTint: ColorStateList = ColorStateList.valueOf(Color.WHITE)
    private var surfaceColor: Int = Color.parseColor("#F2F2F7")
    private var pillColor: Int = Color.parseColor("#1F000000")
    private var toolbarTextColor: Int = Color.WHITE
    private var toolbarTextDimColor: Int = Color.parseColor("#E5FFFFFF")
    private var searchBoxBackgroundColor: Int = Color.WHITE
    private var searchBoxBorderColor: Int = Color.parseColor("#22000000")
    private var searchBoxTextColor: Int = Color.BLACK
    private var searchBoxHintColor: Int = Color.parseColor("#80000000")
    private var gridDividerColor: Int = Color.parseColor("#22000000")
    private var gridDividerWidthPx: Float = dp(1f)
    private var useEmojiImages: Boolean = false
    private var lastCategoryIndex: Int = -1
    private var lastMenu: EmojiMenu? = null
    private var currentState: EmojiUiState? = null

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
                setImageResource(R.drawable.search_line)
                imageTintList = iconTint
                contentDescription = "Search"
                setOnClickListener { toggleSearch() }
            }

        btnDelete =
            ImageButton(context).apply {
                layoutParams =
                    LayoutParams(dp(40f).toInt(), dp(40f).toInt()).apply {
                        gravity = Gravity.END or Gravity.TOP
                        topMargin = dp(154f).toInt()
                        rightMargin = dp(6f).toInt()
                    }
                setBackgroundResource(android.R.color.transparent)
                setImageResource(R.drawable.delete_bin_2_line)
                imageTintList = iconTint
                contentDescription = context.getString(R.string.emoji_delete)
                setOnClickListener { onDelete?.invoke() }
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
                applyAppFont(bold = true)
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
                applyAppFont()
                background =
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(18f)
                        setColor(searchBoxBackgroundColor)
                        setStroke(dp(1f).toInt(), searchBoxBorderColor)
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

        gridView =
            RecyclerView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                overScrollMode = View.OVER_SCROLL_NEVER
            }
        gridAdapter = GridAdapter(onClick = { item -> if (item.isNotBlank()) onCommit?.invoke(item) })
        gridDecoration = GridDecoration(gridDividerColor, gridDividerWidthPx)
        gridView.adapter = gridAdapter
        gridView.addItemDecoration(gridDecoration)

        val gestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float,
                    ): Boolean {
                        val state = currentState ?: return false
                        if (state.isSearching) return false
                        val dx = (e2.x - (e1?.x ?: e2.x))
                        val dy = (e2.y - (e1?.y ?: e2.y))
                        if (kotlin.math.abs(dx) < kotlin.math.abs(dy)) return false
                        if (kotlin.math.abs(velocityX) < 800f) return false
                        val max = state.categories.lastIndex.coerceAtLeast(0)
                        val next = if (dx < 0) state.selectedCategoryIndex + 1 else state.selectedCategoryIndex - 1
                        controller.selectCategory(next.coerceIn(0, max))
                        return true
                    }
                },
            )
        gridView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }

        categoryList =
            RecyclerView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44f).toInt())
                overScrollMode = OVER_SCROLL_NEVER
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            }

        categoryAdapter = CategoryAdapter(onClick = { idx -> controller.selectCategory(idx) })
        categoryList.adapter = categoryAdapter

        root.addView(topBar)
        root.addView(gridView)
        root.addView(categoryList)
        addView(root)
        addView(btnDelete)

        controller.attach { ui, keepPage -> renderUi(ui, keepPage) }
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        surfaceColor = runtime?.resolveColor(theme?.layout?.background?.color, Color.parseColor("#F2F2F7"))
            ?: Color.parseColor("#F2F2F7")
        pillColor = runtime?.resolveColor(theme?.toolbar?.surface?.background?.color, Color.parseColor("#1F000000"))
            ?: Color.parseColor("#1F000000")
        iconTint = ColorStateList.valueOf(runtime?.resolveColor(theme?.toolbar?.itemIcon?.tint, Color.WHITE) ?: Color.WHITE)
        toolbarTextColor = runtime?.resolveColor(theme?.toolbar?.itemText?.color, Color.WHITE) ?: Color.WHITE
        toolbarTextDimColor = toolbarTextColor.withAlpha(230)
        searchBoxBackgroundColor = runtime?.resolveColor(theme?.toolbar?.surface?.background?.color, Color.WHITE)
            ?: Color.WHITE
        searchBoxBorderColor = runtime?.resolveColor(theme?.candidates?.divider?.color, Color.parseColor("#22000000"))
            ?: Color.parseColor("#22000000")
        searchBoxTextColor = runtime?.resolveColor("colors.key_text", Color.BLACK) ?: Color.BLACK
        searchBoxHintColor = searchBoxTextColor.withAlpha(128)

        (background as? GradientDrawable)?.setColor(surfaceColor)
        btnBack.imageTintList = iconTint
        btnSearch.imageTintList = iconTint
        btnDelete.imageTintList = iconTint
        (tabsContainer.background as? GradientDrawable)?.setColor(pillColor)
        (searchField.background as? GradientDrawable)?.apply {
            setColor(searchBoxBackgroundColor)
            setStroke(dp(1f).toInt(), searchBoxBorderColor)
        }
        searchField.setTextColor(searchBoxTextColor)
        searchField.setHintTextColor(searchBoxHintColor)
        val divider = theme?.candidates?.divider
        gridDividerColor = runtime?.resolveColor(divider?.color, gridDividerColor) ?: gridDividerColor
        gridDividerWidthPx = dp(divider?.widthDp ?: 1f)
        gridDecoration.updateStyle(gridDividerColor, gridDividerWidthPx)
        gridView.invalidateItemDecorations()
        categoryAdapter.setTheme(runtime, theme)
        controller.refresh(keepPage = true)
    }

    fun setUseEmojiImages(enabled: Boolean) {
        if (useEmojiImages == enabled) return
        useEmojiImages = enabled
        gridAdapter.setUseImages(enabled)
    }

    private fun toggleSearch() {
        controller.toggleSearch()
    }

    private fun renderUi(state: EmojiUiState, keepPage: Boolean) {
        currentState = state
        when (state.menu) {
            EmojiMenu.EMOJI -> {
                tabEmoji.setTextColor(toolbarTextColor)
                tabKaomoji.setTextColor(toolbarTextDimColor)
            }
            EmojiMenu.KAOMOJI -> {
                tabEmoji.setTextColor(toolbarTextDimColor)
                tabKaomoji.setTextColor(toolbarTextColor)
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

        val shouldResetScroll =
            !keepPage || state.menu != lastMenu || state.selectedCategoryIndex != lastCategoryIndex
        gridAdapter.setConfig(state.gridConfig)
        gridAdapter.setUseImages(useEmojiImages)
        gridAdapter.submit(state.items)
        gridView.layoutManager = GridLayoutManager(context, state.gridConfig.columns.coerceAtLeast(1))
        if (shouldResetScroll) {
            gridView.scrollToPosition(0)
        }
        lastMenu = state.menu
        lastCategoryIndex = state.selectedCategoryIndex
    }

    private class GridAdapter(
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<CellVH>() {
        private var items: List<EmojiItem> = emptyList()
        private var cfg: EmojiGridConfig = EmojiGridConfig(columns = 8, rows = 4, textSizeSp = 24f, cellHeightDp = 60f)
        private var useImages: Boolean = false

        fun setConfig(cfg: EmojiGridConfig) {
            this.cfg = cfg
        }

        fun setUseImages(enabled: Boolean) {
            if (useImages == enabled) return
            useImages = enabled
            notifyDataSetChanged()
        }

        fun submit(items: List<EmojiItem>) {
            this.items = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellVH {
            val root = FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(parent.context, cfg.cellHeightDp).toInt())
                setBackgroundColor(Color.WHITE)
            }
            val iv = ImageView(parent.context).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = ImageView.ScaleType.FIT_CENTER
                visibility = View.GONE
                contentDescription = null
            }
            val tv = TextView(parent.context).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                gravity = Gravity.CENTER
                textSize = cfg.textSizeSp
                setTextColor(Color.BLACK)
                typeface = AppFont.emoji(parent.context)
            }
            root.addView(iv)
            root.addView(tv)
            return CellVH(root, iv, tv, onClick)
        }

        override fun onBindViewHolder(holder: CellVH, position: Int) {
            if (useImages) {
                val rv = holder.itemView.parent as? RecyclerView
                val width = rv?.measuredWidth ?: rv?.width ?: 0
                if (width > 0) {
                    val target = (width / cfg.columns.coerceAtLeast(1)).coerceAtLeast(1)
                    val lp = holder.itemView.layoutParams
                    if (lp != null && lp.height != target) {
                        lp.height = target
                        holder.itemView.layoutParams = lp
                    }
                }
            }
            holder.bind(items.getOrNull(position), useImages)
        }

        override fun getItemCount(): Int = items.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class CellVH(
        private val root: View,
        private val iv: ImageView,
        private val tv: TextView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(root) {
        fun bind(item: EmojiItem?, useImages: Boolean) {
            val emoji = item?.emoji.orEmpty()
            val path = item?.image?.path.orEmpty()
            val bitmap =
                if (useImages && path.isNotBlank()) {
                    EmojiImageCache.getBitmap(iv.context, path)
                } else {
                    null
                }
            if (bitmap != null) {
                iv.setImageBitmap(bitmap)
                iv.scaleType = ImageView.ScaleType.CENTER_INSIDE
                val pad = dp(iv.context, 6f)
                iv.setPadding(pad, pad, pad, pad)
                iv.visibility = View.VISIBLE
                tv.visibility = View.GONE
            } else {
                iv.setImageDrawable(null)
                iv.setPadding(0, 0, 0, 0)
                iv.visibility = View.GONE
                tv.visibility = View.VISIBLE
                tv.text = emoji
            }
            root.setOnClickListener { if (emoji.isNotBlank()) onClick(emoji) }
        }

        private fun dp(context: Context, value: Float): Int {
            return (value * context.resources.displayMetrics.density).toInt()
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
                applyAppFont(bold = true)
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

    private object EmojiImageCache {
        private val cache: LruCache<String, Bitmap> = run {
            val maxKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
            val sizeKb = (maxKb / 16).coerceAtLeast(1024)
            object : LruCache<String, Bitmap>(sizeKb) {
                override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
            }
        }

        fun getBitmap(context: Context, assetPath: String): Bitmap? {
            if (assetPath.isBlank()) return null
            cache.get(assetPath)?.let { return it }
            val bitmap =
                runCatching {
                    context.assets.open(assetPath).use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }.getOrNull() ?: return null
            cache.put(assetPath, bitmap)
            return bitmap
        }
    }
}
