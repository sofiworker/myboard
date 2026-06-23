package xyz.xiao6.myboard.core.state

import xyz.xiao6.myboard.core.contract.*

/**
 * 状态转移引擎。
 * 只处理通用规则，不包含具体语言判断。
 * 阶段 02 实现真实逻辑。
 */
interface TransitionEngine {
    fun reduce(current: KeyboardContext, event: TransitionEvent): TransitionResult
}