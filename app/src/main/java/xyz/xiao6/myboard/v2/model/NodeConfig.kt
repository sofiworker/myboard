package xyz.xiao6.myboard.v2.model

/**
 * 所有节点的基类。
 *
 * 注意：
 * - Region 是页面级区域
 * - Node 是 region 内部的具体内容节点
 */
sealed class NodeConfig {
    /** 节点类型 */
    abstract val type: NodeType

    /** 节点唯一 ID */
    abstract val id: String

    /** 是否可见 */
    abstract val visible: Boolean?

    /** 是否可用 */
    abstract val enabled: Boolean?

    /** 节点尺寸 */
    abstract val size: SizeConfig?

    /** 布局属性，例如 weight / colSpan / rowSpan */
    abstract val layout: LayoutProps?

    /** 节点位置，absolute 布局时有意义 */
    abstract val position: PositionConfig?

    /** 样式引用 */
    abstract val styleRef: String?

    /** 显示内容，例如 label / subLabel / icon / hint */
    abstract val content: ContentConfig?

    /** 条件绑定 */
    abstract val bindings: BindingsConfig?

    /** 不同状态下的差异补丁，例如 shift/symbol */
    abstract val variants: Map<String, VariantPatch>?
}

/**
 * 行节点。
 *
 * 常用于 qwerty 中一整行按键。
 */
data class RowNode(
    override val type: NodeType = NodeType.ROW,
    override val id: String,
    override val visible: Boolean? = null,
    override val enabled: Boolean? = null,
    override val size: SizeConfig? = null,
    override val layout: LayoutProps? = null,
    override val position: PositionConfig? = null,
    override val styleRef: String? = null,
    override val content: ContentConfig? = null,
    override val bindings: BindingsConfig? = null,
    override val variants: Map<String, VariantPatch>? = null,

    /** 行内排布方式，通常是横向 linear */
    val linear: LinearConfig? = null,

    /** 行中的子节点，一般是 key/button/spacer */
    val children: List<NodeConfig> = emptyList()
) : NodeConfig()

/**
 * 分组节点。
 *
 * 用于更复杂的嵌套结构，例如候选区中的复合区域。
 */
data class GroupNode(
    override val type: NodeType = NodeType.GROUP,
    override val id: String,
    override val visible: Boolean? = null,
    override val enabled: Boolean? = null,
    override val size: SizeConfig? = null,
    override val layout: LayoutProps? = null,
    override val position: PositionConfig? = null,
    override val styleRef: String? = null,
    override val content: ContentConfig? = null,
    override val bindings: BindingsConfig? = null,
    override val variants: Map<String, VariantPatch>? = null,

    /** group 自己的布局类型 */
    val layoutType: LayoutType? = null,

    /** 当 layoutType = GRID 时使用 */
    val grid: GridConfig? = null,

    /** 当 layoutType = LINEAR 时使用 */
    val linear: LinearConfig? = null,

    /** 子节点 */
    val children: List<NodeConfig> = emptyList()
) : NodeConfig()

/**
 * 普通按键节点。
 */
data class KeyNode(
    override val type: NodeType = NodeType.KEY,
    override val id: String,
    override val visible: Boolean? = null,
    override val enabled: Boolean? = null,
    override val size: SizeConfig? = null,
    override val layout: LayoutProps? = null,
    override val position: PositionConfig? = null,
    override val styleRef: String? = null,
    override val content: ContentConfig? = null,
    override val bindings: BindingsConfig? = null,
    override val variants: Map<String, VariantPatch>? = null,

    /** 按键业务数据 */
    val key: KeyData? = null,

    /** 手势到动作的集合 */
    val actions: KeyActionSet? = null
) : NodeConfig()

/**
 * 候选项节点。
 */
data class CandidateNode(
    override val type: NodeType = NodeType.CANDIDATE,
    override val id: String,
    override val visible: Boolean? = null,
    override val enabled: Boolean? = null,
    override val size: SizeConfig? = null,
    override val layout: LayoutProps? = null,
    override val position: PositionConfig? = null,
    override val styleRef: String? = null,
    override val content: ContentConfig? = null,
    override val bindings: BindingsConfig? = null,
    override val variants: Map<String, VariantPatch>? = null,

    /** 候选项业务数据 */
    val candidate: CandidateData? = null,

    /** 候选项动作 */
    val actions: CandidateActionSet? = null
) : NodeConfig()

/**
 * 功能按钮节点。
 *
 * 例如：
 * - Shift
 * - Backspace
 * - Enter
 * - 页面切换
 * - 工具栏按钮
 */
data class ButtonNode(
    override val type: NodeType = NodeType.BUTTON,
    override val id: String,
    override val visible: Boolean? = null,
    override val enabled: Boolean? = null,
    override val size: SizeConfig? = null,
    override val layout: LayoutProps? = null,
    override val position: PositionConfig? = null,
    override val styleRef: String? = null,
    override val content: ContentConfig? = null,
    override val bindings: BindingsConfig? = null,
    override val variants: Map<String, VariantPatch>? = null,

    /** 按钮业务数据 */
    val button: ButtonData? = null,

    /** 按钮动作 */
    val actions: ButtonActionSet? = null
) : NodeConfig()

/**
 * 占位空白节点。
 *
 * 常用于对齐、留白、行内补偿。
 */
data class SpacerNode(
    override val type: NodeType = NodeType.SPACER,
    override val id: String,
    override val visible: Boolean? = null,
    override val enabled: Boolean? = null,
    override val size: SizeConfig? = null,
    override val layout: LayoutProps? = null,
    override val position: PositionConfig? = null,
    override val styleRef: String? = null,
    override val content: ContentConfig? = null,
    override val bindings: BindingsConfig? = null,
    override val variants: Map<String, VariantPatch>? = null
) : NodeConfig()

/**
 * 分隔线/分隔节点。
 */
data class DividerNode(
    override val type: NodeType = NodeType.DIVIDER,
    override val id: String,
    override val visible: Boolean? = null,
    override val enabled: Boolean? = null,
    override val size: SizeConfig? = null,
    override val layout: LayoutProps? = null,
    override val position: PositionConfig? = null,
    override val styleRef: String? = null,
    override val content: ContentConfig? = null,
    override val bindings: BindingsConfig? = null,
    override val variants: Map<String, VariantPatch>? = null
) : NodeConfig()