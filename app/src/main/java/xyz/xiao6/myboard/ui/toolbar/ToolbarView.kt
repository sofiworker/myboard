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
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import xyz.xiao6.myboard.model.ThemeSpec
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

    var onItemClick: ((Item) -> Unit)? = null

    private var iconTint: ColorStateList = ColorStateList.valueOf(Color.WHITE)

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
        }
        adapter = ToolbarAdapter { item -> onItemClick?.invoke(item) }
        adapter.setTintProvider { iconTint }
        recyclerView.adapter = adapter
        addView(recyclerView)
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
        adapter.notifyDataSetChanged()
    }

    fun submitItems(items: List<Item>) {
        adapter.submit(items)
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
