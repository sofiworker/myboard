package xyz.xiao6.myboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EmojiData(
    val categories: List<EmojiCategory>
)

@Serializable
data class EmojiCategory(
    val name: String,
    val emojis: List<String>
)
