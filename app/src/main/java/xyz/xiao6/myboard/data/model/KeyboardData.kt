package xyz.xiao6.myboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class KeyboardData(
    val arrangement: List<KeyDataRow>,
    val toolbar: List<KeyData>? = null
)

@Serializable
data class KeyDataRow(
    val keys: List<KeyData>
)
