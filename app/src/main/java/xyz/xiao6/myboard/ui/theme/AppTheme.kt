package xyz.xiao6.myboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import xyz.xiao6.myboard.R

private val AppFontFamily =
    FontFamily(
        Font(R.font.noto_sans_regular, FontWeight.Normal),
        Font(R.font.noto_sans_bold, FontWeight.Bold),
    )

private fun withAppFont(style: TextStyle): TextStyle = style.copy(fontFamily = AppFontFamily)

private val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = withAppFont(base.displayLarge),
        displayMedium = withAppFont(base.displayMedium),
        displaySmall = withAppFont(base.displaySmall),
        headlineLarge = withAppFont(base.headlineLarge),
        headlineMedium = withAppFont(base.headlineMedium),
        headlineSmall = withAppFont(base.headlineSmall),
        titleLarge = withAppFont(base.titleLarge),
        titleMedium = withAppFont(base.titleMedium),
        titleSmall = withAppFont(base.titleSmall),
        bodyLarge = withAppFont(base.bodyLarge),
        bodyMedium = withAppFont(base.bodyMedium),
        bodySmall = withAppFont(base.bodySmall),
        labelLarge = withAppFont(base.labelLarge),
        labelMedium = withAppFont(base.labelMedium),
        labelSmall = withAppFont(base.labelSmall),
    )
}

@Composable
fun MyBoardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = AppTypography,
        content = content,
    )
}
