package xyz.xiao6.myboard.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.xiao6.myboard.contract.theme.ChromeColors
import xyz.xiao6.myboard.contract.theme.FeedbackPolicy
import xyz.xiao6.myboard.contract.theme.HapticToken
import xyz.xiao6.myboard.contract.theme.KeyStyle
import xyz.xiao6.myboard.contract.theme.SoundToken
import xyz.xiao6.myboard.contract.theme.ThemeResolver
import xyz.xiao6.myboard.theme.foundation.KeyStyleRole

/**
 * 主题解析器真实实现。
 * 消费 ThemeDoc，解析 styleRef 到 KeyStyle，提供反馈参数。
 */
class ThemeResolverImpl(
    initialDoc: ThemeDoc
) : ThemeResolver {

    private val _doc = MutableStateFlow(initialDoc)
    val doc: StateFlow<ThemeDoc> = _doc.asStateFlow()

    /** 切换主题 */
    fun setTheme(newDoc: ThemeDoc) {
        _doc.value = newDoc
    }

    override fun resolveKeyStyle(styleRef: String): KeyStyle {
        val currentDoc = _doc.value
        val colors = currentDoc.colors

        // 先查 keyStyles 中是否有定义
        val styleDef = currentDoc.keyStyles[styleRef]

        // 构建默认 KeyStyle
        val defaultStyle = KeyStyle(
            background = parseColorToCompose(colors.keyDefault),
            pressedBackground = parseColorToCompose(colors.keyPressed),
            textColor = parseColorToCompose(colors.keyText),
            pressedTextColor = parseColorToCompose(colors.keyText),
            fontSize = 18f,
            cornerRadius = 8f,
            iconTint = parseColorToCompose(colors.keyText)
        )

        if (styleDef == null) {
            // 回退到 key_default
            if (styleRef != KeyStyleRole.DEFAULT.ref) {
                return resolveKeyStyle(KeyStyleRole.DEFAULT.ref)
            }
            return defaultStyle
        }

        // 合并 styleDef 和默认值
        return KeyStyle(
            background = styleDef.background?.let { parseColorToCompose(it) } ?: defaultStyle.background,
            pressedBackground = styleDef.pressedBackground?.let { parseColorToCompose(it) } ?: defaultStyle.pressedBackground,
            textColor = styleDef.textColor?.let { parseColorToCompose(it) } ?: defaultStyle.textColor,
            pressedTextColor = styleDef.pressedTextColor?.let { parseColorToCompose(it) } ?: defaultStyle.pressedTextColor,
            fontSize = styleDef.fontSize ?: defaultStyle.fontSize,
            cornerRadius = styleDef.cornerRadius ?: defaultStyle.cornerRadius,
            iconTint = styleDef.iconTint?.let { parseColorToCompose(it) } ?: defaultStyle.iconTint
        )
    }

    override fun resolveFeedbackPolicy(): FeedbackPolicy {
        val currentDoc = _doc.value
        val feedback = currentDoc.feedback

        val hapticTokens = feedback.haptic.mapValues { (id, def) ->
            HapticToken(
                id = id,
                durationMs = def.durationMs,
                amplitude = def.amplitude.coerceIn(1, 255),
                fallbackVibration = def.fallbackVibration
            )
        }

        val soundTokens = feedback.sound.mapValues { (id, def) ->
            SoundToken(
                id = id,
                soundResName = def.soundResName,
                volume = def.volume.coerceIn(0f, 1f)
            )
        }

        return FeedbackPolicy(
            hapticTokens = hapticTokens,
            soundTokens = soundTokens
        )
    }

    override fun resolveChromeColors(): ChromeColors {
        val colors = _doc.value.colors
        return ChromeColors(
            background = parseColorToCompose(colors.background),
            surface = parseColorToCompose(colors.surface),
            candidateBackground = parseColorToCompose(colors.candidateBackground),
            candidateText = parseColorToCompose(colors.candidateText),
            candidateHighlight = parseColorToCompose(colors.candidateHighlight),
            keyHint = parseColorToCompose(colors.keyHint)
        )
    }

    override fun isDark(): Boolean {
        return _doc.value.dark
    }

    private fun parseColorToCompose(colorStr: String): androidx.compose.ui.graphics.Color {
        val raw = colorStr.trim().removePrefix("#")
        val argb = when (raw.length) {
            6 -> "FF$raw"
            8 -> raw
            else -> return androidx.compose.ui.graphics.Color.White
        }
        if (!argb.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return androidx.compose.ui.graphics.Color.White
        }
        return androidx.compose.ui.graphics.Color(argb.toLong(16).toInt())
    }
}
