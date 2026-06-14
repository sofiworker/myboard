package xyz.xiao6.myboard.core.input

import xyz.xiao6.myboard.core.dictionary.SuggestionEngine
import xyz.xiao6.myboard.core.keyboard.Candidate
import xyz.xiao6.myboard.core.keyboard.EngineResult
import xyz.xiao6.myboard.core.keyboard.ShiftState

/**
 * 通用直接输入引擎：适用于英文、法语、德语等。
 */
class DirectInputEngine(
    override val config: InputMethodConfig,
    private val suggestionEngine: SuggestionEngine
) : InputEngine {
    override val id = config.id
    override val type = EngineType.DIRECT

    private var shiftState: ShiftState = ShiftState.OFF
    private var capsLock: Boolean = false

    override fun activate() {}
    override fun deactivate() {}
    override fun reset() {
        shiftState = ShiftState.OFF
        capsLock = false
    }

    override suspend fun onKeyInput(char: String): EngineResult {
        val output = when {
            capsLock -> char.uppercase()
            shiftState == ShiftState.ON -> {
                if (config.shift?.autoOffAfterKeys == true) shiftState = ShiftState.OFF
                char.uppercase()
            }
            else -> char.lowercase()
        }
        suggestionEngine.recordWord(output)
        return EngineResult.CommitText(output)
    }

    override suspend fun onShift(): EngineResult {
        if (config.shift?.mode == "disabled") return EngineResult.Nothing
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.ON
            ShiftState.ON -> ShiftState.OFF
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        capsLock = false
        return EngineResult.Nothing
    }

    override suspend fun onDoubleShift(): EngineResult {
        if (config.shift?.mode == "disabled") return EngineResult.Nothing
        capsLock = !capsLock
        shiftState = if (capsLock) ShiftState.CAPS_LOCK else ShiftState.OFF
        return EngineResult.Nothing
    }

    override suspend fun onBackspace(): EngineResult = EngineResult.Delete(1)
    override suspend fun onSpace(): EngineResult = EngineResult.CommitText(" ")
    override suspend fun onEnter(): EngineResult = EngineResult.Nothing
    override suspend fun onCandidateSelected(index: Int): EngineResult = EngineResult.Nothing
    override fun getComposingText(): String = ""
    override fun getCandidates(): List<Candidate> = emptyList()
    override fun getShiftState(): ShiftState = shiftState
}
