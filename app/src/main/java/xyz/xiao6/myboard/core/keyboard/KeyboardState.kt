package xyz.xiao6.myboard.core.keyboard

/**
 * 键盘核心状态模型。
 */
data class KeyboardState(
    val languageId: String = "en_us",
    val inputMethodId: String = "en_qwerty",
    val arrangement: String = "alpha",
    val shiftState: ShiftState = ShiftState.OFF,
    val capsLock: Boolean = false,
    val composingText: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selectedCandidateIndex: Int = -1,
    val activePanel: PanelType = PanelType.NONE
) {
    val isComposing: Boolean get() = composingText.isNotEmpty()
    val hasCandidates: Boolean get() = candidates.isNotEmpty()
}

enum class ShiftState { OFF, ON, CAPS_LOCK }

enum class PanelType { NONE, EMOJI, SYMBOL, CLIPBOARD, LLM, STT }

data class Candidate(
    val text: String,
    val type: CandidateType = CandidateType.WORD,
    val score: Float = 0f,
    val source: CandidateSource = CandidateSource.SYSTEM
)

enum class CandidateType { WORD, PREFIX, PREDICTION, CORRECTION, LLM }
enum class CandidateSource { SYSTEM, USER, HISTORY, LLM }

/**
 * 输入引擎结果。
 */
sealed interface EngineResult {
    data class UpdateComposing(val text: String) : EngineResult
    data class CommitText(val text: String) : EngineResult
    data class UpdateCandidates(val candidates: List<Candidate>) : EngineResult
    data class Combined(
        val composing: String?,
        val candidates: List<Candidate>?,
        val commit: String?
    ) : EngineResult
    data object Nothing : EngineResult
    data class Delete(val count: Int) : EngineResult
}
