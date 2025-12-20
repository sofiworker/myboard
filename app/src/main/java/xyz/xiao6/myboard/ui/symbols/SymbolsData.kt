package xyz.xiao6.myboard.ui.symbols

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SymbolCategory(
    val categoryId: String,
    val name: String,
    val symbols: List<String>,
)

@Serializable
data class SymbolCategoryFile(
    val version: Int = 1,
    val categories: List<SymbolCategory> = emptyList(),
)

interface SymbolCatalogProvider {
    fun load(): List<SymbolCategory>
}

@OptIn(ExperimentalSerializationApi::class)
object SymbolJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
        coerceInputValues = true
    }

    fun parseCategories(text: String): SymbolCategoryFile {
        return json.decodeFromString(SymbolCategoryFile.serializer(), text)
    }
}

class AssetSymbolCatalogProvider(
    private val context: Context,
    private val symbolsPath: String = "symbols/symbols.json",
    private val fallback: SymbolCatalogProvider = BuiltInSymbolCatalogProvider,
) : SymbolCatalogProvider {
    override fun load(): List<SymbolCategory> {
        val file = loadFile(symbolsPath)
        return file?.categories?.takeIf { it.isNotEmpty() } ?: fallback.load()
    }

    private fun loadFile(path: String): SymbolCategoryFile? {
        val text =
            runCatching {
                context.assets.open(path).bufferedReader().use { it.readText() }
            }.getOrNull() ?: return null
        return runCatching { SymbolJsonParser.parseCategories(text) }.getOrNull()
    }
}

object BuiltInSymbolCatalogProvider : SymbolCatalogProvider {
    override fun load(): List<SymbolCategory> {
        val common =
            listOf(
                "，", "。", "？", "！", "、", "；", "：", "“", "”", "‘", "’", "（", "）", "《", "》", "【", "】", "—", "…", "·",
                "～", "￥", "％", "@", "#", "&", "*", "+", "=", "/", "\\",
                "😀", "😂", "🥹", "😭", "❤️", "👍",
            )
        val zh =
            listOf(
                "，", "。", "？", "！", "、", "；", "：", "“", "”", "‘", "’", "（", "）", "《", "》", "【", "】", "「", "」", "『", "』",
                "—", "…", "·", "～",
            )
        val en =
            listOf(
                ",", ".", "?", "!", ";", ":", "\"", "'", "(", ")", "[", "]", "{", "}", "<", ">", "-", "—", "_", "…",
            )
        val math =
            listOf(
                "+", "−", "×", "÷", "=", "≠", "≈", "≤", "≥", "±", "∞", "√", "∑", "∏", "∫", "π", "°", "‰", "‱",
                "∠", "⊥", "∥", "∈", "∉", "⊂", "⊃", "∩", "∪",
            )
        val net =
            listOf(
                "@", "#", "$", "%", "&", "*", "_", "-", "+", "=", "/", "\\", "|", "~", "^", ":", ";", "?", "!", ".", ",",
                "…", "—", "→", "←", "↑", "↓",
            )
        val corner =
            listOf(
                "⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹",
                "₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉",
                "ᵃ", "ᵇ", "ᶜ", "ᵈ", "ᵉ", "ᶠ", "ᵍ", "ʰ", "ᶦ", "ʲ", "ᵏ", "ˡ", "ᵐ", "ⁿ", "ᵒ", "ᵖ", "ʳ", "ˢ", "ᵗ", "ᵘ", "ᵛ", "ʷ", "ˣ", "ʸ", "ᶻ",
            )
        val pinyin =
            listOf(
                "ā", "á", "ǎ", "à",
                "ē", "é", "ě", "è",
                "ī", "í", "ǐ", "ì",
                "ō", "ó", "ǒ", "ò",
                "ū", "ú", "ǔ", "ù",
                "ǖ", "ǘ", "ǚ", "ǜ",
                "ü", "ê",
            )
        return listOf(
            SymbolCategory("common", "常用", common),
            SymbolCategory("zh", "中文", zh),
            SymbolCategory("en", "英文", en),
            SymbolCategory("math", "数学", math),
            SymbolCategory("net", "网络", net),
            SymbolCategory("corner", "角标", corner),
            SymbolCategory("pinyin", "拼音", pinyin),
        )
    }
}
