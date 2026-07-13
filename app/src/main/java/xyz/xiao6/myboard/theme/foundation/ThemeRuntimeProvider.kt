package xyz.xiao6.myboard.theme.foundation

import xyz.xiao6.myboard.theme.ThemeDoc

data class ThemeRuntime(
    val appearanceSettings: AppearanceSettings,
    val variant: ThemeVariant,
    val doc: ThemeDoc,
    val skinThemeId: String? = appearanceSettings.skinThemeId
)

class ThemeRuntimeProvider(
    private val colorGenerator: ThemeColorGenerator = ThemeColorGenerator()
) {
    fun resolve(
        settings: AppearanceSettings,
        systemDark: Boolean,
        dynamicSeedColor: String? = null
    ): ThemeRuntime {
        val variant = when (settings.foundation.appearanceMode) {
            AppearanceMode.FOLLOW_SYSTEM -> if (systemDark) ThemeVariant.DARK else ThemeVariant.LIGHT
            AppearanceMode.LIGHT -> ThemeVariant.LIGHT
            AppearanceMode.DARK -> ThemeVariant.DARK
        }
        val doc = colorGenerator.generate(
            selection = settings.foundation,
            variant = variant,
            dynamicSeedColor = dynamicSeedColor
        )
        return ThemeRuntime(
            appearanceSettings = settings,
            variant = variant,
            doc = doc,
            skinThemeId = settings.skinThemeId
        )
    }
}
