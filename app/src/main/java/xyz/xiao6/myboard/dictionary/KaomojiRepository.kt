package xyz.xiao6.myboard.dictionary

/**
 * 颜文字数据模型。
 */
data class KaomojiEntry(
    val text: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val frequency: Long = 0
)

data class KaomojiCategory(
    val id: String,
    val name: String,
    val kaomojis: List<KaomojiEntry>
)

/**
 * 颜文字仓库。
 */
class KaomojiRepository {
    private val categories = listOf(
        KaomojiCategory("happy", "高兴", listOf(
            KaomojiEntry("(＾▽＾)", "happy"),
            KaomojiEntry("(*≧ω≦*)", "happy"),
            KaomojiEntry("(◕‿◕✿)", "happy"),
            KaomojiEntry("(≧▽≦)", "happy"),
            KaomojiEntry("(´▽`ʃ♡ƪ)", "happy"),
            KaomojiEntry("(♥ω♥*)", "happy")
        )),
        KaomojiCategory("sad", "难过", listOf(
            KaomojiEntry("(╥﹏╥)", "sad"),
            KaomojiEntry("(T_T)", "sad"),
            KaomojiEntry("(ಥ_ಥ)", "sad"),
            KaomojiEntry("(｡•́︿•̀｡)", "sad")
        )),
        KaomojiCategory("angry", "生气", listOf(
            KaomojiEntry("(╬▔皿▔)╯", "angry"),
            KaomojiEntry("(ノ≥∀≤)ノ", "angry"),
            KaomojiEntry("(¬_¬)", "angry"),
            KaomojiEntry("( `д´)", "angry")
        )),
        KaomojiCategory("surprise", "惊讶", listOf(
            KaomojiEntry("(⊙_⊙)", "surprise"),
            KaomojiEntry("(°Д°)", "surprise"),
            KaomojiEntry("(・∀・)", "surprise"),
            KaomojiEntry("(○_○)", "surprise")
        )),
        KaomojiCategory("love", "爱意", listOf(
            KaomojiEntry("(♥ω♥*)", "love"),
            KaomojiEntry("(´▽`ʃ♡ƪ)", "love"),
            KaomojiEntry("(◍•ᴗ•◍)❤", "love"),
            KaomojiEntry("(っ˘ڡ˘ς)", "love")
        )),
        KaomojiCategory("funny", "搞笑", listOf(
            KaomojiEntry("(╯°□°)╯︵ ┻━┻", "funny"),
            KaomojiEntry("¯\\_(ツ)_/¯", "funny"),
            KaomojiEntry("( ͡° ͜ʖ ͡°)", "funny"),
            KaomojiEntry("( ˘ ³˘)♥", "funny")
        ))
    )

    fun getCategories(): List<KaomojiCategory> = categories
    fun getByCategory(categoryId: String): List<KaomojiEntry> {
        return categories.find { it.id == categoryId }?.kaomojis ?: emptyList()
    }
    fun search(query: String): List<KaomojiEntry> {
        return categories.flatMap { it.kaomojis }
            .filter { it.text.contains(query) || it.tags.any { tag -> tag.contains(query, ignoreCase = true) } }
    }
}
