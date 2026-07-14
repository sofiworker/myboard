package xyz.xiao6.myboard.theme.skin

import xyz.xiao6.myboard.theme.FeedbackSection
import xyz.xiao6.myboard.theme.HapticTokenDef
import xyz.xiao6.myboard.theme.KeyStyleDef
import xyz.xiao6.myboard.theme.SoundTokenDef
import xyz.xiao6.myboard.theme.ThemeColors
import xyz.xiao6.myboard.theme.ThemeDoc
import xyz.xiao6.myboard.theme.foundation.FeedbackTokenId
import xyz.xiao6.myboard.theme.foundation.KeyStyleRole
import xyz.xiao6.myboard.theme.foundation.ThemeVariant

/**
 * 「极简赤点」纯 token locked 皮肤。
 * 对齐 docs/superpowers/specs/2026-07-14-pure-flat-minimal-skin-design.md
 * 与参考图 IM/mmexportab337216fcfc110be420fe1f470dfb28_1783862774029.jpeg
 */
object PureFlatSkin {
    val meta = SkinThemeMeta(
        id = SkinThemeId.PURE_FLAT,
        colorPolicy = SkinColorPolicy.LOCKED,
        usesImages = false,
        usesDecorations = false,
        usesLayoutBindings = false
    )

    private const val CORNER_DEFAULT = 16f
    private const val CORNER_SPACE = 18f
    private const val CORNER_CANDIDATE = 8f

    fun themeDoc(variant: ThemeVariant): ThemeDoc {
        val dark = variant == ThemeVariant.DARK
        val colors = if (dark) darkColors() else lightColors()
        return ThemeDoc(
            id = SkinThemeId.PURE_FLAT.id,
            name = SkinThemeId.PURE_FLAT.id,
            dark = dark,
            colors = colors,
            keyStyles = keyStyles(colors),
            feedback = feedback()
        )
    }

    private fun lightColors(): ThemeColors = ThemeColors(
        background = "#FFFFFF",
        surface = "#FFFFFF",
        keyDefault = "#F0F0F0",
        keyPressed = "#E2E2E2",
        keyText = "#1A1A1A",
        keyHint = "#8A8A8A",
        keyFunction = "#F0F0F0",
        keyFunctionPressed = "#E2E2E2",
        keyFunctionText = "#1A1A1A",
        keyAction = "#FF3B30",
        keyActionPressed = "#E0352B",
        keyActionText = "#FFFFFF",
        candidateBackground = "#FFFFFF",
        candidateText = "#1A1A1A",
        candidateHighlight = "#FF3B30"
    )

    private fun darkColors(): ThemeColors = ThemeColors(
        background = "#121212",
        surface = "#121212",
        keyDefault = "#2A2A2A",
        keyPressed = "#3A3A3A",
        keyText = "#F5F5F5",
        keyHint = "#9A9A9A",
        keyFunction = "#2A2A2A",
        keyFunctionPressed = "#3A3A3A",
        keyFunctionText = "#F5F5F5",
        keyAction = "#FF3B30",
        keyActionPressed = "#E0352B",
        keyActionText = "#FFFFFF",
        candidateBackground = "#121212",
        candidateText = "#F5F5F5",
        candidateHighlight = "#FF3B30"
    )

    private fun keyStyles(colors: ThemeColors): Map<String, KeyStyleDef> = mapOf(
        KeyStyleRole.DEFAULT.ref to KeyStyleDef(
            background = colors.keyDefault,
            pressedBackground = colors.keyPressed,
            textColor = colors.keyText,
            pressedTextColor = colors.keyText,
            fontSize = 18f,
            cornerRadius = CORNER_DEFAULT,
            iconTint = colors.keyText,
            decorated = true
        ),
        KeyStyleRole.FUNCTION.ref to KeyStyleDef(
            background = colors.keyFunction,
            pressedBackground = colors.keyFunctionPressed,
            textColor = colors.keyFunctionText,
            pressedTextColor = colors.keyFunctionText,
            fontSize = 14f,
            cornerRadius = CORNER_DEFAULT,
            iconTint = colors.keyFunctionText,
            decorated = true
        ),
        KeyStyleRole.ACTION.ref to KeyStyleDef(
            background = colors.keyAction,
            pressedBackground = colors.keyActionPressed,
            textColor = colors.keyActionText,
            pressedTextColor = colors.keyActionText,
            fontSize = 16f,
            cornerRadius = CORNER_DEFAULT,
            iconTint = colors.keyActionText,
            decorated = true
        ),
        KeyStyleRole.SPACE.ref to KeyStyleDef(
            background = colors.keyDefault,
            pressedBackground = colors.keyPressed,
            textColor = colors.keyText,
            pressedTextColor = colors.keyText,
            fontSize = 14f,
            cornerRadius = CORNER_SPACE,
            iconTint = colors.keyText,
            decorated = true
        ),
        KeyStyleRole.CANDIDATE.ref to KeyStyleDef(
            background = colors.candidateBackground,
            pressedBackground = colors.keyPressed,
            textColor = colors.candidateText,
            pressedTextColor = colors.candidateHighlight,
            fontSize = 16f,
            cornerRadius = CORNER_CANDIDATE,
            iconTint = colors.candidateText,
            decorated = true
        )
    )

    private fun feedback(): FeedbackSection = FeedbackSection(
        haptic = mapOf(
            FeedbackTokenId.KEY_TAP.ref to HapticTokenDef(durationMs = 10, amplitude = 48),
            FeedbackTokenId.KEY_LONG_PRESS.ref to HapticTokenDef(durationMs = 28, amplitude = 96),
            FeedbackTokenId.KEY_ACTION.ref to HapticTokenDef(durationMs = 14, amplitude = 80)
        ),
        sound = mapOf(
            FeedbackTokenId.KEY_TAP.ref to SoundTokenDef(
                soundResName = FeedbackTokenId.KEY_TAP.soundResName,
                volume = 0.25f
            ),
            FeedbackTokenId.KEY_ACTION.ref to SoundTokenDef(
                soundResName = FeedbackTokenId.KEY_ACTION.soundResName,
                volume = 0.4f
            ),
            FeedbackTokenId.KEY_SPACE.ref to SoundTokenDef(
                soundResName = FeedbackTokenId.KEY_SPACE.soundResName,
                volume = 0.18f
            )
        )
    )
}
