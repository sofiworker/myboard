package xyz.xiao6.myboard.data.model

import kotlinx.serialization.Serializable

@Serializable
data class KeyboardData(
    val rows: List<List<KeyData>>,
    val toolbar: List<KeyData> = emptyList()
)
