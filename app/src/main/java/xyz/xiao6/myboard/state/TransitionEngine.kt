package xyz.xiao6.myboard.state

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
 * 状态转移引擎。
 * 只处理通用规则，不包含具体语言判断。
 * 阶段 02 实现真实逻辑。
 */
interface TransitionEngine {
    fun reduce(current: KeyboardContext, event: TransitionEvent): TransitionResult
}