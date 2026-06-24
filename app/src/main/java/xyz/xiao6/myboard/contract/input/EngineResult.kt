package xyz.xiao6.myboard.contract.input

/**
 * 输入引擎输出结果。
 * EngineResult 不直接操作 InputConnection。只有 InputPipeline 可以执行 commit/delete/editor action。
 */
sealed interface EngineResult {
    data class CommitText(val text: String) : EngineResult
    data class UpdateComposing(
        val text: String,
        val candidates: List<Candidate>,
        val selectedIndex: Int = -1
    ) : EngineResult
    data class CommitAndUpdate(
        val commit: String,
        val composing: String,
        val candidates: List<Candidate>
    ) : EngineResult
    data class DeleteText(val beforeCursor: Int) : EngineResult
    data object PerformEditorAction : EngineResult
    data object ClearComposing : EngineResult
    data object Nothing : EngineResult
}