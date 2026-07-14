package xyz.xiao6.myboard.theme.foundation

import xyz.xiao6.myboard.theme.ThemeDoc
import xyz.xiao6.myboard.theme.skin.BuiltInSkinCatalog
import xyz.xiao6.myboard.theme.skin.SkinColorPolicy

data class ThemeRuntime(
    val appearanceSettings: AppearanceSettings,
    val variant: ThemeVariant,
    val doc: ThemeDoc,
    val skinThemeId: String? = appearanceSettings.skinThemeId,
    val foundationDoc: ThemeDoc? = null
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
        val foundationDoc = colorGenerator.generate(
            selection = settings.foundation,
            variant = variant,
            dynamicSeedColor = dynamicSeedColor
        )
        val skinMeta = BuiltInSkinCatalog.metaOf(settings.skinThemeId)
        val skinDoc = BuiltInSkinCatalog.resolve(settings.skinThemeId, variant)
        // locked：完整覆盖；未知/解析失败：回退 foundation
        val effectiveDoc = when {
            skinDoc != null && skinMeta?.colorPolicy == SkinColorPolicy.LOCKED -> skinDoc
            skinDoc != null && skinMeta?.colorPolicy == SkinColorPolicy.INHERIT -> skinDoc
            skinDoc != null && skinMeta?.colorPolicy == SkinColorPolicy.ADAPTIVE -> skinDoc
            else -> foundationDoc
        }
        val effectiveSkinId = settings.skinThemeId?.takeIf { skinDoc != null }

        return ThemeRuntime(
            appearanceSettings = settings,
            variant = variant,
            doc = effectiveDoc,
            skinThemeId = effectiveSkinId,
            foundationDoc = foundationDoc
        )
    }
}
