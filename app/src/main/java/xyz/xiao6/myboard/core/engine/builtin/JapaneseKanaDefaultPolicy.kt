package xyz.xiao6.myboard.core.engine.builtin

import xyz.xiao6.myboard.core.contract.*

/**
 * 日文假名默认候选策略。
 * 空格选择候选，回车提交假名。
 */
class JapaneseKanaDefaultPolicy : CandidatePolicy {
    override val policyId: String = "japanese_kana_default"
    
    override fun sort(candidates: List<Candidate>): List<Candidate> = candidates
    
    override fun onSpace(state: InputSessionState): PolicyAction {
        val candidates = state.candidates
        return if (candidates.isNotEmpty()) {
            PolicyAction.Commit(candidates[0].text)
        } else if (state.rawBuffer.isNotEmpty()) {
            PolicyAction.Commit(state.rawBuffer)
        } else {
            PolicyAction.Commit(" ")
        }
    }
    
    override fun onEnter(state: InputSessionState): PolicyAction {
        return if (state.rawBuffer.isNotEmpty()) {
            PolicyAction.Commit(state.rawBuffer)
        } else {
            PolicyAction.PerformEditorAction
        }
    }
    
    override fun onCandidateSelected(state: InputSessionState, index: Int): PolicyAction {
        val candidates = state.candidates
        return if (index in candidates.indices) {
            PolicyAction.Commit(candidates[index].text)
        } else {
            PolicyAction.Noop
        }
    }
}
