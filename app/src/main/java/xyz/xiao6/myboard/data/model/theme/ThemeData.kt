package xyz.xiao6.myboard.data.model.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class ThemeData(
    @Serializable(with = ColorSerializer::class)
    val keyboardBackground: Color,
    @Serializable(with = ColorSerializer::class)
    val keyBackground: Color,
    @Serializable(with = ColorSerializer::class)
    val keyForeground: Color,
    @Serializable(with = ColorSerializer::class)
    val suggestionsBackground: Color,
    @Serializable(with = ColorSerializer::class)
    val suggestionsForeground: Color,
)
