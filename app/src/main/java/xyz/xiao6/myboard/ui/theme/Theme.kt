package xyz.xiao6.myboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import xyz.xiao6.myboard.data.model.theme.SerializableColor
import xyz.xiao6.myboard.data.model.theme.ThemeData

val DefaultThemeData = ThemeData(
    name = "Default",
    keyboardBackground = SerializableColor(Color(0xFFF2F2F4)),
    keyBackground = SerializableColor(Color(0xFFFFFFFF)),
    keyForeground = SerializableColor(Color(0xFF1F1F1F)),
    suggestionsBackground = SerializableColor(Color(0xFFF6F6F8)),
    suggestionsForeground = SerializableColor(Color(0xFF1F1F1F))
)

@Composable
fun MyboardTheme(
    themeData: ThemeData?,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTheme provides (themeData ?: DefaultThemeData)) {
        content()
    }
}
