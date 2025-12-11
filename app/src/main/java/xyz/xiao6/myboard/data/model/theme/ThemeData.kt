package xyz.xiao6.myboard.data.model.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class ThemeData(
    val name: String,
    val keyboardBackground: SerializableColor,
    val keyBackground: SerializableColor,
    val keyForeground: SerializableColor,
    val suggestionsBackground: SerializableColor,
    val suggestionsForeground: SerializableColor,
    val backgroundImageUri: String? = null,
    val backgroundAlpha: Float = 1.0f
)

@Serializable
data class SerializableColor(val red: Int, val green: Int, val blue: Int, val alpha: Int) {
    fun toColor(): Color = Color(red, green, blue, alpha)
}
