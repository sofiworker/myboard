package xyz.xiao6.myboard.ui.emoji

import android.content.Context
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EmojiCategory(
    val categoryId: String,
    val name: String,
    val items: List<String>,
)

@Serializable
data class EmojiCategoryFile(
    val version: Int = 1,
    val categories: List<EmojiCategory> = emptyList(),
)

data class EmojiCatalog(
    val emojiCategories: List<EmojiCategory>,
    val kaomojiCategories: List<EmojiCategory>,
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
}

class AssetEmojiCatalogProvider(
    private val context: Context,
    private val emojiPath: String = "emoji/emoji.json",
    private val kaomojiPath: String = "emoji/kaomoji.json",
    private val fallback: EmojiCatalogProvider = BuiltInEmojiCatalogProvider,
) : EmojiCatalogProvider {
    override fun load(): EmojiCatalog {
        val emoji = loadCategories(emojiPath)
        val kaomoji = loadCategories(kaomojiPath)
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
}

object BuiltInEmojiCatalogProvider : EmojiCatalogProvider {
    override fun load(): EmojiCatalog {
        val recent = listOf("😀", "😂", "🥹", "😭", "❤️", "👍", "🔥", "🙏", "🎉", "🤔", "😅", "😡")
        val smileys = listOf("😀", "😁", "😂", "🤣", "😅", "😊", "😍", "😘", "😎", "🤔", "😴", "😭", "😡", "🥹", "🥲", "😇")
        val gestures = listOf("👍", "👎", "👌", "✌️", "🤞", "🤟", "👏", "🙏", "💪", "🫶", "🫰", "🤝")
        val objects = listOf("❤️", "💔", "🔥", "⭐", "🌙", "☀️", "⚡", "🎉", "🎁", "📌", "🔔", "✅", "❌")

        val happy = listOf("(＾▽＾)", "(≧▽≦)", "ヾ(•ω•`)o", "(•‿•)", "(๑•̀ㅂ•́)و✧", "(*^_^*)", "(｡♥‿♥｡)")
        val sad = listOf("(；′⌒`)", "(╥﹏╥)", "(ಥ﹏ಥ)", "（；´д｀）ゞ", "(｡•́︿•̀｡)")
        val angry = listOf("(＃`Д´)", "(╬▔皿▔)╯", "(╯°□°）╯︵ ┻━┻", "ಠ_ಠ", "(눈_눈)")
        val action = listOf("m(_ _)m", "（づ￣3￣）づ", "ヽ(•̀ω•́ )ゝ", "(*´∀`)~♥", "٩(ˊᗜˋ*)و")

        return EmojiCatalog(
            emojiCategories = listOf(
                EmojiCategory("recent", "常用", recent),
                EmojiCategory("smileys", "表情", smileys),
                EmojiCategory("gestures", "手势", gestures),
                EmojiCategory("objects", "符号", objects),
            ),
            kaomojiCategories = listOf(
                EmojiCategory("happy", "开心", happy),
                EmojiCategory("sad", "难过", sad),
                EmojiCategory("angry", "生气", angry),
                EmojiCategory("action", "动作", action),
            ),
        )
    }
}
