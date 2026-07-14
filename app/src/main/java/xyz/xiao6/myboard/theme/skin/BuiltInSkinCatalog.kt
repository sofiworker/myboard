package xyz.xiao6.myboard.theme.skin

import xyz.xiao6.myboard.theme.ThemeDoc
import xyz.xiao6.myboard.theme.foundation.ThemeVariant

/**
 * 内置高级皮肤目录。
 * 与 BuiltInThemes / Foundation 分离，避免第二套状态源。
 */
object BuiltInSkinCatalog {
    val all: List<SkinThemeMeta> = listOf(PureFlatSkin.meta)

    fun contains(skinThemeId: String?): Boolean =
        SkinThemeId.fromId(skinThemeId) != null

    fun metaOf(skinThemeId: String?): SkinThemeMeta? {
        val id = SkinThemeId.fromId(skinThemeId) ?: return null
        return all.firstOrNull { it.id == id }
    }

    /**
     * 解析皮肤 ThemeDoc。
     * 未知 id 返回 null，由调用方回退 Foundation。
     */
    fun resolve(skinThemeId: String?, variant: ThemeVariant): ThemeDoc? {
        return when (SkinThemeId.fromId(skinThemeId)) {
            SkinThemeId.PURE_FLAT -> PureFlatSkin.themeDoc(variant)
            null -> null
        }
    }
}
