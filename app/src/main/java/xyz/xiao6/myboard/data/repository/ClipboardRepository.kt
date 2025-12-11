package xyz.xiao6.myboard.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import xyz.xiao6.myboard.data.db.AppDatabase
import xyz.xiao6.myboard.data.db.ClipboardItem

class ClipboardRepository(context: Context) {

    private val clipboardDao = AppDatabase.getDatabase(context).clipboardDao()

    fun getHistory(): Flow<List<ClipboardItem>> {
        return clipboardDao.getHistory()
    }

    suspend fun insert(text: String) {
        // Avoid inserting empty or duplicate text
        if (text.isNotBlank()) {
            clipboardDao.insert(ClipboardItem(text = text))
        }
    }

    suspend fun delete(id: Int) {
        clipboardDao.delete(id)
    }

    suspend fun clear() {
        clipboardDao.clear()
    }
}
