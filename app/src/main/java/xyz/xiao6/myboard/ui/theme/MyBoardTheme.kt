package xyz.xiao6.myboard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand-aligned palette (blue primary, neutral surfaces)
private val LightPrimary = Color(0xFF1A73E8)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFD3E3FD)
private val LightOnPrimaryContainer = Color(0xFF041E49)
private val LightSecondary = Color(0xFF5F6368)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFE8EAED)
private val LightOnSecondaryContainer = Color(0xFF202124)
private val LightTertiary = Color(0xFF018786)
private val LightBackground = Color(0xFFF8F9FA)
private val LightOnBackground = Color(0xFF202124)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF202124)
private val LightSurfaceVariant = Color(0xFFE8EAED)
private val LightOnSurfaceVariant = Color(0xFF5F6368)
private val LightOutline = Color(0xFFDADCE0)
private val LightError = Color(0xFFD93025)

private val DarkPrimary = Color(0xFF8AB4F8)
private val DarkOnPrimary = Color(0xFF0B1F3A)
private val DarkPrimaryContainer = Color(0xFF174EA6)
private val DarkOnPrimaryContainer = Color(0xFFD3E3FD)
private val DarkSecondary = Color(0xFF9AA0A6)
private val DarkOnSecondary = Color(0xFF202124)
private val DarkSecondaryContainer = Color(0xFF3C4043)
private val DarkOnSecondaryContainer = Color(0xFFE8EAED)
private val DarkTertiary = Color(0xFF03DAC5)
private val DarkBackground = Color(0xFF121212)
private val DarkOnBackground = Color(0xFFE8EAED)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkOnSurface = Color(0xFFE8EAED)
private val DarkSurfaceVariant = Color(0xFF2D2D2D)
private val DarkOnSurfaceVariant = Color(0xFF9AA0A6)
private val DarkOutline = Color(0xFF3C4043)
private val DarkError = Color(0xFFF28B82)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError
)

@Composable
fun MyBoardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
