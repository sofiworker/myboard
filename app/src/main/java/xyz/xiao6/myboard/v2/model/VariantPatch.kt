package xyz.xiao6.myboard.v2.model

/**
 * 节点在某个 layer / state 下的差异补丁。
 *
 * 例如：
 * - shift 时 label 从 a 变为 A
 * - symbol 时 token 从 a 变为 @
 */
data class VariantPatch(
    /** 覆盖可见性 */
    val visible: Boolean? = null,

    /** 覆盖可用性 */
    val enabled: Boolean? = null,

    /** 覆盖尺寸 */
    val size: SizeConfig? = null,

    /** 覆盖布局属性 */
    val layout: LayoutProps? = null,

    /** 覆盖样式引用 */
    val styleRef: String? = null,

    /** 覆盖显示内容 */
    val content: ContentConfig? = null,

    /** 覆盖 key 数据 */
    val key: KeyData? = null,

    /** 覆盖 candidate 数据 */
    val candidate: CandidateData? = null,

    /** 覆盖 button 数据 */
    val button: ButtonData? = null,

    /** 覆盖动作集合 */
    val actions: ActionSetPatch? = null,

    /** 覆盖条件绑定 */
    val bindings: BindingsConfig? = null
)

/**
 * 动作集合补丁。
 */
data class ActionSetPatch(
    /** 覆盖 tap */
    val tap: ActionConfig? = null,

    /** 覆盖 longPress */
    val longPress: ActionConfig? = null,

    /** 覆盖 doubleTap */
    val doubleTap: ActionConfig? = null,

    /** 覆盖 repeat */
    val repeat: ActionConfig? = null,

    /** 覆盖 press */
    val press: ActionConfig? = null,

    /** 覆盖 release */
    val release: ActionConfig? = null,

    /** 覆盖 swipeUp */
    val swipeUp: ActionConfig? = null,

    /** 覆盖 swipeDown */
    val swipeDown: ActionConfig? = null,

    /** 覆盖 swipeLeft */
    val swipeLeft: ActionConfig? = null,

    /** 覆盖 swipeRight */
    val swipeRight: ActionConfig? = null
)