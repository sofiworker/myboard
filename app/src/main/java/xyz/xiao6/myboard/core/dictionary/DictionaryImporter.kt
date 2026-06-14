package xyz.xiao6.myboard.core.dictionary

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader

/**
 * 词典导入导出。
 */
class DictionaryImporter {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun importFromAssets(context: Context, path: String): List<DictEntry> {
        return try {
            val text = context.assets.open(path).bufferedReader().readText()
            parseDictFile(text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun importFromUri(context: Context, uri: Uri): List<DictEntry> {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return emptyList()
            parseDictFile(text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseDictFile(text: String): List<DictEntry> {
        val entries = mutableListOf<DictEntry>()
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#")) continue

            val parts = trimmed.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val word = parts[0]
                val freq = parts[1].toLongOrNull() ?: 1
                entries.add(DictEntry(word, freq))
            } else if (parts.size == 1) {
                entries.add(DictEntry(parts[0], 1))
            }
        }
        return entries
    }

    fun loadToDict(dict: TrieDict, entries: List<DictEntry>) {
        for (entry in entries) {
            dict.insert(entry.word, entry.frequency)
        }
    }
}
