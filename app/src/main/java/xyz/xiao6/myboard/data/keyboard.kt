package xyz.xiao6.myboard.data

import kotlinx.serialization.Serializable
import xyz.xiao6.myboard.data.model.theme.SerializableColor
import java.util.UUID

@Serializable
data class KeyboardData(
    val type: String,
    val name: String,
    val label: String,
    val direction: String,
    val authors: List<String>,
    val arrangement: List<RowData>,
    val toolbar: List<KeyData>? = null
)

@Serializable
data class RowData(
    val row: List<KeyArrangement>,
    val id: String = UUID.randomUUID().toString()
)

@Serializable
data class KeyArrangement(
    val width: Float,
    val keys: List<KeyData>,
    val id: String = UUID.randomUUID().toString()
)

@Serializable
data class KeyData(
    val type: String,
    val value: String,
    val label: String? = null,
    val id: String = UUID.randomUUID().toString(),
    val overrideBackgroundColor: SerializableColor? = null,
    val overrideForegroundColor: SerializableColor? = null,
    val popup: List<KeyData>? = null
)
