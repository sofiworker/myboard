package xyz.xiao6.myboard.core.keyboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 键盘状态管理器。
 */
class KeyboardStateManager {
    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    fun update(transform: (KeyboardState) -> KeyboardState) {
        _state.update(transform)
    }

    fun reset() {
        _state.value = KeyboardState()
    }

    fun toggleShift() {
        update { s ->
            when (s.shiftState) {
                ShiftState.OFF -> s.copy(shiftState = ShiftState.ON)
                ShiftState.ON -> s.copy(shiftState = ShiftState.OFF)
                ShiftState.CAPS_LOCK -> s.copy(shiftState = ShiftState.OFF, capsLock = false)
            }
        }
    }

    fun toggleCapsLock() {
        update { s ->
            if (s.shiftState == ShiftState.CAPS_LOCK) {
                s.copy(shiftState = ShiftState.OFF, capsLock = false)
            } else {
                s.copy(shiftState = ShiftState.CAPS_LOCK, capsLock = true)
            }
        }
    }

    fun clearComposing() {
        update { it.copy(composingText = "", candidates = emptyList(), selectedCandidateIndex = -1) }
    }

    fun setComposing(text: String, candidates: List<Candidate> = emptyList()) {
        update { it.copy(composingText = text, candidates = candidates) }
    }

    fun selectCandidate(index: Int) {
        val state = _state.value
        val candidate = state.candidates.getOrNull(index) ?: return
        update {
            it.copy(
                composingText = "",
                candidates = emptyList(),
                selectedCandidateIndex = -1
            )
        }
        candidate
    }
}
