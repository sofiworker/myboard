package xyz.xiao6.myboard.v2.model


/**
 * 所有动作参数的基类。
 */
sealed class ActionPayload

/**
 * 推入一个 token。
 *
 * 常用于普通字符键，例如 q / w / e。
 */
data class PushTokenPayload(
    /** 要推入的 token */
    val token: String
) : ActionPayload()

/**
 * 直接提交一段文本。
 */
data class CommitTextPayload(
    /** 要提交的文本 */
    val text: String
) : ActionPayload()

/**
 * 提交候选项。
 */
data class CommitCandidatePayload(
    /** 候选索引，可选 */
    val index: Int? = null,

    /** 候选文本，可选 */
    val text: String? = null
) : ActionPayload()

/**
 * 删除动作参数。
 */
data class DeletePayload(
    /** 删除数量，默认 1 */
    val count: Int = 1
) : ActionPayload()

/**
 * 切换 layer。
 */
data class SwitchLayerPayload(
    /** 目标 layer */
    val layer: String
) : ActionPayload()

/**
 * 切换 mode。
 */
data class SwitchModePayload(
    /** 目标 mode */
    val mode: String
) : ActionPayload()

/**
 * 打开面板。
 */
data class OpenPanelPayload(
    /** 面板名，例如 emoji / clipboard */
    val panel: String
) : ActionPayload()

/**
 * 关闭面板。
 */
data class ClosePanelPayload(
    /** 面板名，可空，空则表示关闭当前面板 */
    val panel: String? = null
) : ActionPayload()

/**
 * 移动光标。
 */
data class MoveCursorPayload(
    /** 移动方向 */
    val direction: CursorMoveDirection,

    /** 移动次数 */
    val count: Int = 1
) : ActionPayload()

/**
 * 弹出 popup。
 */
data class PopupPayload(
    /** popup ID */
    val popupId: String
) : ActionPayload()

/**
 * 替换文本。
 */
data class ReplaceTextPayload(
    /** 被替换内容，可选 */
    val from: String? = null,

    /** 目标内容 */
    val to: String
) : ActionPayload()

/**
 * 自定义动作。
 *
 * 给业务层留扩展口。
 */
data class CustomPayload(
    /** 自定义动作名 */
    val name: String,

    /** 业务参数 */
    val arguments: Map<String, String>? = null
) : ActionPayload()