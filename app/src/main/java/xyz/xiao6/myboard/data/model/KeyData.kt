package xyz.xiao6.myboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class KeyData(
    val type: KeyType,
    val label: String,
    val action: KeyAction,
    val weight: Float = 1f,
    val more: List<KeyData> = emptyList()
)
