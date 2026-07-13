package xyz.xiao6.myboard.theme.foundation

import xyz.xiao6.myboard.theme.FeedbackSection
import xyz.xiao6.myboard.theme.HapticTokenDef
import xyz.xiao6.myboard.theme.KeyStyleDef
import xyz.xiao6.myboard.theme.SoundTokenDef
import xyz.xiao6.myboard.theme.ThemeColors
import xyz.xiao6.myboard.theme.ThemeDoc

class ThemeColorGenerator {
    fun generate(
        selection: FoundationThemeSelection,
        variant: ThemeVariant,
        dynamicSeedColor: String? = null
    ): ThemeDoc {
        val palette = FoundationPalette.resolve(selection, dynamicSeedColor)
        val dark = variant == ThemeVariant.DARK
        val accent = if (dark) palette.darkAccent else palette.lightAccent
        val corner = cornerRadius(selection.cornerStyle)
        val highContrast = selection.keyContrast == KeyContrast.HIGH

        val colors = if (dark) {
            ThemeColors(
                background = "#1E1E1E",
                surface = "#2D2D2D",
                keyDefault = keyBackground(selection.keyTreatment, dark),
                keyPressed = "#4A4A4A",
                keyText = "#E8EAED",
                keyHint = if (highContrast) "#BDC1C6" else "#8E8E93",
                keyFunction = "#3C3C3C",
                keyFunctionPressed = "#4A4A4A",
                keyFunctionText = "#E8EAED",
                keyAction = accent,
                keyActionPressed = ThemeColorUtils.mix(accent, "#000000", 0.18f),
                keyActionText = "#1E1E1E",
                candidateBackground = "#2D2D2D",
                candidateText = "#E8EAED",
                candidateHighlight = accent
            )
        } else {
            ThemeColors(
                background = "#F1F3F4",
                surface = "#FFFFFF",
                keyDefault = keyBackground(selection.keyTreatment, dark),
                keyPressed = "#E8EAED",
                keyText = "#202124",
                keyHint = if (highContrast) "#5F6368" else "#8E8E93",
                keyFunction = "#E8EAED",
                keyFunctionPressed = "#DADCE0",
                keyFunctionText = "#202124",
                keyAction = accent,
                keyActionPressed = ThemeColorUtils.mix(accent, "#000000", 0.20f),
                keyActionText = "#FFFFFF",
                candidateBackground = "#FFFFFF",
                candidateText = "#202124",
                candidateHighlight = accent
            )
        }

        return ThemeDoc(
            id = "foundation_${palette.id.name.lowercase()}_${variant.name.lowercase()}",
            name = palette.titleKey,
            dark = dark,
            colors = colors,
            keyStyles = keyStyles(colors, selection.keyTreatment, corner),
            feedback = defaultFeedback()
        )
    }

    private fun keyBackground(treatment: KeyTreatment, dark: Boolean): String {
        return when (treatment) {
            KeyTreatment.FILLED -> if (dark) "#3C3C3C" else "#FFFFFF"
            KeyTreatment.OUTLINED -> if (dark) "#003C3C3C" else "#00FFFFFF"
            KeyTreatment.BORDERLESS -> "#00000000"
        }
    }

    private fun cornerRadius(style: CornerStyle): Float {
        return when (style) {
            CornerStyle.COMPACT -> 6f
            CornerStyle.ROUNDED -> 10f
            CornerStyle.PILL -> 20f
        }
    }

    private fun keyStyles(colors: ThemeColors, treatment: KeyTreatment, corner: Float): Map<String, KeyStyleDef> {
        val defaultCorner = if (treatment == KeyTreatment.BORDERLESS) 6f else corner
        return mapOf(
            KeyStyleRole.DEFAULT.ref to KeyStyleDef(
                background = colors.keyDefault,
                pressedBackground = colors.keyPressed,
                textColor = colors.keyText,
                pressedTextColor = colors.keyText,
                fontSize = 18f,
                cornerRadius = defaultCorner,
                iconTint = colors.keyText
            ),
            KeyStyleRole.FUNCTION.ref to KeyStyleDef(
                background = if (treatment == KeyTreatment.BORDERLESS) "#00000000" else colors.keyFunction,
                pressedBackground = colors.keyFunctionPressed,
                textColor = colors.keyFunctionText,
                pressedTextColor = colors.keyFunctionText,
                fontSize = 14f,
                cornerRadius = defaultCorner,
                iconTint = colors.keyFunctionText
            ),
            KeyStyleRole.ACTION.ref to KeyStyleDef(
                background = colors.keyAction,
                pressedBackground = colors.keyActionPressed,
                textColor = colors.keyActionText,
                pressedTextColor = colors.keyActionText,
                fontSize = 14f,
                cornerRadius = defaultCorner,
                iconTint = colors.keyActionText
            ),
            KeyStyleRole.SPACE.ref to KeyStyleDef(
                background = colors.keyDefault,
                pressedBackground = colors.keyPressed,
                textColor = colors.keyText,
                pressedTextColor = colors.keyText,
                fontSize = 14f,
                cornerRadius = (defaultCorner * 2f).coerceAtMost(24f),
                iconTint = colors.keyText
            ),
            KeyStyleRole.CANDIDATE.ref to KeyStyleDef(
                background = colors.candidateBackground,
                pressedBackground = colors.keyPressed,
                textColor = colors.candidateText,
                pressedTextColor = colors.candidateHighlight,
                fontSize = 16f,
                cornerRadius = 4f,
                iconTint = colors.candidateText
            )
        )
    }

    private fun defaultFeedback(): FeedbackSection =
        FeedbackSection(
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
}
