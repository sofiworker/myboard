package xyz.xiao6.myboard.v2.model

/**
 * 样式配置。
 *
 * 这里只放一部分通用视觉字段，避免过早复杂化。
 */
data class StyleConfig(
    /** 默认背景色，例如 #FFFFFFFF */
    val background: String? = null,

    /** 按下态背景色 */
    val pressedBackground: String? = null,

    /** 主文本颜色 */
    val textColor: String? = null,

    /** hint 文本颜色 */
    val hintColor: String? = null,

    /** 边框颜色 */
    val borderColor: String? = null,

    /** 边框宽度 */
    val borderWidth: Float? = null,

    /** 圆角 */
    val cornerRadius: Float? = null,

    /** 主文字字号 */
    val fontSize: Float? = null,

    /** hint 字号 */
    val hintFontSize: Float? = null,

    /** 字重 */
    val fontWeight: FontWeight? = null
)