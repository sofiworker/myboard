package xyz.xiao6.myboard.ui.toolbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.ui.candidate.CandidateView
import xyz.xiao6.myboard.ui.theme.ThemeRuntime

/**
 * 工具栏/功能栏（固定高度，横向滚动）。
 * Toolbar/quick-actions bar (fixed height, horizontal list).
 *
 * 约束 / Constraints:
 * - 固定高度
 * - 使用 RecyclerView 保证性能与复用
 */
class ToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    data class Item(
        val itemId: String,
        @DrawableRes val iconResId: Int,
        val contentDescription: String,
    )

    private val recyclerView: RecyclerView
    private val adapter: ToolbarAdapter
    private val candidateView: CandidateView

    var onItemClick: ((Item) -> Unit)? = null
    var onOverflowClick: (() -> Unit)? = null
    var onOverflowLongClick: (() -> Unit)? = null
    var onCandidateClick: ((String) -> Unit)? = null

    private var iconTint: ColorStateList = ColorStateList.valueOf(Color.WHITE)
    private val overflowButtonWidthPx = dp(context, 48f).toInt()
    private val overflowButton: ImageButton
    private var itemsVisible: Boolean = true
    private var showingCandidates: Boolean = false

    init {
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(context, 12f)
            setColor(Color.parseColor("#EE1F1F1F"))
            setStroke(dp(context, 1f).toInt(), Color.parseColor("#55FFFFFF"))
        }
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER_VERTICAL,
            )
            overScrollMode = OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            setPadding(paddingLeft, paddingTop, overflowButtonWidthPx, paddingBottom)
            clipToPadding = false
        }
        adapter = ToolbarAdapter { item -> onItemClick?.invoke(item) }
        adapter.setTintProvider { iconTint }
        recyclerView.adapter = adapter
        addView(recyclerView)

        candidateView = CandidateView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                Gravity.CENTER_VERTICAL,
            )
            setEmbeddedInToolbar(true)
            setPaddingRelative(paddingStart, paddingTop, overflowButtonWidthPx, paddingBottom)
            visibility = GONE
            onCandidateClick = { text -> this@ToolbarView.onCandidateClick?.invoke(text) }
        }
        addView(candidateView)

        overflowButton = ImageButton(context).apply {
            layoutParams = LayoutParams(overflowButtonWidthPx, LayoutParams.MATCH_PARENT, Gravity.END or Gravity.CENTER_VERTICAL)
            setBackgroundResource(android.R.color.transparent)
            setImageResource(android.R.drawable.arrow_down_float)
            contentDescription = "More"
            imageTintList = iconTint
            scaleType = ImageView.ScaleType.CENTER
            setOnClickListener { onOverflowClick?.invoke() }
            setOnLongClickListener {
                onOverflowLongClick?.invoke()
                true
            }
        }
        addView(overflowButton)
    }

    fun applyTheme(theme: ThemeSpec?) {
        val runtime = theme?.let { ThemeRuntime(it) }
        val bg = background as? GradientDrawable
        val surfaceBg = runtime?.resolveColor(theme?.toolbar?.surface?.background?.color, Color.parseColor("#EE1F1F1F"))
            ?: Color.parseColor("#EE1F1F1F")
        val strokeColor = runtime?.resolveColor(theme?.toolbar?.surface?.stroke?.color, Color.parseColor("#55FFFFFF"))
            ?: Color.parseColor("#55FFFFFF")
        val strokeWidth = dp(context, theme?.toolbar?.surface?.stroke?.widthDp ?: 1f).toInt()
        val corner = dp(context, theme?.toolbar?.surface?.cornerRadiusDp ?: 12f)
        bg?.apply {
            cornerRadius = corner
            setColor(surfaceBg)
            setStroke(strokeWidth, strokeColor)
        }
        iconTint = ColorStateList.valueOf(
            runtime?.resolveColor(theme?.toolbar?.itemIcon?.tint, Color.WHITE) ?: Color.WHITE,
        )
        overflowButton.imageTintList = iconTint
        candidateView.applyTheme(theme)
        adapter.notifyDataSetChanged()
    }

    fun submitItems(items: List<Item>) {
        adapter.submit(items)
    }

    fun setItemsVisible(visible: Boolean) {
        itemsVisible = visible
        updateVisibility()
    }

    fun submitCandidates(candidates: List<String>) {
        if (candidates.isEmpty()) {
            clearCandidates()
            return
        }
        showingCandidates = true
        candidateView.submitCandidates(candidates)
        updateVisibility()
    }

    fun clearCandidates() {
        showingCandidates = false
        candidateView.submitCandidates(emptyList())
        updateVisibility()
    }

    private fun updateVisibility() {
        candidateView.isVisible = showingCandidates
        recyclerView.isVisible = itemsVisible && !showingCandidates
    }

    fun setOverflowIcon(@DrawableRes resId: Int) {
        overflowButton.setImageResource(resId)
    }

    fun setOverflowRotation(deg: Float) {
        overflowButton.rotation = deg
    }

    fun setOverflowContentDescription(desc: String) {
        overflowButton.contentDescription = desc
    }

    private class ToolbarAdapter(
        private val onClick: (Item) -> Unit,
    ) : RecyclerView.Adapter<ToolbarViewHolder>() {

        private var items: List<Item> = emptyList()
        private var tintProvider: (() -> ColorStateList)? = null

        fun setTintProvider(provider: () -> ColorStateList) {
            tintProvider = provider
        }

        fun submit(list: List<Item>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolbarViewHolder {
            val button = ImageButton(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    dp(parent.context, 48f).toInt(),
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setBackgroundResource(android.R.color.transparent)
                scaleType = ImageView.ScaleType.CENTER
                adjustViewBounds = true
                imageTintList = tintProvider?.invoke() ?: ColorStateList.valueOf(Color.WHITE)
            }
            return ToolbarViewHolder(button, onClick)
        }

        override fun onBindViewHolder(holder: ToolbarViewHolder, position: Int) {
            holder.bind(items[position], tintProvider?.invoke() ?: ColorStateList.valueOf(Color.WHITE))
        }

        override fun getItemCount(): Int = items.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class ToolbarViewHolder(
        private val button: ImageButton,
        private val onClick: (Item) -> Unit,
    ) : RecyclerView.ViewHolder(button) {

        fun bind(item: Item, tint: ColorStateList) {
            button.setImageResource(item.iconResId)
            button.imageTintList = tint
            button.contentDescription = item.contentDescription
            button.setOnClickListener { onClick(item) }
        }
    }

    private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
}
