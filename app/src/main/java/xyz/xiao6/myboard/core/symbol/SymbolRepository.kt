package xyz.xiao6.myboard.core.symbol

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 符号分类。
 */
@Serializable
data class SymbolCategory(
    val categoryId: String,
    val name: String,
    val symbols: List<String>
)

@Serializable
data class SymbolCategoryFile(
    val version: Int = 1,
    val categories: List<SymbolCategory> = emptyList()
)

/**
 * 符号仓库。
 */
class SymbolRepository(private val context: Context) {
    private val categories = mutableListOf<SymbolCategory>()
    private var loaded = false

    fun loadAll() {
        if (loaded) return
        try {
            val text = context.assets.open("symbols/symbols.json").bufferedReader().readText()
            val file = Json.decodeFromString(SymbolCategoryFile.serializer(), text)
            categories.addAll(file.categories)
        } catch (_: Exception) {
            loadDefaults()
        }
        loaded = true
    }

    fun getCategories(): List<SymbolCategory> {
        loadAll()
        return categories.toList()
    }

    fun search(query: String): List<String> {
        loadAll()
        return categories.flatMap { it.symbols }
            .filter { it.contains(query, ignoreCase = true) }
    }

    private fun loadDefaults() {
        categories.add(SymbolCategory("common", "常用",
            listOf("，", "。", "？", "！", "、", "；", "：", """, """, "'", "'", "（", "）", "《", "》", "【", "】", "—", "…", "·", "@", "#", "$", "%", "&", "*", "+", "=", "/", "\\")
        ))
        categories.add(SymbolCategory("math", "数学",
            listOf("+", "−", "×", "÷", "=", "≠", "≈", "≤", "≥", "±", "∞", "√", "∑", "∏", "∫", "π", "°", "‰")
        ))
        categories.add(SymbolCategory("arrow", "箭头",
            listOf("←", "↑", "→", "↓", "↔", "↕", "⇐", "⇑", "⇒", "⇓", "⇔", "⇕")
        ))
        categories.add(SymbolCategory("currency", "货币",
            listOf("$", "€", "£", "¥", "¢", "₹", "₩", "₽", "₺", "₴")
        ))
    }
}
