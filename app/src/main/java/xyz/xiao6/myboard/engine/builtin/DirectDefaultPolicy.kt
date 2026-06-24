package xyz.xiao6.myboard.engine.builtin

import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.manifest.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.contract.engine.*
import xyz.xiao6.myboard.contract.bridge.*
import xyz.xiao6.myboard.contract.registry.*
import xyz.xiao6.myboard.contract.panel.*
import xyz.xiao6.myboard.contract.language.*
import xyz.xiao6.myboard.contract.state.*

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
