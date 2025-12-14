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
        recyclerView.adapter = adapter
        addView(recyclerView)
    }

    fun submitItems(items: List<Item>) {
        adapter.submit(items)
    }

    private class ToolbarAdapter(
        private val onClick: (Item) -> Unit,
    ) : RecyclerView.Adapter<ToolbarViewHolder>() {

        private var items: List<Item> = emptyList()
        private val whiteTint = ColorStateList.valueOf(Color.WHITE)

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
                imageTintList = whiteTint
            }
            return ToolbarViewHolder(button, onClick)
        }

        override fun onBindViewHolder(holder: ToolbarViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
    }

    private class ToolbarViewHolder(
        private val button: ImageButton,
        private val onClick: (Item) -> Unit,
    ) : RecyclerView.ViewHolder(button) {

        fun bind(item: Item) {
            button.setImageResource(item.iconResId)
            button.contentDescription = item.contentDescription
            button.setOnClickListener { onClick(item) }
        }
    }

    private fun dp(context: Context, value: Float): Float = value * context.resources.displayMetrics.density
}
