package xyz.xiao6.myboard.v2.model


/**
 * 一个 region 表示页面中的一个区域。
 *
 * 例如：
 * - candidate_bar 候选栏
 * - toolbar 工具栏
 * - main_keyboard 主键盘
 * - sidebar 侧边栏
 */
data class RegionConfig(
    /** region 唯一 ID */
    val id: String,

    /** region 的语义角色 */
    val role: RegionRole,

    /** region 内部采用的布局方式 */
    val layoutType: LayoutType,

    /** 是否可见 */
    val visible: Boolean? = null,

    /** 是否可用 */
    val enabled: Boolean? = null,

    /** 尺寸定义 */
    val size: SizeConfig? = null,

    /** 位置信息，通常 absolute 布局才会用到 */
    val position: PositionConfig? = null,

    /** 内边距 */
    val padding: BoxSpacing? = null,

    /** 外边距 */
    val margin: BoxSpacing? = null,

    /** 滚动配置 */
    val scroll: ScrollConfig? = null,

    /** grid 布局参数，layoutType = GRID 时常用 */
    val grid: GridConfig? = null,

    /** linear 布局参数，layoutType = LINEAR / ROWS 时常用 */
    val linear: LinearConfig? = null,

    /** 引用的样式 ID，对应 styles 中的 key */
    val styleRef: String? = null,

    /** 条件绑定，例如 visibleWhen / enabledWhen */
    val bindings: BindingsConfig? = null,

    /** region 下的子节点 */
    val children: List<NodeConfig>? = null
)