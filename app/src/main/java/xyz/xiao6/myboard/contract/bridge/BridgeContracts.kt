package xyz.xiao6.myboard.contract.bridge

import xyz.xiao6.myboard.contract.input.ResetReason

/**
 * 编辑器特性解析结果。
 * EditorInfoResolver 把 EditorInfo 转为 EditorProfile。
 */
data class EditorProfile(
    val layoutHint: LayoutHint,
    val enterAction: EnterAction,
    val enterLabel: String?,
    val candidateDisabled: Boolean,
    val composingDisabled: Boolean,
    val rawInputType: Int,
    val rawImeOptions: Int
)

/**
 * 布局建议类型。
 */
enum class LayoutHint { ALPHA, NUMBER, PHONE, DATETIME, URL, EMAIL, PASSWORD }

/**
 * 回车键动作类型。
 */
enum class EnterAction { UNSPECIFIED, DONE, GO, NEXT, PREVIOUS, SEARCH, SEND, NONE }

/**
 * 光标选择快照。
 */
data class SelectionSnapshot(
    val start: Int,
    val end: Int,
    val composingStart: Int,
    val composingEnd: Int
)

/**
 * 光标选择决策。
 */
sealed interface SelectionDecision {
    data object Trusted : SelectionDecision
    data class MustReset(val reason: ResetReason) : SelectionDecision
}