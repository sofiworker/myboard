package xyz.xiao6.myboard.theme.foundation

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object ThemeRuntimeMaterialColors {
    fun colorSchemeFor(runtime: ThemeRuntime): ColorScheme {
        val colors = runtime.doc.colors
        val primary = parseColor(colors.candidateHighlight)
        val background = parseColor(colors.background)
        val surface = parseColor(colors.surface)
        val onSurface = parseColor(colors.candidateText)
        return if (runtime.variant == ThemeVariant.DARK) {
            darkColorScheme(
                primary = primary,
                onPrimary = parseColor(colors.keyActionText),
                background = background,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = parseColor(colors.candidateBackground),
                onSurfaceVariant = onSurface,
                outline = parseColor(colors.keyHint),
                error = Color(0xFFBA1A1A)
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = parseColor(colors.keyActionText),
                background = background,
                onBackground = onSurface,
                surface = surface,
                onSurface = onSurface,
                surfaceVariant = parseColor(colors.candidateBackground),
                onSurfaceVariant = onSurface,
                outline = parseColor(colors.keyHint),
                error = Color(0xFFBA1A1A)
            )
        }
    }

    private fun parseColor(colorStr: String): Color {
        val raw = colorStr.trim().removePrefix("#")
        val argb = when (raw.length) {
            6 -> "FF$raw"
            8 -> raw
            else -> return Color.White
        }
        if (!argb.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return Color.White
        }
        return Color(argb.toLong(16).toInt())
    }
}
