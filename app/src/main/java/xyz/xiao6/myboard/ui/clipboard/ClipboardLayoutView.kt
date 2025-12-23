package xyz.xiao6.myboard.ui.clipboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.theme.applyAppFont
import xyz.xiao6.myboard.ui.theme.ThemeRuntime

class ClipboardLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    data class ClipboardEntry(
        val id: Long,
        val text: String,
        val timestamp: Long,
    )

    var onBack: (() -> Unit)? = null
    var onCommit: ((ClipboardEntry) -> Unit)? = null
    var onClearAll: (() -> Unit)? = null
    var onDeleteSelected: ((Set<Long>) -> Unit)? = null

    private val topBar: LinearLayout
    private val btnBack: ImageButton
    private val btnAction: ImageButton
    private val titleView: TextView
    private val listView: RecyclerView
    private val emptyView: TextView
    private val adapter: ClipboardAdapter

    private var selectionMode: Boolean = false
    private val selectedIds = LinkedHashSet<Long>()
    private var clearAllConfirmPending: Boolean = false

    private var iconTint: ColorStateList = ColorStateList.valueOf(Color.WHITE)
    private var textTint: ColorStateList = ColorStateList.valueOf(Color.BLACK)
    private var itemBgColor: Int = Color.WHITE
    private var itemStrokeColor: Int = Color.parseColor("#14000000")
    private var itemSelectedBgColor: Int = Color.parseColor("#33007AFF")
    private var itemCornerRadius: Float = dp(10f)

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#F2F2F7"))
        }

        val root = LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
        }

        topBar = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48f).toInt(),
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4f).toInt(), 0, dp(4f).toInt(), 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#F2F2F7"))
                setStroke(dp(1f).toInt(), Color.parseColor("#14000000"))
            }
        }

        fun actionButton(desc: String): ImageButton {
            return ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(48f).toInt(), dp(48f).toInt())
                setBackgroundResource(android.R.color.transparent)
                contentDescription = desc
                imageTintList = iconTint
                scaleType = android.widget.ImageView.ScaleType.CENTER
            }
        }

        btnBack = actionButton("Back")
        btnAction = actionButton("Clear all")
        btnBack.setImageResource(android.R.drawable.ic_media_previous)
        titleView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
            text = "Clipboard"
            setTextColor(textTint)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
            applyAppFont(bold = true)
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
        }

        topBar.addView(btnBack)
        topBar.addView(titleView)
        topBar.addView(btnAction)

        listView = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
            overScrollMode = OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(context)
        }

        emptyView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
            gravity = Gravity.CENTER
            text = "No clipboard items"
            setTextColor(textTint)
            applyAppFont()
            visibility = View.GONE
        }

        adapter = ClipboardAdapter(
            onClick = { entry ->
                clearClearAllConfirm()
                if (selectionMode) {
                    toggleSelection(entry.id)
                } else {
                    onCommit?.invoke(entry)
                }
            },
            onLongClick = { entry ->
                clearClearAllConfirm()
                if (!selectionMode) {
                    setSelectionMode(true)
                    toggleSelection(entry.id)
                } else {
                    toggleSelection(entry.id)
                }
            },
        )
        listView.adapter = adapter

        root.addView(topBar)
        root.addView(listView)
        root.addView(emptyView)
        addView(root)

        btnBack.setOnClickListener { onBack?.invoke() }
        btnAction.setOnClickListener {
            if (selectionMode) {
                if (selectedIds.isNotEmpty()) {
                    onDeleteSelected?.invoke(selectedIds.toSet())
                } else {
                    setSelectionMode(false)
                }
            } else {
                if (adapter.currentItems.isEmpty()) return@setOnClickListener
                if (clearAllConfirmPending) {
                    clearAllConfirmPending = false
                    updateActionUi()
                    onClearAll?.invoke()
                } else {
                    clearAllConfirmPending = true
                    updateActionUi()
                }
            }
        }

        updateActionUi()
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        val bg = runtime?.resolveColor(theme?.layout?.background?.color, Color.parseColor("#F2F2F7"))
            ?: Color.parseColor("#F2F2F7")
        (background as? GradientDrawable)?.setColor(bg)

        val topBg = runtime?.resolveColor(theme?.toolbar?.surface?.background?.color, bg) ?: bg
        val topStroke = runtime?.resolveColor(theme?.toolbar?.surface?.stroke?.color, Color.parseColor("#14000000"))
            ?: Color.parseColor("#14000000")
        val topStrokeWidth = dp(context, theme?.toolbar?.surface?.stroke?.widthDp ?: 1f).toInt()
        (topBar.background as? GradientDrawable)?.apply {
            setColor(topBg)
            setStroke(topStrokeWidth, topStroke)
        }

        iconTint = ColorStateList.valueOf(
            runtime?.resolveColor(theme?.toolbar?.itemIcon?.tint, Color.WHITE) ?: Color.WHITE,
        )
        textTint = ColorStateList.valueOf(
            runtime?.resolveColor(theme?.toolbar?.itemText?.color, Color.BLACK) ?: Color.BLACK,
        )
        itemBgColor = runtime?.resolveColor("colors.key_bg", Color.WHITE) ?: Color.WHITE
        itemStrokeColor = runtime?.resolveColor("colors.stroke", Color.parseColor("#14000000"))
            ?: Color.parseColor("#14000000")

        val accent = runtime?.resolveColor("colors.accent", Color.parseColor("#007AFF")) ?: Color.parseColor("#007AFF")
        itemSelectedBgColor = ColorUtils.setAlphaComponent(accent, 0x26)
        itemCornerRadius = dp(context, theme?.global?.paint?.cornerRadiusDp ?: 10f)

        btnBack.imageTintList = iconTint
        btnAction.imageTintList = iconTint
        titleView.setTextColor(textTint)
        emptyView.setTextColor(textTint)

        adapter.updateTheme(
            textColor = textTint.defaultColor,
            itemBgColor = itemBgColor,
            itemStrokeColor = itemStrokeColor,
            itemSelectedBgColor = itemSelectedBgColor,
            cornerRadius = itemCornerRadius,
        )
    }

    fun submitItems(items: List<ClipboardEntry>) {
        adapter.submit(items, selectedIds, selectionMode)
        listView.isVisible = items.isNotEmpty()
        emptyView.isVisible = items.isEmpty()
        if (items.isEmpty()) {
            clearAllConfirmPending = false
        }
        updateActionUi()
    }

    fun setSelectionMode(enabled: Boolean) {
        if (selectionMode == enabled) return
        if (enabled) clearClearAllConfirm()
        selectionMode = enabled
        if (!selectionMode) {
            selectedIds.clear()
        }
        adapter.submit(adapter.currentItems, selectedIds, selectionMode)
        updateActionUi()
    }

    fun clearSelection() {
        selectedIds.clear()
        if (selectionMode) {
            adapter.submit(adapter.currentItems, selectedIds, selectionMode)
            updateActionUi()
        }
    }

    fun enterSelectionMode() {
        setSelectionMode(true)
    }

    private fun toggleSelection(id: Long) {
        if (!selectionMode) return
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        adapter.submit(adapter.currentItems, selectedIds, selectionMode)
        updateActionUi()
    }

    private fun clearClearAllConfirm() {
        if (!clearAllConfirmPending) return
        clearAllConfirmPending = false
        updateActionUi()
    }

    private fun updateActionUi() {
        if (selectionMode) {
            val count = selectedIds.size
            titleView.text = if (count > 0) "Selected $count" else "Select items"
            btnAction.setImageResource(android.R.drawable.ic_menu_delete)
            btnAction.contentDescription = "Delete"
        } else if (clearAllConfirmPending) {
            titleView.text = "Tap again to clear"
            btnAction.setImageResource(android.R.drawable.ic_menu_delete)
            btnAction.contentDescription = "Confirm clear all"
        } else {
            titleView.text = "Clipboard"
            btnAction.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            btnAction.contentDescription = "Clear all"
        }
        btnAction.imageTintList = iconTint
    }

    private class ClipboardAdapter(
        private val onClick: (ClipboardEntry) -> Unit,
        private val onLongClick: (ClipboardEntry) -> Unit,
    ) : RecyclerView.Adapter<ClipboardViewHolder>() {
        var currentItems: List<ClipboardEntry> = emptyList()
            private set
        private var selectedIds: Set<Long> = emptySet()
        private var selectionMode: Boolean = false
        private var textColor: Int = Color.BLACK
        private var itemBgColor: Int = Color.WHITE
        private var itemStrokeColor: Int = Color.parseColor("#14000000")
        private var itemSelectedBgColor: Int = Color.parseColor("#33007AFF")
        private var cornerRadius: Float = 0f

        fun submit(items: List<ClipboardEntry>, selectedIds: Set<Long>, selectionMode: Boolean) {
            this.currentItems = items
            this.selectedIds = selectedIds
            this.selectionMode = selectionMode
            notifyDataSetChanged()
        }

        fun updateTheme(
            textColor: Int,
            itemBgColor: Int,
            itemStrokeColor: Int,
            itemSelectedBgColor: Int,
            cornerRadius: Float,
        ) {
            this.textColor = textColor
            this.itemBgColor = itemBgColor
            this.itemStrokeColor = itemStrokeColor
            this.itemSelectedBgColor = itemSelectedBgColor
            this.cornerRadius = cornerRadius
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipboardViewHolder {
            val tv = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = dp(parent.context, 6f).toInt()
                    bottomMargin = dp(parent.context, 6f).toInt()
                    leftMargin = dp(parent.context, 10f).toInt()
                    rightMargin = dp(parent.context, 10f).toInt()
                }
                setPadding(dp(parent.context, 14f).toInt(), dp(parent.context, 12f).toInt(),
                    dp(parent.context, 14f).toInt(), dp(parent.context, 12f).toInt())
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                applyAppFont()
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
            }
            return ClipboardViewHolder(tv, onClick, onLongClick)
        }

        override fun onBindViewHolder(holder: ClipboardViewHolder, position: Int) {
            val entry = currentItems[position]
            val selected = selectionMode && selectedIds.contains(entry.id)
            holder.bind(
                entry = entry,
                textColor = textColor,
                bgColor = if (selected) itemSelectedBgColor else itemBgColor,
                strokeColor = itemStrokeColor,
                cornerRadius = cornerRadius,
            )
        }

        override fun getItemCount(): Int = currentItems.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class ClipboardViewHolder(
        private val textView: TextView,
        private val onClick: (ClipboardEntry) -> Unit,
        private val onLongClick: (ClipboardEntry) -> Unit,
    ) : RecyclerView.ViewHolder(textView) {
        fun bind(
            entry: ClipboardEntry,
            textColor: Int,
            bgColor: Int,
            strokeColor: Int,
            cornerRadius: Float,
        ) {
            textView.text = entry.text
            textView.setTextColor(textColor)
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(bgColor)
                setStroke(dp(textView.context, 1f).toInt(), strokeColor)
                this.cornerRadius = cornerRadius
            }
            textView.background = bg
            textView.setOnClickListener { onClick(entry) }
            textView.setOnLongClickListener {
                onLongClick(entry)
                true
            }
        }

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
}
