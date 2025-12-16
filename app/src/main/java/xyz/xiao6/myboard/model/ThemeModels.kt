package xyz.xiao6.myboard.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Theme schema (JSON) used by IME runtime.
 *
 * Design goals:
 * - Backwards-compatible with existing `assets/themes/theme_default.json`
 * - Split between global defaults and per-area overrides
 * - Provide per-style (styleId) key rendering configuration, and optional per-key overrides
 *
 * NOTE: Most values are raw strings and are resolved at runtime:
 * - Colors may be hex "#RRGGBB" / "#AARRGGBB" or references like "colors.key_text"
 * - Images are asset paths like "drawable/key_bg_alpha.9.png"
 */
@Serializable
data class ThemeSpec(
    val version: Int = 1,
    val themeId: String,
    val name: String? = null,
    /**
     * Named color tokens. Values may be hex or references to other tokens.
     */
    val colors: Map<String, String> = emptyMap(),
    /**
     * Global typography + common paint defaults.
     */
    val global: GlobalTheme = GlobalTheme(),
    /**
     * Keyboard layout surface (background behind keys).
     */
    val layout: LayoutTheme = LayoutTheme(),
    /**
     * Toolbar / top bar theme.
     */
    val toolbar: ToolbarTheme = ToolbarTheme(),
    /**
     * Candidate bar theme.
     */
    val candidates: CandidateTheme = CandidateTheme(),
    /**
     * Composing badge (candidate-left popup) theme.
     */
    val composingPopup: PopupTheme = PopupTheme(),
    /**
     * Long-press popup theme (key preview + candidates popup).
     */
    val keyPopup: PopupTheme = PopupTheme(),
    /**
     * Key style registry addressed by layout `Key.styleId`.
     */
    @SerialName("styles")
    val keyStyles: Map<String, KeyStyle> = emptyMap(),
    /**
     * Optional per-key overrides addressed by layout `Key.keyId`.
     * If present, it is applied on top of the resolved [keyStyles] style.
     */
    val keyOverrides: Map<String, KeyStyle> = emptyMap(),
)

@Serializable
data class GlobalTheme(
    val font: FontTheme = FontTheme(),
    val paint: PaintTheme = PaintTheme(),
)

@Serializable
data class FontTheme(
    /**
     * Default font family name (system) or custom family id.
     */
    val defaultFamily: String? = null,
    /**
     * Optional custom font family registry.
     */
    val families: Map<String, FontFamilySpec> = emptyMap(),
)

@Serializable
data class FontFamilySpec(
    val familyId: String? = null,
    /**
     * Asset paths for font files (e.g. "fonts/MyFont-Regular.ttf").
     */
    val regularAsset: String? = null,
    val mediumAsset: String? = null,
    val boldAsset: String? = null,
    val italicAsset: String? = null,
)

@Serializable
data class PaintTheme(
    val antiAlias: Boolean = true,
    /**
     * Default corner radius for rounded rectangles (dp).
     */
    val cornerRadiusDp: Float? = null,
    /**
     * Default border/stroke.
     */
    val stroke: StrokeStyle? = null,
    /**
     * Default shadow.
     */
    val shadow: ShadowStyle? = null,
)

@Serializable
data class LayoutTheme(
    val background: BackgroundStyle? = null,
    /**
     * Whether to extend/paint the same background into the toolbar region.
     * Useful when the keyboard background should appear continuous.
     */
    val extendToToolbar: Boolean = false,
    /**
     * Whether to extend/paint the same background into the candidate bar region.
     */
    val extendToCandidates: Boolean = false,
)

@Serializable
data class ToolbarTheme(
    val surface: SurfaceStyle = SurfaceStyle(),
    val itemText: TextStyle = TextStyle(),
    val itemIcon: IconStyle = IconStyle(),
)

@Serializable
data class CandidateTheme(
    val surface: SurfaceStyle = SurfaceStyle(),
    val candidateText: TextStyle = TextStyle(),
    val candidateTextSelected: TextStyle = TextStyle(),
    val divider: StrokeStyle? = null,
)

@Serializable
data class PopupTheme(
    val surface: SurfaceStyle = SurfaceStyle(),
    val text: TextStyle = TextStyle(),
    val textSelected: TextStyle = TextStyle(),
    val divider: StrokeStyle? = null,
)

@Serializable
data class KeyStyle(
    /**
     * Optional key background override.
     * Backwards-compatible fields are also supported: [keyBackground], [keyBackgroundPressed].
     */
    val background: BackgroundStyle? = null,
    val backgroundPressed: BackgroundStyle? = null,
    val backgroundDisabled: BackgroundStyle? = null,
    val stroke: StrokeStyle? = null,
    val strokePressed: StrokeStyle? = null,
    val shadow: ShadowStyle? = null,
    val padding: EdgeInsetsDp? = null,
    val cornerRadiusDp: Float? = null,

    val icon: IconStyle? = null,
    val label: TextStyle? = null,
    val hint: TextStyle? = null,

    /**
     * Backwards-compatible fields used by existing theme_default.json.
     */
    val keyBackground: String? = null,
    val textColor: String? = null,
    val textSizeSp: Float? = null,
)

@Serializable
data class SurfaceStyle(
    val background: BackgroundStyle? = null,
    val stroke: StrokeStyle? = null,
    val shadow: ShadowStyle? = null,
    val padding: EdgeInsetsDp? = null,
    val cornerRadiusDp: Float? = null,
)

@Serializable
data class BackgroundStyle(
    val color: String? = null,
    val image: ImageSpec? = null,
    /**
     * If true, repeats the image to fill the area.
     */
    val tile: Boolean = false,
)

@Serializable
data class ImageSpec(
    /**
     * Asset path, e.g. "drawable/key_bg_alpha.9.png" or "themes/bg.png".
     */
    val assetPath: String,
    /**
     * Optional tint color (hex or token reference).
     */
    val tint: String? = null,
    /**
     * Optional alpha (0..1).
     */
    val alpha: Float? = null,
)

@Serializable
data class TextStyle(
    /**
     * Whether the label should be drawn at all.
     */
    val visible: Boolean = true,
    val color: String? = null,
    val sizeSp: Float? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    /**
     * If provided, overrides global font family.
     */
    val fontFamily: String? = null,
    val stroke: StrokeStyle? = null,
    val shadow: ShadowStyle? = null,
    val letterSpacingEm: Float? = null,
    val maxLines: Int? = null,
    val ellipsize: Boolean? = null,
)

@Serializable
data class IconStyle(
    val visible: Boolean = true,
    val tint: String? = null,
    val sizeDp: Float? = null,
    val alpha: Float? = null,
)

@Serializable
data class StrokeStyle(
    val color: String? = null,
    val widthDp: Float? = null,
    val alpha: Float? = null,
)

@Serializable
data class ShadowStyle(
    val color: String? = null,
    val radiusDp: Float? = null,
    val dxDp: Float? = null,
    val dyDp: Float? = null,
    val alpha: Float? = null,
)

@Serializable
data class EdgeInsetsDp(
    val leftDp: Float = 0f,
    val topDp: Float = 0f,
    val rightDp: Float = 0f,
    val bottomDp: Float = 0f,
)
