package xyz.xiao6.myboard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
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

// Modern Light Color Scheme - Material You inspired
private val ModernLightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF3B82F6),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE0E7FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF1E3A8A),
    secondary = androidx.compose.ui.graphics.Color(0xFF8B5CF6),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFEDE9FE),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF5B21B6),
    tertiary = androidx.compose.ui.graphics.Color(0xFF14B8A6),
    onTertiary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFCCFBF1),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFF134E4A),
    background = androidx.compose.ui.graphics.Color(0xFFF0F4F8),
    onBackground = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1A1C1E),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6B7280),
    surfaceTint = androidx.compose.ui.graphics.Color(0xFF3B82F6),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFF0F172A),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
    error = androidx.compose.ui.graphics.Color(0xFFEF4444),
    onError = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFEE2E2),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFF991B1B),
    outline = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
)

// Modern Dark Color Scheme - Material You inspired
private val ModernDarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF60A5FA),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF1E3A8A),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF2563EB),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE0E7FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFA78BFA),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF5B21B6),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF7C3AED),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFEDE9FE),
    tertiary = androidx.compose.ui.graphics.Color(0xFF5EEAD4),
    onTertiary = androidx.compose.ui.graphics.Color(0xFF0F766E),
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF14B8A6),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFCCFBF1),
    background = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
    surface = androidx.compose.ui.graphics.Color(0xFF1E293B),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF334155),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF94A3B8),
    surfaceTint = androidx.compose.ui.graphics.Color(0xFF60A5FA),
    inverseSurface = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFF0F172A),
    error = androidx.compose.ui.graphics.Color(0xFFF87171),
    onError = androidx.compose.ui.graphics.Color(0xFF7F1D1D),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFB91C1C),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFEE2E2),
    outline = androidx.compose.ui.graphics.Color(0xFF334155),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF475569),
    scrim = androidx.compose.ui.graphics.Color(0xFF000000),
)

@Composable
fun MyBoardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ModernDarkColorScheme
        else -> ModernLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
