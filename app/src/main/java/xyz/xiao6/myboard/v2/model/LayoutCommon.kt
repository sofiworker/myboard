package xyz.xiao6.myboard.v2.model


/**
 * 尺寸定义。
 *
 * 推荐值示例：
 * - "match"
 * - "wrap"
 * - "48dp"
 * - "1w"
 * - "50%"
 */
data class SizeConfig(
    /** 宽度 */
    val width: String? = null,

    /** 高度 */
    val height: String? = null,

    /** 最小宽度 */
    val minWidth: String? = null,

    /** 最小高度 */
    val minHeight: String? = null,

    /** 最大宽度 */
    val maxWidth: String? = null,

    /** 最大高度 */
    val maxHeight: String? = null
)

/**
 * 节点位置。
 *
 * 多用于 absolute 布局。
 */
data class PositionConfig(
    /** X 坐标 */
    val x: String? = null,

    /** Y 坐标 */
    val y: String? = null,

    /** 层级 */
    val zIndex: Int? = null
)

/**
 * 盒模型边距定义。
 */
data class BoxSpacing(
    /** 左边距 */
    val left: Float? = null,

    /** 上边距 */
    val top: Float? = null,

    /** 右边距 */
    val right: Float? = null,

    /** 下边距 */
    val bottom: Float? = null
)

/**
 * 滚动配置。
 */
data class ScrollConfig(
    /** 是否允许滚动 */
    val enabled: Boolean? = null,

    /** 滚动方向 */
    val direction: ScrollDirection? = null,

    /** 是否分页滚动 */
    val paging: Boolean? = null
)

/**
 * grid 布局配置。
 */
data class GridConfig(
    /** 列数 */
    val columns: Int? = null,

    /** 行数，可选 */
    val rows: Int? = null,

    /** 行高 */
    val rowHeight: Float? = null,

    /** 列宽，可选，字符串是为了兼容 auto 等表达 */
    val columnWidth: String? = null,

    /** 间距 */
    val gap: Float? = null
)

/**
 * linear 布局配置。
 */
data class LinearConfig(
    /** 排列方向 */
    val orientation: Orientation? = null,

    /** 子项间距 */
    val gap: Float? = null,

    /** 对齐/分布方式 */
    val gravity: Gravity? = null
)

/**
 * 子节点在父布局中的布局属性。
 */
data class LayoutProps(
    /** 权重，常用于 linear/row 布局 */
    val weight: Float? = null,

    /** 跨列数，常用于 grid */
    val colSpan: Int? = null,

    /** 跨行数，常用于 grid */
    val rowSpan: Int? = null,

    /** 自身对齐方式 */
    val alignSelf: AlignSelf? = null
)

/**
 * 绑定表达式。
 *
 * 这里不负责表达式解析，只是承载字符串。
 */
data class BindingsConfig(
    /** 可见条件表达式 */
    val visibleWhen: String? = null,

    /** 可用条件表达式 */
    val enabledWhen: String? = null,

    /** 选中条件表达式 */
    val selectedWhen: String? = null
)