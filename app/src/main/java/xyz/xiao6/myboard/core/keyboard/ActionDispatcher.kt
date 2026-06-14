package xyz.xiao6.myboard.core.keyboard

import android.view.inputmethod.InputConnection

/**
 * Action 分发器：将 InputAction 转换为实际操作。
 */
class ActionDispatcher(
    private val stateManager: KeyboardStateManager
) {
    private var inputConnection: InputConnection? = null

    fun setInputConnection(ic: InputConnection?) {
        inputConnection = ic
    }

    suspend fun dispatch(action: InputAction) {
        when (action) {
            is InputAction.CommitText -> commitText(action.text)
            is InputAction.Delete -> delete(action.count)
            is InputAction.ToggleShift -> stateManager.toggleShift()
            is InputAction.ToggleCapsLock -> stateManager.toggleCapsLock()
            is InputAction.SwitchArrangement -> switchArrangement(action.id)
            is InputAction.SwitchLanguage -> switchLanguage(action.id)
            is InputAction.SelectCandidate -> selectCandidate(action.index)
            is InputAction.OpenPanel -> openPanel(action.id)
            is InputAction.ClosePanel -> closePanel()
            is InputAction.MoveCursor -> moveCursor(action.direction)
            is InputAction.PerformEditorAction -> performEditorAction(action.action)
            else -> {}
        }
    }

    private fun commitText(text: String) {
        val ic = inputConnection ?: return
        ic.commitText(text, 1)
        stateManager.clearComposing()
    }

    private fun delete(count: Int) {
        val ic = inputConnection ?: return
        ic.deleteSurroundingText(count, 0)
    }

    private fun switchArrangement(id: String) {
        stateManager.update { it.copy(arrangement = id) }
    }

    private fun switchLanguage(id: String) {
        stateManager.update { it.copy(languageId = id) }
    }

    private fun selectCandidate(index: Int) {
        val state = stateManager.state.value
        val candidate = state.candidates.getOrNull(index) ?: return
        stateManager.clearComposing()
        commitText(candidate.text)
    }

    private fun openPanel(id: String) {
        val panel = when (id) {
            "emoji" -> PanelType.EMOJI
            "symbol" -> PanelType.SYMBOL
            "clipboard" -> PanelType.CLIPBOARD
            "llm" -> PanelType.LLM
            "stt" -> PanelType.STT
            else -> PanelType.NONE
        }
        stateManager.update { it.copy(activePanel = panel) }
    }

    private fun closePanel() {
        stateManager.update { it.copy(activePanel = PanelType.NONE) }
    }

    private fun moveCursor(direction: Direction) {
        val ic = inputConnection ?: return
        val current = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        if (current == null) return
        val newPos = when (direction) {
            Direction.LEFT -> current.startOffset - 1
            Direction.RIGHT -> current.startOffset + 1
            else -> current.startOffset
        }
        ic.setSelection(newPos, newPos)
    }

    private fun performEditorAction(action: String) {
        val ic = inputConnection ?: return
        val imeOptions = when (action) {
            "search" -> android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            "send" -> android.view.inputmethod.EditorInfo.IME_ACTION_SEND
            "next" -> android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
            "done" -> android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            else -> android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED
        }
        ic.performEditorAction(imeOptions)
    }
}
