package xyz.xiao6.myboard.core.input

import xyz.xiao6.myboard.core.dictionary.SuggestionEngine
import xyz.xiao6.myboard.core.keyboard.Candidate
import xyz.xiao6.myboard.core.keyboard.EngineResult
import xyz.xiao6.myboard.core.keyboard.ShiftState

/**
 * 通用组合输入引擎：适用于拼音、五笔、双拼、罗马字等。
 */
class CompositionInputEngine(
    override val config: InputMethodConfig,
    private val suggestionEngine: SuggestionEngine,
    private val composingResolver: ComposingResolver
) : InputEngine {
    override val id = config.id
    override val type = EngineType.COMPOSITION

    private val composingBuffer = StringBuilder()
    private var candidates: List<Candidate> = emptyList()

    override fun activate() {}
    override fun deactivate() {}
    override fun reset() {
        composingBuffer.clear()
        candidates = emptyList()
    }

    override suspend fun onKeyInput(char: String): EngineResult {
        composingBuffer.append(char)
        val resolved = composingResolver.resolve(composingBuffer.toString(), config.engineParams)
        candidates = suggestionEngine.suggest(resolved.displayText)
        return EngineResult.Combined(resolved.displayText, candidates, null)
    }

    override suspend fun onSpace(): EngineResult {
        return when {
            composingBuffer.isEmpty() -> EngineResult.CommitText(" ")
            candidates.isNotEmpty() -> {
                val first = candidates.first()
                reset()
                EngineResult.CommitText(first.text)
            }
            else -> {
                if (config.engineParams["autoCommitOnSpace"] == "true") {
                    val text = composingBuffer.toString()
                    reset()
                    EngineResult.CommitText(text)
                } else EngineResult.Nothing
            }
        }
    }

    override suspend fun onEnter(): EngineResult {
        return when (config.enter?.composing) {
            "commitThenAction" -> {
                val text = composingBuffer.toString()
                reset()
                EngineResult.Combined(null, null, text)
            }
            "selectFirst" -> {
                if (candidates.isNotEmpty()) {
                    val first = candidates.first()
                    reset()
                    EngineResult.CommitText(first.text)
                } else {
                    val text = composingBuffer.toString()
                    reset()
                    EngineResult.CommitText(text)
                }
            }
            else -> EngineResult.Nothing
        }
    }

    override suspend fun onBackspace(): EngineResult {
        return when {
            composingBuffer.isNotEmpty() -> {
                composingBuffer.deleteCharAt(composingBuffer.length - 1)
                if (composingBuffer.isEmpty()) {
                    candidates = emptyList()
                    EngineResult.UpdateComposing("")
                } else {
                    val resolved = composingResolver.resolve(composingBuffer.toString(), config.engineParams)
                    candidates = suggestionEngine.suggest(resolved.displayText)
                    EngineResult.Combined(resolved.displayText, candidates, null)
                }
            }
            else -> EngineResult.Delete(1)
        }
    }

    override suspend fun onCandidateSelected(index: Int): EngineResult {
        val candidate = candidates.getOrNull(index) ?: return EngineResult.Nothing
        reset()
        return EngineResult.CommitText(candidate.text)
    }

    override suspend fun onShift(): EngineResult = EngineResult.Nothing
    override suspend fun onDoubleShift(): EngineResult = EngineResult.Nothing
    override fun getComposingText(): String = composingBuffer.toString()
    override fun getCandidates(): List<Candidate> = candidates
    override fun getShiftState(): ShiftState = ShiftState.OFF
}

/**
 * 组合解析器接口。
 */
interface ComposingResolver {
    fun resolve(buffer: String, params: Map<String, String>): ComposingResult
}

data class ComposingResult(
    val displayText: String,
    val isComplete: Boolean = false
)
