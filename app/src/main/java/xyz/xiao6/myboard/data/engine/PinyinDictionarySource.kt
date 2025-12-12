package xyz.xiao6.myboard.data.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 一个从 assets 加载的简易拼音->汉字词库源，用于调试中文候选。
 *
 * 词库格式：每行 "pinyin<space>候选1<space>候选2..."
 * 例如： "nihao 你好 你号 妮好"
 */
class PinyinDictionarySource(
    private val context: Context,
    private val assetPath: String = "dictionary/pinyin_hanzi.txt"
) : DictionarySource {

    private val mapping = mutableMapOf<String, List<String>>()
    private var isLoaded = false

    private suspend fun loadIfNeeded() {
        if (isLoaded) return
        withContext(Dispatchers.IO) {
            try {
                context.assets.open(assetPath).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                        val parts = trimmed.split(Regex("\\s+"))
                        if (parts.size < 2) return@forEach
                        val key = parts[0].lowercase()
                        val values = parts.drop(1)
                        mapping[key] = values
                    }
                }
                isLoaded = true
            } catch (e: IOException) {
                e.printStackTrace()
                isLoaded = true
            }
        }
    }

    override suspend fun search(term: String): List<Candidate> {
        loadIfNeeded()
        val key = term.trim().lowercase()
        if (key.isBlank()) return emptyList()

        val candidates = buildList {
            mapping[key]?.let { addAll(it) }
            // allow prefix match for incremental pinyin
            if (size < 8) {
                mapping.entries
                    .asSequence()
                    .filter { it.key.startsWith(key) && it.key != key }
                    .take(8 - size)
                    .forEach { addAll(it.value.take(3)) }
            }
        }.distinct()

        return candidates.map { Candidate(text = it, source = "Pinyin", frequency = 2.0) }
    }
}

