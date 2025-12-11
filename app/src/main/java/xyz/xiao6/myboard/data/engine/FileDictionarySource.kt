package xyz.xiao6.myboard.data.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * 一个从应用内部存储加载词典的词库源。
 */
class FileDictionarySource(private val context: Context, private val fileName: String) : DictionarySource {

    private val words = mutableListOf<String>()
    private var isLoaded = false

    private suspend fun loadWords() {
        if (isLoaded) return
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return@withContext

            try {
                file.bufferedReader().useLines { lines ->
                    lines.forEach { words.add(it) }
                }
                isLoaded = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun search(term: String): List<Candidate> {
        loadWords()
        if (term.isBlank()) return emptyList()

        return words
            .filter { it.startsWith(term, ignoreCase = true) }
            .map { Candidate(text = it, source = "File: $fileName", frequency = 1.0) }
    }
}
