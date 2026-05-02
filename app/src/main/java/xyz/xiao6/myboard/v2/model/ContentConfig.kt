package xyz.xiao6.myboard.v2.model

/**
 * 节点显示内容。
 */
data class ContentConfig(
    /** 主标签，例如 q / 空格 / 回车 */
    val label: String? = null,

    /** 次标签，例如 T9 上的 ABC / 候选注释 */
    val subLabel: String? = null,

    /** 图标资源名或图标标识 */
    val icon: String? = null,

    /** 九宫位 hint 提示 */
    val hint: HintSet? = null
)

/**
 * hint 文本集合。
 *
 * 比 Map 更稳定，后续做 UI 时也更方便。
 */
data class HintSet(
    /** 左上 */
    val topLeft: String? = null,

    /** 上中 */
    val topCenter: String? = null,

    /** 右上 */
    val topRight: String? = null,

    /** 左中 */
    val centerLeft: String? = null,

    /** 正中 */
    val center: String? = null,

    /** 右中 */
    val centerRight: String? = null,

    /** 左下 */
    val bottomLeft: String? = null,

    /** 下中 */
    val bottomCenter: String? = null,

    /** 右下 */
    val bottomRight: String? = null
)