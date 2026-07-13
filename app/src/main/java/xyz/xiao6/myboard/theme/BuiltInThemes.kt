package xyz.xiao6.myboard.theme

import xyz.xiao6.myboard.theme.foundation.FeedbackTokenId
import xyz.xiao6.myboard.theme.foundation.KeyStyleRole

/**
 * 内置主题常量。
 * 对应 assets/themes/ 下的 JSONC 文件。
 */
object BuiltInThemes {
    
    val light = ThemeDoc(
        id = "default_light",
        name = "Default Light",
        dark = false,
        colors = ThemeColors(
            background = "#F1F3F4",
            surface = "#FFFFFF",
            keyDefault = "#FFFFFF",
            keyPressed = "#E8EAED",
            keyText = "#202124",
            keyHint = "#8E8E93",
            keyFunction = "#E8EAED",
            keyFunctionPressed = "#DADCE0",
            keyFunctionText = "#202124",
            keyAction = "#E8EAED",
            keyActionPressed = "#DADCE0",
            keyActionText = "#202124",
            candidateBackground = "#FFFFFF",
            candidateText = "#202124",
            candidateHighlight = "#1A73E8"
        ),
        keyStyles = mapOf(
            KeyStyleRole.DEFAULT.ref to KeyStyleDef(
                background = "#FFFFFF",
                pressedBackground = "#E8EAED",
                textColor = "#202124",
                pressedTextColor = "#202124",
                fontSize = 18f,
                cornerRadius = 10f,
                iconTint = "#202124"
            ),
            KeyStyleRole.FUNCTION.ref to KeyStyleDef(
                background = "#E8EAED",
                pressedBackground = "#DADCE0",
                textColor = "#202124",
                pressedTextColor = "#202124",
                fontSize = 14f,
                cornerRadius = 10f,
                iconTint = "#202124"
            ),
            KeyStyleRole.ACTION.ref to KeyStyleDef(
                background = "#1A73E8",
                pressedBackground = "#1558B0",
                textColor = "#FFFFFF",
                pressedTextColor = "#FFFFFF",
                fontSize = 14f,
                cornerRadius = 10f,
                iconTint = "#FFFFFF"
            ),
            KeyStyleRole.SPACE.ref to KeyStyleDef(
                background = "#FFFFFF",
                pressedBackground = "#E8EAED",
                textColor = "#202124",
                pressedTextColor = "#202124",
                fontSize = 14f,
                cornerRadius = 20f,
                iconTint = "#202124"
            ),
            KeyStyleRole.CANDIDATE.ref to KeyStyleDef(
                background = "#FFFFFF",
                pressedBackground = "#E8EAED",
                textColor = "#202124",
                pressedTextColor = "#1A73E8",
                fontSize = 16f,
                cornerRadius = 4f,
                iconTint = "#202124"
            )
        ),
        feedback = FeedbackSection(
            haptic = mapOf(
                FeedbackTokenId.KEY_TAP.ref to HapticTokenDef(durationMs = 10, amplitude = 64),
                FeedbackTokenId.KEY_LONG_PRESS.ref to HapticTokenDef(durationMs = 30, amplitude = 128),
                FeedbackTokenId.KEY_ACTION.ref to HapticTokenDef(durationMs = 15, amplitude = 96)
            ),
            sound = mapOf(
                FeedbackTokenId.KEY_TAP.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_TAP.soundResName, volume = 0.3f),
                FeedbackTokenId.KEY_ACTION.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_ACTION.soundResName, volume = 0.5f),
                FeedbackTokenId.KEY_SPACE.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_SPACE.soundResName, volume = 0.2f)
            )
        )
    )
    
    val dark = ThemeDoc(
        id = "default_dark",
        name = "Default Dark",
        dark = true,
        colors = ThemeColors(
            background = "#1E1E1E",
            surface = "#2D2D2D",
            keyDefault = "#3C3C3C",
            keyPressed = "#4A4A4A",
            keyText = "#E8EAED",
            keyHint = "#8E8E93",
            keyFunction = "#3C3C3C",
            keyFunctionPressed = "#4A4A4A",
            keyFunctionText = "#E8EAED",
            keyAction = "#3C3C3C",
            keyActionPressed = "#4A4A4A",
            keyActionText = "#E8EAED",
            candidateBackground = "#2D2D2D",
            candidateText = "#E8EAED",
            candidateHighlight = "#8AB4F8"
        ),
        keyStyles = mapOf(
            KeyStyleRole.DEFAULT.ref to KeyStyleDef(
                background = "#3C3C3C",
                pressedBackground = "#4A4A4A",
                textColor = "#E8EAED",
                pressedTextColor = "#E8EAED",
                fontSize = 18f,
                cornerRadius = 10f,
                iconTint = "#E8EAED"
            ),
            KeyStyleRole.FUNCTION.ref to KeyStyleDef(
                background = "#3C3C3C",
                pressedBackground = "#4A4A4A",
                textColor = "#E8EAED",
                pressedTextColor = "#E8EAED",
                fontSize = 14f,
                cornerRadius = 10f,
                iconTint = "#E8EAED"
            ),
            KeyStyleRole.ACTION.ref to KeyStyleDef(
                background = "#8AB4F8",
                pressedBackground = "#669DF6",
                textColor = "#1E1E1E",
                pressedTextColor = "#1E1E1E",
                fontSize = 14f,
                cornerRadius = 10f,
                iconTint = "#1E1E1E"
            ),
            KeyStyleRole.SPACE.ref to KeyStyleDef(
                background = "#3C3C3C",
                pressedBackground = "#4A4A4A",
                textColor = "#E8EAED",
                pressedTextColor = "#E8EAED",
                fontSize = 14f,
                cornerRadius = 20f,
                iconTint = "#E8EAED"
            ),
            KeyStyleRole.CANDIDATE.ref to KeyStyleDef(
                background = "#2D2D2D",
                pressedBackground = "#3C3C3C",
                textColor = "#E8EAED",
                pressedTextColor = "#8AB4F8",
                fontSize = 16f,
                cornerRadius = 4f,
                iconTint = "#E8EAED"
            )
        ),
        feedback = FeedbackSection(
            haptic = mapOf(
                FeedbackTokenId.KEY_TAP.ref to HapticTokenDef(durationMs = 10, amplitude = 64),
                FeedbackTokenId.KEY_LONG_PRESS.ref to HapticTokenDef(durationMs = 30, amplitude = 128),
                FeedbackTokenId.KEY_ACTION.ref to HapticTokenDef(durationMs = 15, amplitude = 96)
            ),
            sound = mapOf(
                FeedbackTokenId.KEY_TAP.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_TAP.soundResName, volume = 0.3f),
                FeedbackTokenId.KEY_ACTION.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_ACTION.soundResName, volume = 0.5f),
                FeedbackTokenId.KEY_SPACE.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_SPACE.soundResName, volume = 0.2f)
            )
        )
    )
    
    /** 所有内置主题 */
    val all: List<ThemeDoc> = listOf(light, dark)

    /** 解析失败时使用的兜底主题。 */
    val defaultFallback: ThemeDoc
        get() = light
    
    /** 根据 ID 查找 */
    fun byId(id: String): ThemeDoc? = all.find { it.id == id }
}
