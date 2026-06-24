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
 * ShowQuery 显示策略。
 * 显示原始查询 buffer。
 */
class ShowQueryPolicy : DisplayPolicy {
    override val policyId: String = "show_query"
    
    override fun display(state: InputSessionState): String {
        return state.queryBuffer
    }
}
