package xyz.xiao6.myboard.dictionary

/**
 * Emoji 数据模型。
 */
data class EmojiEntry(
    val emoji: String,
    val name: String,
    val category: String,
    val keywords: List<String> = emptyList()
)

data class EmojiCategory(
    val id: String,
    val name: String,
    val icon: String,
    val emojis: List<EmojiEntry>
)

/**
 * Emoji 仓库。
 */
class EmojiRepository {
    private val categories = listOf(
        EmojiCategory("smileys", "笑脸", "😀", listOf(
            EmojiEntry("😀", "grinning face", "smileys"),
            EmojiEntry("😁", "beaming face", "smileys"),
            EmojiEntry("😂", "joy tears", "smileys"),
            EmojiEntry("🤣", "rolling laughing", "smileys"),
            EmojiEntry("😃", "smile", "smileys"),
            EmojiEntry("😄", "smile eyes", "smileys"),
            EmojiEntry("😅", "sweat smile", "smileys"),
            EmojiEntry("😆", "grin", "smileys"),
            EmojiEntry("😇", "innocent", "smileys"),
            EmojiEntry("🥰", "hearts face", "smileys"),
            EmojiEntry("😍", "heart eyes", "smileys"),
            EmojiEntry("🤩", "star struck", "smileys"),
            EmojiEntry("😘", "kiss", "smileys"),
            EmojiEntry("😜", "wink", "smileys"),
            EmojiEntry("🤔", "thinking", "smileys"),
            EmojiEntry("😎", "cool", "smileys"),
            EmojiEntry("🥳", "party", "smileys"),
            EmojiEntry("😢", "cry", "smileys"),
            EmojiEntry("😭", "sob", "smileys"),
            EmojiEntry("😡", "angry", "smileys")
        )),
        EmojiCategory("people", "人物", "👋", listOf(
            EmojiEntry("👋", "wave", "people"),
            EmojiEntry("👍", "thumbs up", "people"),
            EmojiEntry("👎", "thumbs down", "people"),
            EmojiEntry("👏", "clap", "people"),
            EmojiEntry("🙌", "raise hands", "people"),
            EmojiEntry("🤝", "handshake", "people"),
            EmojiEntry("🙏", "pray", "people"),
            EmojiEntry("💪", "strong", "people"),
            EmojiEntry("✌️", "peace", "people"),
            EmojiEntry("🤞", "crossed fingers", "people")
        )),
        EmojiCategory("animals", "动物", "🐶", listOf(
            EmojiEntry("🐶", "dog", "animals"),
            EmojiEntry("🐱", "cat", "animals"),
            EmojiEntry("🐭", "mouse", "animals"),
            EmojiEntry("🐰", "rabbit", "animals"),
            EmojiEntry("🦊", "fox", "animals"),
            EmojiEntry("🐻", "bear", "animals"),
            EmojiEntry("🐼", "panda", "animals"),
            EmojiEntry("🐨", "koala", "animals"),
            EmojiEntry("🐯", "tiger", "animals"),
            EmojiEntry("🦁", "lion", "animals")
        )),
        EmojiCategory("food", "食物", "🍎", listOf(
            EmojiEntry("🍎", "apple", "food"),
            EmojiEntry("🍊", "orange", "food"),
            EmojiEntry("🍋", "lemon", "food"),
            EmojiEntry("🍌", "banana", "food"),
            EmojiEntry("🍉", "watermelon", "food"),
            EmojiEntry("🍇", "grapes", "food"),
            EmojiEntry("🍓", "strawberry", "food"),
            EmojiEntry("🫐", "blueberry", "food"),
            EmojiEntry("🍈", "melon", "food"),
            EmojiEntry("🍒", "cherry", "food")
        )),
        EmojiCategory("objects", "物体", "💡", listOf(
            EmojiEntry("💡", "lightbulb", "objects"),
            EmojiEntry("📱", "phone", "objects"),
            EmojiEntry("💻", "laptop", "objects"),
            EmojiEntry("⌨️", "keyboard", "objects"),
            EmojiEntry("🖥️", "desktop", "objects"),
            EmojiEntry("🖨️", "printer", "objects"),
            EmojiEntry("📷", "camera", "objects"),
            EmojiEntry("🎮", "game", "objects"),
            EmojiEntry("🎧", "headphones", "objects"),
            EmojiEntry("🎵", "music", "objects")
        )),
        EmojiCategory("symbols", "符号", "❤️", listOf(
            EmojiEntry("❤️", "red heart", "symbols"),
            EmojiEntry("🧡", "orange heart", "symbols"),
            EmojiEntry("💛", "yellow heart", "symbols"),
            EmojiEntry("💚", "green heart", "symbols"),
            EmojiEntry("💙", "blue heart", "symbols"),
            EmojiEntry("💜", "purple heart", "symbols"),
            EmojiEntry("⭐", "star", "symbols"),
            EmojiEntry("🌟", "glowing star", "symbols"),
            EmojiEntry("✨", "sparkles", "symbols"),
            EmojiEntry("🔥", "fire", "symbols")
        ))
    )

    fun getCategories(): List<EmojiCategory> = categories
    fun getByCategory(categoryId: String): List<EmojiEntry> {
        return categories.find { it.id == categoryId }?.emojis ?: emptyList()
    }
    fun search(query: String): List<EmojiEntry> {
        return categories.flatMap { it.emojis }
            .filter { it.name.contains(query, ignoreCase = true) || it.keywords.any { k -> k.contains(query, ignoreCase = true) } }
    }
}
