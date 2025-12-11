package xyz.xiao6.myboard.data.engine

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * 一个从应用内部存储加载词典的词库源。
 */
class FileDictionarySource(private val context: Context, private val fileName: String) : DictionarySource {

    private val words = mutableListOf<String>()

    init {
        loadWords()
    }

    private fun loadWords() {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return

        try {
            file.bufferedReader().useLines { lines ->
                lines.forEach { words.add(it) }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun search(term: String): List<Candidate> {
        if (term.isBlank()) return emptyList()

        return words
            .filter { it.startsWith(term, ignoreCase = true) }
            .map { Candidate(text = it, source = "File: $fileName", frequency = 1.0) }
    }
}
