package xyz.xiao6.myboard.core.theme

import android.graphics.Color

/**
 * 主题数据模型。
 */
data class KeyboardTheme(
    val id: String,
    val name: String,
    val version: Int = 1,
    val type: String = "static",
    val colors: ThemeColors,
    val geometry: ThemeGeometry = ThemeGeometry(),
    val typography: ThemeTypography = ThemeTypography()
)

data class ThemeColors(
    val background: String = "#F1F3F4",
    val surface: String = "#FFFFFF",
    val key: KeyColors = KeyColors(),
    val action: ActionColors = ActionColors(),
    val candidate: CandidateColors = CandidateColors()
)

data class KeyColors(
    val normal: String = "#FFFFFF",
    val pressed: String = "#E8EAED",
    val text: String = "#202124",
    val hint: String = "#8E8E93"
)

data class ActionColors(
    val normal: String = "#E8EAED",
    val pressed: String = "#DADCE0",
    val text: String = "#202124"
)

data class CandidateColors(
    val background: String = "#FFFFFF",
    val text: String = "#202124",
    val highlight: String = "#1A73E8"
)

data class ThemeGeometry(
    val cornerRadius: Float = 8f,
    val keyHeightDp: Float = 46f,
    val gapHDp: Float = 4f,
    val gapVDp: Float = 5f
)

data class ThemeTypography(
    val keyLabelSizeSp: Float = 18f,
    val keyHintSizeSp: Float = 10f,
    val candidateSizeSp: Float = 16f
)

/**
 * 主题解析器。
 */
class ThemeResolver(private val theme: KeyboardTheme) {
    fun resolveKeyBackgroundColor(isPressed: Boolean): Int {
        val color = if (isPressed) theme.colors.key.pressed else theme.colors.key.normal
        return try { Color.parseColor(color) } catch (_: Exception) { Color.WHITE }
    }

    fun resolveKeyTextColor(): Int {
        return try { Color.parseColor(theme.colors.key.text) } catch (_: Exception) { Color.BLACK }
    }

    fun resolveActionBackgroundColor(isPressed: Boolean): Int {
        val color = if (isPressed) theme.colors.action.pressed else theme.colors.action.normal
        return try { Color.parseColor(color) } catch (_: Exception) { Color.parseColor("#E8EAED") }
    }

    fun resolveCandidateTextColor(): Int {
        return try { Color.parseColor(theme.colors.candidate.text) } catch (_: Exception) { Color.BLACK }
    }

    fun resolveCandidateHighlightColor(): Int {
        return try { Color.parseColor(theme.colors.candidate.highlight) } catch (_: Exception) { Color.parseColor("#1A73E8") }
    }

    fun resolveCornerRadius(): Float = theme.geometry.cornerRadius
    fun resolveKeyHeight(): Float = theme.geometry.keyHeightDp
}

/**
 * 内置主题。
 */
object BuiltInThemes {
    val default = KeyboardTheme(
        id = "default",
        name = "Default",
        colors = ThemeColors()
    )

    val dark = KeyboardTheme(
        id = "dark",
        name = "Dark",
        colors = ThemeColors(
            background = "#1F1F1F",
            surface = "#2D2D2D",
            key = KeyColors(normal = "#3C3C3C", pressed = "#4A4A4A", text = "#E8EAED", hint = "#8E8E93"),
            action = ActionColors(normal = "#3C3C3C", pressed = "#4A4A4A", text = "#E8EAED"),
            candidate = CandidateColors(background = "#2D2D2D", text = "#E8EAED", highlight = "#8AB4F8")
        )
    )

    val dracula = KeyboardTheme(
        id = "dracula",
        name = "Dracula",
        colors = ThemeColors(
            background = "#282A36",
            surface = "#44475A",
            key = KeyColors(normal = "#6272A4", pressed = "#BD93F9", text = "#F8F8F2", hint = "#6272A4"),
            action = ActionColors(normal = "#44475A", pressed = "#BD93F9", text = "#F8F8F2"),
            candidate = CandidateColors(background = "#282A36", text = "#F8F8F2", highlight = "#BD93F9")
        )
    )

    val nord = KeyboardTheme(
        id = "nord",
        name = "Nord",
        colors = ThemeColors(
            background = "#2E3440",
            surface = "#3B4252",
            key = KeyColors(normal = "#434C5E", pressed = "#4C566A", text = "#ECEFF4", hint = "#7B88A1"),
            action = ActionColors(normal = "#3B4252", pressed = "#434C5E", text = "#ECEFF4"),
            candidate = CandidateColors(background = "#2E3440", text = "#ECEFF4", highlight = "#88C0D0")
        )
    )

    val solarized = KeyboardTheme(
        id = "solarized",
        name = "Solarized",
        colors = ThemeColors(
            background = "#002B36",
            surface = "#073642",
            key = KeyColors(normal = "#586E75", pressed = "#657B83", text = "#EEE8D5", hint = "#839496"),
            action = ActionColors(normal = "#073642", pressed = "#586E75", text = "#EEE8D5"),
            candidate = CandidateColors(background = "#002B36", text = "#EEE8D5", highlight = "#268BD2")
        )
    )

    fun getAll(): List<KeyboardTheme> = listOf(default, dark, dracula, nord, solarized)
}
