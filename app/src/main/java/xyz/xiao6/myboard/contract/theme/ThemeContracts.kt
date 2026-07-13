package xyz.xiao6.myboard.contract.theme

import androidx.compose.ui.graphics.Color

/**
 * 按键样式。由 ThemeResolver 解析主题 token 产出。
 */
data class KeyStyle(
    val background: Color,
    val pressedBackground: Color,
    val textColor: Color,
    val pressedTextColor: Color,
    val fontSize: Float,
    val cornerRadius: Float,
    val iconTint: Color,
    val decorated: Boolean = true
)

/**
 * 反馈参数集合。由 ThemeResolver 从当前主题的 feedback 段解析产出。
 */
data class FeedbackPolicy(
    val hapticTokens: Map<String, HapticToken>,
    val soundTokens: Map<String, SoundToken>
)

/**
 * 触觉反馈 token。
 */
data class HapticToken(
    val id: String,
    val durationMs: Long,
    val amplitude: Int,         // [1, 255]
    val fallbackVibration: Boolean = true
)

/**
 * 声音反馈 token。
 */
data class SoundToken(
    val id: String,
    val soundResName: String,
    val volume: Float = 1.0f   // [0.0, 1.0]
)

/**
 * 键盘 chrome（背景 / 候选栏）颜色，从主题 token 解析。
 */
data class ChromeColors(
    val background: Color,
    val surface: Color,
    val candidateBackground: Color,
    val candidateText: Color,
    val candidateHighlight: Color,
    val keyHint: Color
)

/**
 * 主题解析器接口。
 * 阶段 03 实现真实逻辑。旧 ThemeResolver 类在阶段 03 删除后此接口移入 core.theme 包。
 */
interface ThemeResolver {
    fun resolveKeyStyle(styleRef: String): KeyStyle
    fun resolveFeedbackPolicy(): FeedbackPolicy
    fun resolveChromeColors(): ChromeColors
    fun isDark(): Boolean
}
