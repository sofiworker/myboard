package xyz.xiao6.myboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import xyz.xiao6.myboard.data.model.theme.ThemeData

@Composable
fun MyboardTheme(
    themeData: ThemeData?,
    content: @Composable () -> Unit
) {
    if (themeData != null) {
        CompositionLocalProvider(LocalTheme provides themeData) {
            content()
        }
    }
}
