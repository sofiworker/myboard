package xyz.xiao6.myboard.core.contract

/**
 * 输入会话的内部状态。
 * KeyboardContext 只保存 UI 所需快照，完整 buffer 状态属于 InputSession。
 */
data class InputSessionState(
    val rawBuffer: String = "",
    val queryBuffer: String = "",
    val composingText: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selectedIndex: Int = -1,
    val page: Int = 0
)