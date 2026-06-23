package xyz.xiao6.myboard.core.contract

/**
 * 输入事件。
 * 布局层只产生 InputAction，由 InputPipeline 转成 InputEvent。引擎不直接读取布局 key id。
 */
sealed interface InputEvent {
    data class PushToken(val token: String) : InputEvent
    data object Backspace : InputEvent
    data object Space : InputEvent
    data object Enter : InputEvent
    data class SelectCandidate(val index: Int) : InputEvent
    data class SelectCandidateByLabel(val label: String) : InputEvent
    data object Reset : InputEvent
}