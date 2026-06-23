package xyz.xiao6.myboard.core.engine.builtin

import xyz.xiao6.myboard.core.contract.*

/**
 * ShowQuery 显示策略。
 * 显示原始查询 buffer。
 */
class ShowQueryPolicy : DisplayPolicy {
    override val policyId: String = "show_query"
    
    override fun display(state: InputSessionState): String {
        return state.queryBuffer
    }
}
