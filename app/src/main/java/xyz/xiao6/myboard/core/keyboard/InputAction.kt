package xyz.xiao6.myboard.core.keyboard

/**
 * 统一的键盘动作定义。
 */
sealed interface InputAction {
    data class CommitText(val text: String) : InputAction
    data class Delete(val count: Int = 1) : InputAction
    data object ToggleShift : InputAction
    data object ToggleCapsLock : InputAction
    data class SwitchArrangement(val id: String) : InputAction
    data class SwitchLanguage(val id: String) : InputAction
    data class SelectCandidate(val index: Int) : InputAction
    data class OpenPanel(val id: String) : InputAction
    data object ClosePanel : InputAction
    data class MoveCursor(val direction: Direction) : InputAction
    data class PerformEditorAction(val action: String) : InputAction
    data object StartSTT : InputAction
    data object StopSTT : InputAction
    data class LLMComplete(val prompt: String) : InputAction
}

enum class Direction { LEFT, RIGHT, UP, DOWN }
