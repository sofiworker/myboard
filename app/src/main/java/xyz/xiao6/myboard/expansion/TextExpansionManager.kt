package xyz.xiao6.myboard.expansion

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 文本填充管理器。
 */
class TextExpansionManager(private val context: Context) {
    private val expansions = mutableListOf<TextExpansion>()
    private var loaded = false

    fun loadAll() {
        if (loaded) return
        try {
            val text = context.assets.open("expansions/default.json").bufferedReader().readText()
            val file = Json.decodeFromString(TextExpansionFile.serializer(), text)
            expansions.addAll(file.expansions)
        } catch (_: Exception) {
            loadDefaults()
        }
        loaded = true
    }

    fun checkExpansion(input: String): String? {
        loadAll()
        return expansions.find { it.shortcut == input }?.let { expand(it.expansion) }
    }

    fun addExpansion(expansion: TextExpansion) {
        expansions.add(expansion)
    }

    fun removeExpansion(shortcut: String) {
        expansions.removeAll { it.shortcut == shortcut }
    }

    fun getExpansions(): List<TextExpansion> {
        loadAll()
        return expansions.toList()
    }

    private fun expand(template: String): String {
        var result = template
        result = result.replace(Regex("\\$\\{datetime:([^}]+)}")) { match ->
            val format = match.groupValues[1]
            SimpleDateFormat(format, Locale.getDefault()).format(Date())
        }
        result = result.replace(Regex("\\$\\{date:([^}]+)}")) { match ->
            val format = match.groupValues[1]
            SimpleDateFormat(format, Locale.getDefault()).format(Date())
        }
        return result
    }

    private fun loadDefaults() {
        expansions.addAll(listOf(
            TextExpansion("addr", "请输入您的地址", "address"),
            TextExpansion("tel", "请输入您的电话", "phone"),
            TextExpansion("eml", "请输入您的邮箱", "email"),
            TextExpansion("ts", "\${datetime:yyyy-MM-dd HH:mm}", "timestamp")
        ))
    }
}

@Serializable
data class TextExpansion(
    val shortcut: String,
    val expansion: String,
    val description: String = ""
)

@Serializable
data class TextExpansionFile(
    val version: Int = 1,
    val expansions: List<TextExpansion> = emptyList()
)
