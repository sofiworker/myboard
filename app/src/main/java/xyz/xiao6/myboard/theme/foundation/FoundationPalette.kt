package xyz.xiao6.myboard.theme.foundation

data class FoundationPalette(
    val id: FoundationPaletteId,
    val seedColor: String,
    val lightAccent: String,
    val darkAccent: String,
    val titleKey: String
) {
    companion object {
        val all: List<FoundationPalette> = listOf(
            FoundationPalette(FoundationPaletteId.GBOARD_BLUE, "#1A73E8", "#1A73E8", "#8AB4F8", "theme_palette_gboard_blue"),
            FoundationPalette(FoundationPaletteId.MINT, "#00A86B", "#00875A", "#65D6A4", "theme_palette_mint"),
            FoundationPalette(FoundationPaletteId.ROSE, "#D9306E", "#C5225E", "#F28BAE", "theme_palette_rose"),
            FoundationPalette(FoundationPaletteId.VIOLET, "#7E57C2", "#6D46B3", "#B69DF8", "theme_palette_violet"),
            FoundationPalette(FoundationPaletteId.GRAPHITE, "#5F6368", "#4E5358", "#BDC1C6", "theme_palette_graphite")
        )

        fun byId(id: FoundationPaletteId): FoundationPalette =
            all.firstOrNull { it.id == id } ?: all.first()

        fun resolve(selection: FoundationThemeSelection, dynamicSeedColor: String?): FoundationPalette {
            return when (selection.paletteSource) {
                PaletteSource.PRESET -> byId(selection.paletteId)
                PaletteSource.SYSTEM_DYNAMIC -> {
                    val seed = dynamicSeedColor?.takeIf { it.isNotBlank() } ?: byId(selection.paletteId).seedColor
                    fromSeed(FoundationPaletteId.GBOARD_BLUE, seed, "theme_palette_system_dynamic")
                }
                PaletteSource.CUSTOM_SEED -> {
                    val seed = selection.customSeedColor?.takeIf { it.isNotBlank() } ?: byId(selection.paletteId).seedColor
                    fromSeed(selection.paletteId, seed, "theme_palette_custom")
                }
            }
        }

        private fun fromSeed(id: FoundationPaletteId, seed: String, titleKey: String): FoundationPalette =
            FoundationPalette(
                id = id,
                seedColor = ThemeColorUtils.normalizeHex(seed),
                lightAccent = ThemeColorUtils.normalizeHex(seed),
                darkAccent = ThemeColorUtils.mix(seed, "#FFFFFF", 0.45f),
                titleKey = titleKey
            )
    }
}
