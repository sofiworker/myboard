package xyz.xiao6.myboard.core.layout

import xyz.xiao6.myboard.core.contract.*

/**
 * Bindings 评估器。
 * 评估 region 的 visible 和 enabled。
 * 阶段 01 使用 StubBindingsEvaluator，阶段 04 替换真实实现。
 */
interface BindingsEvaluator {
    fun evaluate(bindings: Bindings?, context: KeyboardContext): Pair<Boolean, Boolean>
}