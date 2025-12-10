package xyz.xiao6.myboard.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class KeyType {
    CHARACTER,
    MODIFIER,
    ACTION
}
