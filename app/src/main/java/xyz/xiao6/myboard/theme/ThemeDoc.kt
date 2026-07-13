package xyz.xiao6.myboard.theme

import kotlinx.serialization.Serializable
import xyz.xiao6.myboard.common.SchemaVersion

/**
 * 主题文档数据模型。
 * 覆盖颜色、形状、字号、圆角、反馈参数。
 * 全部 @Serializable，支持 JSONC 解析。
 */
@Serializable
data class ThemeDoc(
    val schemaVersion: String = "1.0.0",
    val id: String,
    val name: String,
    val dark: Boolean = false,
    val colors: ThemeColors = ThemeColors(),
    val keyStyles: Map<String, KeyStyleDef> = emptyMap(),
    val feedback: FeedbackSection = FeedbackSection()
) {
    companion object {
        /**
         * 验证 ThemeDoc 的 schema 版本兼容性。
         * @return true 如果兼容，否则返回 false
         */
        fun validateSchemaVersion(doc: ThemeDoc): Boolean {
            return SchemaVersion.isCompatible(doc.schemaVersion)
        }
    }
}

/**
 * 主题颜色 token。
 * 使用字符串表达颜色（ARGB hex 格式）。
 */
@Serializable
data class ThemeColors(
    val background: String = "#F1F3F4",
    val surface: String = "#FFFFFF",
    val keyDefault: String = "#FFFFFF",
    val keyPressed: String = "#E8EAED",
    val keyText: String = "#202124",
    val keyHint: String = "#8E8E93",
    val keyFunction: String = "#E8EAED",
    val keyFunctionPressed: String = "#DADCE0",
    val keyFunctionText: String = "#202124",
    val keyAction: String = "#E8EAED",
    val keyActionPressed: String = "#DADCE0",
    val keyActionText: String = "#202124",
    val candidateBackground: String = "#FFFFFF",
    val candidateText: String = "#202124",
    val candidateHighlight: String = "#1A73E8"
)

/**
 * 按键样式定义。
 */
@Serializable
data class KeyStyleDef(
    val background: String? = null,
    val pressedBackground: String? = null,
    val textColor: String? = null,
    val pressedTextColor: String? = null,
    val fontSize: Float? = null,
    val cornerRadius: Float? = null,
    val iconTint: String? = null,
    val decorated: Boolean? = null
)

/**
 * 反馈参数段。
 */
@Serializable
data class FeedbackSection(
    val haptic: Map<String, HapticTokenDef> = emptyMap(),
    val sound: Map<String, SoundTokenDef> = emptyMap()
)

/**
 * 触觉反馈 token 定义。
 */
@Serializable
data class HapticTokenDef(
    val durationMs: Long,
    val amplitude: Int = 128,
    val fallbackVibration: Boolean = true
)

/**
 * 声音反馈 token 定义。
 */
@Serializable
data class SoundTokenDef(
    val soundResName: String,
    val volume: Float = 1.0f
)
