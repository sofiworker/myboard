package xyz.xiao6.myboard.core.theme

import android.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.xiao6.myboard.core.contract.FeedbackPolicy
import xyz.xiao6.myboard.core.contract.HapticToken
import xyz.xiao6.myboard.core.contract.KeyStyle
import xyz.xiao6.myboard.core.contract.SoundToken
import xyz.xiao6.myboard.core.contract.ThemeResolver

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
            background = parseColor(colors.keyDefault),
            pressedBackground = parseColor(colors.keyPressed),
            textColor = parseColor(colors.keyText),
            pressedTextColor = parseColor(colors.keyText),
            fontSize = 18f,
            cornerRadius = 8f,
            iconTint = parseColor(colors.keyText)
        )
        
        if (styleDef == null) {
            // 回退到 key_default
            if (styleRef != "key_default") {
                return resolveKeyStyle("key_default")
            }
            return defaultStyle
        }
        
        // 合并 styleDef 和默认值
        return KeyStyle(
            background = styleDef.background?.let { parseColor(it) } ?: defaultStyle.background,
            pressedBackground = styleDef.pressedBackground?.let { parseColor(it) } ?: defaultStyle.pressedBackground,
            textColor = styleDef.textColor?.let { parseColor(it) } ?: defaultStyle.textColor,
            pressedTextColor = styleDef.pressedTextColor?.let { parseColor(it) } ?: defaultStyle.pressedTextColor,
            fontSize = styleDef.fontSize ?: defaultStyle.fontSize,
            cornerRadius = styleDef.cornerRadius ?: defaultStyle.cornerRadius,
            iconTint = styleDef.iconTint?.let { parseColor(it) } ?: defaultStyle.iconTint
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
    
    override fun isDark(): Boolean {
        return _doc.value.dark
    }
    
    private fun parseColor(colorStr: String): Long {
        return try {
            Color.parseColor(colorStr).toLong()
        } catch (_: Exception) {
            Color.WHITE.toLong()
        }
    }
}
