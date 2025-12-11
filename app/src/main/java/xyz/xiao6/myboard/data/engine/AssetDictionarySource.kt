package xyz.xiao6.myboard.data.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 一个从应用 `assets` 目录加载词典的词库源。
 */
class AssetDictionarySource(private val context: Context, private val assetPath: String) : DictionarySource {

    private val words = mutableListOf<String>()
    private var isLoaded = false

    private suspend fun loadWords() {
        if (isLoaded) return
        withContext(Dispatchers.IO) {
            try {
                context.assets.open(assetPath).bufferedReader().useLines { lines ->
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
            .map { Candidate(text = it, source = "Asset: $assetPath", frequency = 1.0) } // Assign a default frequency
    }
}
