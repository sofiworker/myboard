package xyz.xiao6.myboard.core.engine.builtin

import xyz.xiao6.myboard.core.contract.*

/**
 * Direct 默认候选策略。
 * 不显示候选，直接提交。
 */
class DirectDefaultPolicy : CandidatePolicy {
    override val policyId: String = "direct_default"
    
    override fun sort(candidates: List<Candidate>): List<Candidate> = candidates
    
    override fun onSpace(state: InputSessionState): PolicyAction {
        return PolicyAction.Commit(" ")
    }
    
    override fun onEnter(state: InputSessionState): PolicyAction {
        return PolicyAction.PerformEditorAction
    }
    
    override fun onCandidateSelected(state: InputSessionState, index: Int): PolicyAction {
        return PolicyAction.Noop
    }
}
