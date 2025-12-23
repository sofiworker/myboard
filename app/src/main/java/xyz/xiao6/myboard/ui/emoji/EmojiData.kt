package xyz.xiao6.myboard.ui.emoji

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EmojiCategory(
    val categoryId: String,
    val name: String,
    val items: List<EmojiItem>,
)

@Serializable
data class KaomojiCategory(
    val categoryId: String,
    val name: String,
    val items: List<String>,
)

@Serializable
data class EmojiItem(
    val emoji: String,
    val codes: List<String> = emptyList(),
    val name: EmojiName = EmojiName(),
    val keywords: EmojiKeywords = EmojiKeywords(),
)

@Serializable
data class EmojiName(
    val zh: String = "",
    val en: String = "",
)

@Serializable
data class EmojiKeywords(
    val zh: List<String> = emptyList(),
    val en: List<String> = emptyList(),
)

@Serializable
data class EmojiCategoryFile(
    val version: Int = 1,
    val categories: List<EmojiCategory> = emptyList(),
)

@Serializable
data class KaomojiCategoryFile(
    val version: Int = 1,
    val categories: List<KaomojiCategory> = emptyList(),
)

data class EmojiCatalog(
    val emojiCategories: List<EmojiCategory>,
    val kaomojiCategories: List<KaomojiCategory>,
)

enum class EmojiMenu {
    EMOJI,
    KAOMOJI,
}

data class EmojiGridConfig(
    val columns: Int,
    val rows: Int,
    val textSizeSp: Float,
    val cellHeightDp: Float,
)

data class EmojiUiState(
    val menu: EmojiMenu,
    val categories: List<EmojiCategory>,
    val selectedCategoryIndex: Int,
    val items: List<String>,
    val isSearching: Boolean,
    val query: String,
    val gridConfig: EmojiGridConfig,
)

interface EmojiCatalogProvider {
    fun load(): EmojiCatalog
}

@OptIn(ExperimentalSerializationApi::class)
object EmojiJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
        coerceInputValues = true
    }

    fun parseCategories(text: String): EmojiCategoryFile {
        return json.decodeFromString(EmojiCategoryFile.serializer(), text)
    }

    fun parseKaomoji(text: String): KaomojiCategoryFile {
        return json.decodeFromString(KaomojiCategoryFile.serializer(), text)
    }
}

class AssetEmojiCatalogProvider(
    private val context: Context,
    private val emojiPath: String = "emoji/emoji.json",
    private val kaomojiPath: String = "emoji/kaomoji.json",
    private val fallback: EmojiCatalogProvider = BuiltInEmojiCatalogProvider,
) : EmojiCatalogProvider {
    override fun load(): EmojiCatalog {
        val emoji = loadCategories(emojiPath)
        val kaomoji = loadKaomoji(kaomojiPath)
        if (emoji != null && kaomoji != null) {
            return EmojiCatalog(emoji.categories, kaomoji.categories)
        }
        return fallback.load()
    }

    private fun loadCategories(path: String): EmojiCategoryFile? {
        val text =
            runCatching {
                context.assets.open(path).bufferedReader().use { it.readText() }
            }.getOrNull() ?: return null
        return runCatching { EmojiJsonParser.parseCategories(text) }.getOrNull()
    }

    private fun loadKaomoji(path: String): KaomojiCategoryFile? {
        val text =
            runCatching {
                context.assets.open(path).bufferedReader().use { it.readText() }
            }.getOrNull() ?: return null
        return runCatching { EmojiJsonParser.parseKaomoji(text) }.getOrNull()
    }
}

object BuiltInEmojiCatalogProvider : EmojiCatalogProvider {
    override fun load(): EmojiCatalog {
        fun item(emoji: String): EmojiItem {
            val codes = emoji.codePoints().toArray().map { Integer.toHexString(it) }
            return EmojiItem(emoji = emoji, codes = codes)
        }

        val recent = listOf("??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??").map(::item)
        val smileys = listOf("??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??").map(::item)
        val gestures = listOf("??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??").map(::item)
        val objects = listOf("??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "??", "?", "?").map(::item)

        val happy = listOf("(???)", "(???)", "?(???`)o", "(???)", "(??????)??", "(*^_^*)", "(?????)")
        val sad = listOf("(???`)", "(???)", "(???)", "???????", "(???????)")
        val angry = listOf("(#`??)", "(????)?", "(??????? ???", "?_?", "(???)")
        val action = listOf("m(_ _)m", "???3???", "?(?????)?", "(*??`)~?", "?(???)?")

        return EmojiCatalog(
            emojiCategories = listOf(
                EmojiCategory("recent", "??", recent),
                EmojiCategory("smileys", "??", smileys),
                EmojiCategory("gestures", "??", gestures),
                EmojiCategory("objects", "??", objects),
            ),
            kaomojiCategories = listOf(
                KaomojiCategory("happy", "??", happy),
                KaomojiCategory("sad", "??", sad),
                KaomojiCategory("angry", "??", angry),
                KaomojiCategory("action", "??", action),
            ),
        )
    }
}
