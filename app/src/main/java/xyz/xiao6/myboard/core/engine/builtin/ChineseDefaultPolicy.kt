package xyz.xiao6.myboard.core.engine.builtin

import xyz.xiao6.myboard.core.contract.*

/**
 * 中文默认候选策略。
 * 空格选择第一个候选，回车提交原始编码。
 */
class ChineseDefaultPolicy : CandidatePolicy {
    override val policyId: String = "chinese_default"
    
    override fun sort(candidates: List<Candidate>): List<Candidate> {
        // 阶段 05 简化：直接返回原始顺序
        // 后续可加入词频调整、上下文排序等
        return candidates
    }
    
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
