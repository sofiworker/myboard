package xyz.xiao6.myboard.core.engine.builtin

import xyz.xiao6.myboard.core.contract.*

/**
 * ShowComposing 显示策略。
 * 显示 composing buffer（用于转写引擎）。
 */
class ShowComposingPolicy : DisplayPolicy {
    override val policyId: String = "show_composing"
    
    override fun display(state: InputSessionState): String {
        return state.rawBuffer
    }
}
