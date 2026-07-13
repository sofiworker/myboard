package xyz.xiao6.myboard.theme.foundation

import kotlinx.serialization.Serializable

@Serializable
data class AppearanceSettings(
    val foundation: FoundationThemeSelection = FoundationThemeSelection(),
    val skinThemeId: String? = null
) {
    companion object {
        fun default(): AppearanceSettings = AppearanceSettings()
    }
}

@Serializable
data class FoundationThemeSelection(
    val paletteSource: PaletteSource = PaletteSource.PRESET,
    val paletteId: FoundationPaletteId = FoundationPaletteId.GBOARD_BLUE,
    val customSeedColor: String? = null,
    val keyTreatment: KeyTreatment = KeyTreatment.FILLED,
    val keyContrast: KeyContrast = KeyContrast.NORMAL,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val appearanceMode: AppearanceMode = AppearanceMode.FOLLOW_SYSTEM
)

@Serializable
enum class PaletteSource {
    PRESET,
    SYSTEM_DYNAMIC,
    CUSTOM_SEED
}

@Serializable
enum class FoundationPaletteId {
    GBOARD_BLUE,
    MINT,
    ROSE,
    VIOLET,
    GRAPHITE
}

@Serializable
enum class KeyTreatment {
    FILLED,
    OUTLINED,
    BORDERLESS
}

@Serializable
enum class KeyContrast {
    NORMAL,
    HIGH
}

@Serializable
enum class CornerStyle {
    COMPACT,
    ROUNDED,
    PILL
}

@Serializable
enum class AppearanceMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

enum class ThemeVariant {
    LIGHT,
    DARK
}
