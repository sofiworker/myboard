package xyz.xiao6.myboard.ui.candidate

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 候选词/预测栏骨架（RecyclerView 横向滚动）。
 * Candidate bar skeleton (horizontal RecyclerView).
 */
class CandidateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val recyclerView: RecyclerView
    private val adapter: CandidateAdapter

    var onCandidateClick: ((String) -> Unit)? = null

    init {
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            )
            overScrollMode = OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        adapter = CandidateAdapter { text -> onCandidateClick?.invoke(text) }
        recyclerView.adapter = adapter

        addView(recyclerView)
    }

    fun submitCandidates(candidates: List<String>) {
        adapter.submit(candidates)
        visibility = if (candidates.isEmpty()) GONE else VISIBLE
    }

    private class CandidateAdapter(
        private val onClick: (String) -> Unit,
    ) : RecyclerView.Adapter<CandidateViewHolder>() {

        private var items: List<String> = emptyList()

        fun submit(list: List<String>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateViewHolder {
            val textView = TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                minWidth = (parent.context.resources.displayMetrics.density * 48f).toInt()
                gravity = Gravity.CENTER
                setPaddingRelative(
                    (parent.context.resources.displayMetrics.density * 12f).toInt(),
                    0,
                    (parent.context.resources.displayMetrics.density * 12f).toInt(),
                    0,
                )
            }
            return CandidateViewHolder(textView, onClick)
        }

        override fun onBindViewHolder(holder: CandidateViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private class CandidateViewHolder(
        private val textView: TextView,
        private val onClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(textView) {

        fun bind(text: String) {
            textView.text = text
            textView.setOnClickListener { onClick(text) }
        }
    }
}
