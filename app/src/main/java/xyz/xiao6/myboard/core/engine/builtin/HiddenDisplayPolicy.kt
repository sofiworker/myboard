package xyz.xiao6.myboard.core.engine.builtin

import xyz.xiao6.myboard.core.contract.*

/**
 * Hidden 显示策略。
 * 不显示任何 composing 文本。
 */
class HiddenDisplayPolicy : DisplayPolicy {
    override val policyId: String = "hidden"
    
    override fun display(state: InputSessionState): String {
        return ""
    }
}
