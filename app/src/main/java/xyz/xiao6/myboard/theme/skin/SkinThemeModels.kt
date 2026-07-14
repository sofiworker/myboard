package xyz.xiao6.myboard.theme.skin

/**
 * 高级皮肤协议 id 与元数据。
 * wire value 仅在此集中定义，业务代码通过枚举引用。
 */
enum class SkinThemeId(val id: String) {
    PURE_FLAT("pure_flat");

    companion object {
        fun fromId(id: String?): SkinThemeId? =
            id?.let { raw -> entries.firstOrNull { it.id == raw } }
    }
}

enum class SkinColorPolicy {
    /** 继承 Foundation 色板，仅叠加装饰/局部样式。 */
    INHERIT,

    /** 设计师锁定完整配色，覆盖 Foundation 颜色与按键样式。 */
    LOCKED,

    /** 根据 Foundation 语义 token 自适应；首版不开放给第三方。 */
    ADAPTIVE
}

data class SkinThemeMeta(
    val id: SkinThemeId,
    val colorPolicy: SkinColorPolicy,
    val usesImages: Boolean = false,
    val usesDecorations: Boolean = false,
    val usesLayoutBindings: Boolean = false
)
