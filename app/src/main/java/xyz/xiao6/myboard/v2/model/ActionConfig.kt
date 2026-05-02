package xyz.xiao6.myboard.v2.model

/**
 * 手势动作集合接口。
 *
 * 不同节点都可以实现这套手势集合：
 * - tap
 * - longPress
 * - swipe
 * - repeat
 */
interface ActionSet {
    /** 单击 */
    val tap: ActionConfig?

    /** 长按 */
    val longPress: ActionConfig?

    /** 双击 */
    val doubleTap: ActionConfig?

    /** 按住重复触发 */
    val repeat: ActionConfig?

    /** 按下时 */
    val press: ActionConfig?

    /** 抬起时 */
    val release: ActionConfig?

    /** 上滑 */
    val swipeUp: ActionConfig?

    /** 下滑 */
    val swipeDown: ActionConfig?

    /** 左滑 */
    val swipeLeft: ActionConfig?

    /** 右滑 */
    val swipeRight: ActionConfig?
}

/**
 * Key 的手势动作集合。
 */
data class KeyActionSet(
    override val tap: ActionConfig? = null,
    override val longPress: ActionConfig? = null,
    override val doubleTap: ActionConfig? = null,
    override val repeat: ActionConfig? = null,
    override val press: ActionConfig? = null,
    override val release: ActionConfig? = null,
    override val swipeUp: ActionConfig? = null,
    override val swipeDown: ActionConfig? = null,
    override val swipeLeft: ActionConfig? = null,
    override val swipeRight: ActionConfig? = null
) : ActionSet

/**
 * Candidate 的手势动作集合。
 */
data class CandidateActionSet(
    override val tap: ActionConfig? = null,
    override val longPress: ActionConfig? = null,
    override val doubleTap: ActionConfig? = null,
    override val repeat: ActionConfig? = null,
    override val press: ActionConfig? = null,
    override val release: ActionConfig? = null,
    override val swipeUp: ActionConfig? = null,
    override val swipeDown: ActionConfig? = null,
    override val swipeLeft: ActionConfig? = null,
    override val swipeRight: ActionConfig? = null
) : ActionSet

/**
 * Button 的手势动作集合。
 */
data class ButtonActionSet(
    override val tap: ActionConfig? = null,
    override val longPress: ActionConfig? = null,
    override val doubleTap: ActionConfig? = null,
    override val repeat: ActionConfig? = null,
    override val press: ActionConfig? = null,
    override val release: ActionConfig? = null,
    override val swipeUp: ActionConfig? = null,
    override val swipeDown: ActionConfig? = null,
    override val swipeLeft: ActionConfig? = null,
    override val swipeRight: ActionConfig? = null
) : ActionSet

/**
 * 单个动作定义。
 */
data class ActionConfig(
    /** 动作类型 */
    val actionType: ActionType,

    /** 动作参数 */
    val payload: ActionPayload? = null
)