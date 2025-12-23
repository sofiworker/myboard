package xyz.xiao6.myboard.model

import android.content.Context
import android.content.res.Configuration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object ThemeParser {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    fun parseThemeSpec(text: String): ThemeSpec {
        return json.decodeFromString(ThemeSpec.serializer(), text)
    }

    fun parseThemeSpec(text: String, useDark: Boolean): ThemeSpec {
        val base = parseThemeSpec(text)
        if (!useDark) return base
        val overrides = base.dark ?: return base
        return applyOverrides(base, overrides)
    }

    fun isSystemDark(context: Context): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyOverrides(base: ThemeSpec, overrides: ThemeOverrides): ThemeSpec {
        return base.copy(
            colors = base.colors + overrides.colors,
            global = overrides.global?.let { mergeGlobal(base.global, it) } ?: base.global,
            icons = base.icons + overrides.icons,
            layout = overrides.layout?.let { mergeLayout(base.layout, it) } ?: base.layout,
            toolbar = overrides.toolbar?.let { mergeToolbar(base.toolbar, it) } ?: base.toolbar,
            candidates = overrides.candidates?.let { mergeCandidates(base.candidates, it) } ?: base.candidates,
            composingPopup = overrides.composingPopup?.let { mergePopup(base.composingPopup, it) } ?: base.composingPopup,
            keyPopup = overrides.keyPopup?.let { mergePopup(base.keyPopup, it) } ?: base.keyPopup,
            keyStyles = mergeKeyStyles(base.keyStyles, overrides.keyStyles),
            styleDefaults = overrides.styleDefaults?.let { mergeStyleDefaults(base.styleDefaults, it) } ?: base.styleDefaults,
        )
    }

    private fun mergeGlobal(base: GlobalTheme, override: GlobalTheme): GlobalTheme {
        return base.copy(
            font = mergeFont(base.font, override.font),
            fontSize = override.fontSize ?: base.fontSize,
            icon = override.icon ?: base.icon,
            labelCase = override.labelCase ?: base.labelCase,
            paint = mergePaint(base.paint, override.paint),
        )
    }

    private fun mergeFont(base: FontTheme, override: FontTheme): FontTheme {
        return base.copy(
            defaultFamily = override.defaultFamily ?: base.defaultFamily,
            families = base.families + override.families,
        )
    }

    private fun mergePaint(base: PaintTheme, override: PaintTheme): PaintTheme {
        return base.copy(
            antiAlias = override.antiAlias,
            cornerRadiusDp = override.cornerRadiusDp ?: base.cornerRadiusDp,
            stroke = override.stroke ?: base.stroke,
            shadow = override.shadow ?: base.shadow,
        )
    }

    private fun mergeLayout(base: LayoutTheme, override: LayoutTheme): LayoutTheme {
        return base.copy(
            background = override.background ?: base.background,
            extendToToolbar = override.extendToToolbar || base.extendToToolbar,
            extendToCandidates = override.extendToCandidates || base.extendToCandidates,
        )
    }

    private fun mergeToolbar(base: ToolbarTheme, override: ToolbarTheme): ToolbarTheme {
        return base.copy(
            surface = mergeSurface(base.surface, override.surface),
            itemText = mergeText(base.itemText, override.itemText),
            itemIcon = mergeIcon(base.itemIcon, override.itemIcon),
        )
    }

    private fun mergeCandidates(base: CandidateTheme, override: CandidateTheme): CandidateTheme {
        return base.copy(
            surface = mergeSurface(base.surface, override.surface),
            candidateText = mergeText(base.candidateText, override.candidateText),
            candidateTextSelected = mergeText(base.candidateTextSelected, override.candidateTextSelected),
            divider = override.divider ?: base.divider,
        )
    }

    private fun mergePopup(base: PopupTheme, override: PopupTheme): PopupTheme {
        return base.copy(
            surface = mergeSurface(base.surface, override.surface),
            text = mergeText(base.text, override.text),
            textSelected = mergeText(base.textSelected, override.textSelected),
            divider = override.divider ?: base.divider,
        )
    }

    private fun mergeSurface(base: SurfaceStyle, override: SurfaceStyle): SurfaceStyle {
        return base.copy(
            background = override.background ?: base.background,
            stroke = override.stroke ?: base.stroke,
            shadow = override.shadow ?: base.shadow,
            padding = override.padding ?: base.padding,
            cornerRadiusDp = override.cornerRadiusDp ?: base.cornerRadiusDp,
        )
    }

    private fun mergeText(base: TextStyle, override: TextStyle): TextStyle {
        return base.copy(
            visible = override.visible,
            color = override.color ?: base.color,
            sizeSp = override.sizeSp ?: base.sizeSp,
            bold = override.bold ?: base.bold,
            italic = override.italic ?: base.italic,
            underline = override.underline ?: base.underline,
            fontFamily = override.fontFamily ?: base.fontFamily,
            case = override.case ?: base.case,
            stroke = override.stroke ?: base.stroke,
            shadow = override.shadow ?: base.shadow,
            letterSpacingEm = override.letterSpacingEm ?: base.letterSpacingEm,
            maxLines = override.maxLines ?: base.maxLines,
            ellipsize = override.ellipsize ?: base.ellipsize,
        )
    }

    private fun mergeIcon(base: IconStyle, override: IconStyle): IconStyle {
        return base.copy(
            visible = override.visible,
            tint = override.tint ?: base.tint,
            sizeDp = override.sizeDp ?: base.sizeDp,
            alpha = override.alpha ?: base.alpha,
            scaleMode = override.scaleMode ?: base.scaleMode,
            paddingDp = override.paddingDp ?: base.paddingDp,
        )
    }

    private fun mergeKeyStyles(
        base: Map<String, KeyStyle>,
        overrides: Map<String, KeyStyle>,
    ): Map<String, KeyStyle> {
        if (overrides.isEmpty()) return base
        val out = LinkedHashMap<String, KeyStyle>(base.size + overrides.size)
        for ((k, v) in base) {
            out[k] = v
        }
        for ((k, ov) in overrides) {
            val existing = out[k]
            out[k] = if (existing == null) ov else mergeKeyStyle(existing, ov)
        }
        return out
    }

    private fun mergeKeyStyle(base: KeyStyle, override: KeyStyle): KeyStyle {
        return base.copy(
            background = override.background ?: base.background,
            backgroundPressed = override.backgroundPressed ?: base.backgroundPressed,
            backgroundDisabled = override.backgroundDisabled ?: base.backgroundDisabled,
            stroke = override.stroke ?: base.stroke,
            strokePressed = override.strokePressed ?: base.strokePressed,
            shadow = override.shadow ?: base.shadow,
            padding = override.padding ?: base.padding,
            cornerRadiusDp = override.cornerRadiusDp ?: base.cornerRadiusDp,
            icon = override.icon?.let { mergeIcon(base.icon ?: IconStyle(), it) } ?: base.icon,
            label = override.label?.let { mergeText(base.label ?: TextStyle(), it) } ?: base.label,
            hint = override.hint?.let { mergeText(base.hint ?: TextStyle(), it) } ?: base.hint,
            keyBackground = override.keyBackground ?: base.keyBackground,
            textColor = override.textColor ?: base.textColor,
            textSizeSp = override.textSizeSp ?: base.textSizeSp,
        )
    }

    private fun mergeStyleDefaults(base: StyleDefaults?, override: StyleDefaults): StyleDefaults {
        val safeBase = base ?: StyleDefaults()
        return safeBase.copy(
            normal = override.normal ?: safeBase.normal,
            function = override.function ?: safeBase.function,
            functionPrimary = override.functionPrimary ?: safeBase.functionPrimary,
            functionDanger = override.functionDanger ?: safeBase.functionDanger,
            functionToggle = override.functionToggle ?: safeBase.functionToggle,
        )
    }
}
