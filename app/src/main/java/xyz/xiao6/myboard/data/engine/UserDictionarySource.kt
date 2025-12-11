package xyz.xiao6.myboard.data.engine

import android.content.Context
import xyz.xiao6.myboard.data.db.AppDatabase

/**
 * 一个从用户词典 Room 数据库加载词汇的词库源。
 */
class UserDictionarySource(context: Context) : DictionarySource {

    private val userWordDao = AppDatabase.getDatabase(context).userWordDao()

    override suspend fun search(term: String): List<Candidate> {
        if (term.isBlank()) return emptyList()

        return userWordDao.search(term).map { 
            Candidate(text = it.text, source = "User Dictionary", frequency = it.frequency.toDouble())
        }
    }
    
    // This is a new function for learning
    suspend fun learn(word: String) {
        val result = userWordDao.insert(xyz.xiao6.myboard.data.db.UserWord(text = word))
        if (result == -1L) { // -1 indicates conflict, so the word already exists
            userWordDao.incrementFrequency(word)
        }
    }
}
