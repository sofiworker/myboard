package xyz.xiao6.myboard.core.input

import xyz.xiao6.myboard.core.keyboard.Candidate
import xyz.xiao6.myboard.core.keyboard.EngineResult
import xyz.xiao6.myboard.core.keyboard.ShiftState

/**
 * 通用输入引擎接口。
 */
interface InputEngine {
    val id: String
    val type: EngineType
    val config: InputMethodConfig

    fun activate()
    fun deactivate()
    fun reset()

    suspend fun onKeyInput(char: String): EngineResult
    suspend fun onBackspace(): EngineResult
    suspend fun onSpace(): EngineResult
    suspend fun onEnter(): EngineResult
    suspend fun onCandidateSelected(index: Int): EngineResult
    suspend fun onShift(): EngineResult
    suspend fun onDoubleShift(): EngineResult

    fun getComposingText(): String
    fun getCandidates(): List<Candidate>
    fun getShiftState(): ShiftState
}

enum class EngineType { DIRECT, COMPOSITION, COMPLEX }

data class InputMethodConfig(
    val id: String,
    val name: String,
    val engine: String,
    val language: String,
    val engineParams: Map<String, String> = emptyMap(),
    val shift: ShiftConfig? = null,
    val enter: EnterConfig? = null,
    val space: SpaceConfig? = null,
    val backspace: BackspaceConfig? = null
)

data class ShiftConfig(
    val mode: String = "autoOff",
    val autoOffAfterKeys: Boolean = true
)

data class EnterConfig(
    val idle: String = "editorAction",
    val composing: String = "commitThenAction",
    val hasCandidates: String = "selectFirst"
)

data class SpaceConfig(
    val idle: String = "commitText",
    val composing: String = "commitComposition",
    val hasCandidates: String = "selectFirst"
)

data class BackspaceConfig(
    val idle: String = "delete",
    val composing: String = "deleteComposition"
)
