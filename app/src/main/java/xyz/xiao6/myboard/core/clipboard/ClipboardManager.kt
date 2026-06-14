package xyz.xiao6.myboard.core.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * 剪贴板管理器。
 */
class ClipboardManagerWrapper(context: Context) {
    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val history = mutableListOf<ClipboardEntry>()
    private val maxHistory = 100

    fun copy(text: String) {
        val clip = ClipData.newPlainText("text", text)
        clipboard.setPrimaryClip(clip)
        addEntry(ClipboardEntry(text = text, timestamp = System.currentTimeMillis()))
    }

    fun paste(): String? {
        val clip = clipboard.primaryClip
        return clip?.getItemAt(0)?.text?.toString()
    }

    fun getHistory(): List<ClipboardEntry> = history.toList()

    fun clearHistory() {
        history.clear()
    }

    fun removeEntry(entry: ClipboardEntry) {
        history.remove(entry)
    }

    private fun addEntry(entry: ClipboardEntry) {
        history.add(0, entry)
        if (history.size > maxHistory) {
            history.removeAt(history.lastIndex)
        }
    }

    fun startListening() {
        clipboard.addPrimaryClipChangedListener {
            val clip = clipboard.primaryClip
            val text = clip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank()) {
                addEntry(ClipboardEntry(text = text, timestamp = System.currentTimeMillis()))
            }
        }
    }
}

data class ClipboardEntry(
    val text: String,
    val timestamp: Long,
    val isPinned: Boolean = false
)
